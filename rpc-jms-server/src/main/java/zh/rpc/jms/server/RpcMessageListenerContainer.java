package zh.rpc.jms.server;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import zh.rpc.jms.common.bean.RpcRequest;
import zh.rpc.jms.common.bean.RpcResponse;
import zh.rpc.jms.common.util.ConnectionFactoryUtils;
import zh.rpc.jms.common.util.JmsUtils;
import zh.rpc.jms.common.util.SerializationUtil;

public class RpcMessageListenerContainer implements InitializingBean, ApplicationContextAware {

	private static final Logger LOGGER = LoggerFactory.getLogger(RpcMessageListenerContainer.class);

	private Connection sharedConnection;

	private ConnectionFactory connectionFactory;

	private Queue destination;

	private int concurrentConsumers = 1;

	private Executor taskExecutor;

	private Set<Session> sessions;

	private Set<MessageConsumer> consumers;

	/**
	 * 存放 服务名 与 服务对象 之间的映射关系
	 */
	private Map<String, Object> serviceMap = new HashMap<String, Object>();

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		Map<String, Object> serviceBeanMap = applicationContext.getBeansWithAnnotation(RpcService.class);
		if (serviceBeanMap != null && serviceBeanMap.size() > 0) {
			for (Object serviceBean : serviceBeanMap.values()) {
				RpcService rpcService = serviceBean.getClass().getAnnotation(RpcService.class);
				String serviceName = rpcService.value().getName();
				String serviceVersion = rpcService.version();
				if (serviceVersion != null && !serviceVersion.equals("")) {
					serviceName += "-" + serviceVersion;
				}
				serviceMap.put(serviceName, serviceBean);
			}
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.connectionFactory == null) {
			throw new IllegalArgumentException("Property 'connectionFactory' is required");
		}
		if (this.destination == null) {
			throw new IllegalArgumentException("Property 'destination' is required");
		}
		initialize();
	}

	public void initialize() throws JMSException {
		try {
			doInitialize();
			startSharedConnection();
		} catch (JMSException ex) {
			ConnectionFactoryUtils.releaseConnection(this.sharedConnection);
			this.sharedConnection = null;
		}
	}

	protected void doInitialize() throws JMSException {
		try {
			establishSharedConnection();
		} catch (JMSException ex) {
			LOGGER.error("Could not connect on initialization - registering message consumers lazily", ex);
			return;
		}
		initializeConsumers();
	}

	protected void establishSharedConnection() throws JMSException {
		if (this.sharedConnection == null) {
			this.sharedConnection = createSharedConnection();
			LOGGER.info("Established shared JMS Connection");
		}
	}

	protected Connection createSharedConnection() throws JMSException {
		return getConnectionFactory().createConnection();
	}

	protected void initializeConsumers() throws JMSException {
		if (this.consumers == null) {
			this.sessions = new HashSet<Session>(this.concurrentConsumers);
			this.consumers = new HashSet<MessageConsumer>(this.concurrentConsumers);
			Connection con = getSharedConnection();
			for (int i = 0; i < this.concurrentConsumers; i++) {
				Session session = createSession(con);
				MessageConsumer consumer = createListenerConsumer(session);
				this.sessions.add(session);
				this.consumers.add(consumer);
			}
		}
	}

	protected Session createSession(Connection con) throws JMSException {
		return con.createSession(false, Session.AUTO_ACKNOWLEDGE);
	}

	protected MessageConsumer createListenerConsumer(final Session session) throws JMSException {
		MessageConsumer consumer = session.createConsumer(destination);
		if (this.taskExecutor != null) {
			consumer.setMessageListener(new MessageListener() {
				public void onMessage(final Message message) {
					taskExecutor.execute(new Runnable() {
						public void run() {
							executeListener(session, message);
						}
					});
				}
			});
		} else {
			consumer.setMessageListener(new MessageListener() {
				public void onMessage(Message message) {
					executeListener(session, message);
				}
			});
		}

		return consumer;
	}

	protected void executeListener(Session session, Message message) {
		try {
			RpcRequest rpcRequest = getRpcRequest(message);
			RpcResponse rpcResponse = invokeAndCreateResult(rpcRequest);
			writeResponseMessage(session, message, rpcResponse);
		} catch (Throwable ex) {
			handleListenerException(ex);
		}
	}

	/**
	 * 回复客户端消息
	 * 
	 * @param session
	 * @param requestMessage
	 * @param rpcResponse
	 * @throws JMSException
	 */
	private void writeResponseMessage(Session session, Message requestMessage, RpcResponse rpcResponse)
			throws JMSException {
		Message response = createResponseMessage(session, requestMessage, rpcResponse);
		MessageProducer producer = session.createProducer(requestMessage.getJMSReplyTo());
		try {
			producer.send(response);
		} finally {
			JmsUtils.closeMessageProducer(producer);
		}
	}

	private RpcResponse invokeAndCreateResult(RpcRequest rpcRequest) {
		RpcResponse rpcResponse = new RpcResponse();
		rpcResponse.setRequestId(rpcRequest.getRequestId());
		try {
			rpcResponse.setResult(invoke(rpcRequest));
		} catch (Exception ex) {
			rpcResponse.setException(ex);
		}
		return rpcResponse;
	}

	/**
	 * 反射调用服务器本地方法
	 * 
	 * @param rpcRequest
	 * @return
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	private Object invoke(RpcRequest rpcRequest) throws NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		String serviceName = rpcRequest.getInterfaceName();
		String serviceVersion = rpcRequest.getServiceVersion();
		if (serviceVersion != null && !serviceVersion.equals("")) {
			serviceName += "-" + serviceVersion;
		}
		Object serviceBean = serviceMap.get(serviceName);
		if (serviceBean == null) {
			throw new RuntimeException(String.format("can not find service bean by key: %s", serviceName));
		}

		Class<?> serviceClass = serviceBean.getClass();
		String methodName = rpcRequest.getMethodName();
		Class<?>[] parameterTypes = rpcRequest.getParameterTypes();
		Object[] parameters = rpcRequest.getParameters();

		Method method = serviceClass.getMethod(methodName, parameterTypes);
		method.setAccessible(true);
		return method.invoke(serviceBean, parameters);
	}

	/**
	 * 反序列化Message为RpcRequest
	 * 
	 * @param message
	 * @return
	 * @throws JMSException
	 */
	private RpcRequest getRpcRequest(Message message) throws JMSException {
		BytesMessage byteMessage = (BytesMessage) message;
		byte messByte[] = new byte[(int) byteMessage.getBodyLength()];
		byteMessage.readBytes(messByte);
		RpcRequest rpcRequest = SerializationUtil.deserialize(messByte, RpcRequest.class);
		return rpcRequest;
	}

	/**
	 * 创建回复Message
	 * 
	 * @param session
	 * @param requestMessage
	 * @param rpcResponse
	 * @return
	 * @throws JMSException
	 */
	private Message createResponseMessage(Session session, Message requestMessage, RpcResponse rpcResponse)
			throws JMSException {
		BytesMessage responeByte = session.createBytesMessage();
		responeByte.writeBytes(SerializationUtil.serialize(rpcResponse));
		String correlation = requestMessage.getJMSCorrelationID();
		if (correlation == null) {
			correlation = requestMessage.getJMSMessageID();
		}
		responeByte.setJMSCorrelationID(correlation);
		return responeByte;
	}

	protected void handleListenerException(Throwable ex) {
		// 待实现
	}

	protected void startSharedConnection() throws JMSException {
		if (this.sharedConnection != null) {
			try {
				this.sharedConnection.start();
			} catch (javax.jms.IllegalStateException ex) {
				LOGGER.debug("Ignoring Connection start exception - assuming already started: " + ex);
			}
		}
	}

	public Connection getSharedConnection() {
		return sharedConnection;
	}

	public void setSharedConnection(Connection sharedConnection) {
		this.sharedConnection = sharedConnection;
	}

	public ConnectionFactory getConnectionFactory() {
		return connectionFactory;
	}

	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	public void setConcurrentConsumers(int concurrentConsumers) {
		this.concurrentConsumers = concurrentConsumers;
	}

	public void setTaskExecutor(Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	public void setSessions(Set<Session> sessions) {
		this.sessions = sessions;
	}

	public void setConsumers(Set<MessageConsumer> consumers) {
		this.consumers = consumers;
	}

	public void setDestination(Queue destination) {
		this.destination = destination;
	}

}
