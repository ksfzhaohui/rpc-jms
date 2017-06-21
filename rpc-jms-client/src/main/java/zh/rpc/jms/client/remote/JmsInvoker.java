package zh.rpc.jms.client.remote;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TemporaryQueue;

import org.springframework.beans.factory.InitializingBean;

import zh.rpc.jms.common.bean.RpcRequest;
import zh.rpc.jms.common.bean.RpcResponse;
import zh.rpc.jms.common.util.ConnectionFactoryUtils;
import zh.rpc.jms.common.util.JmsUtils;
import zh.rpc.jms.common.util.SerializationUtil;

public class JmsInvoker implements IRemoteInvoker, InitializingBean {

	private ConnectionFactory connectionFactory;

	private Queue queue;

	// 接受数据超时时间 默认为0
	private long receiveTimeout = 0;

	// 消息优先级 默认为4
	private int priority = 4;

	@Override
	public Object invoke(RpcRequest request) throws Throwable {
		Connection con = createConnection();
		Session session = null;
		try {
			session = createSession(con);
			Message requestMessage = createRequestMessage(session, request);
			con.start();
			Message responseMessage = doExecuteRequest(session, queue, requestMessage);
			RpcResponse rpcResponse = extractInvocationResult(responseMessage);
			if (rpcResponse.hasException()) {
				throw rpcResponse.getException();
			} else {
				return rpcResponse.getResult();
			}
		} catch (JMSException e) {
			throw new RuntimeException("Could not access JMS invoker queue [" + this.queue + "]", e);
		} finally {
			JmsUtils.closeSession(session);
			ConnectionFactoryUtils.releaseConnection(con);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.connectionFactory == null) {
			throw new IllegalArgumentException("Property 'connectionFactory' is required");
		}
		if (this.queue == null) {
			throw new IllegalArgumentException("Property 'queue' is required");
		}
		if (this.receiveTimeout < 0) {
			throw new IllegalArgumentException("Property 'receiveTimeout' Not less than '0'");
		}
		if (this.priority < 0) {
			throw new IllegalArgumentException("Property 'priority' Not less than '0'");
		}
	}

	protected Connection createConnection() throws JMSException {
		ConnectionFactory cf = getConnectionFactory();
		return cf.createConnection();
	}

	protected Session createSession(Connection con) throws JMSException {
		return con.createSession(false, Session.AUTO_ACKNOWLEDGE);
	}

	/**
	 * 序列化RpcRequest为Message
	 * 
	 * @param session
	 * @param request
	 * @return
	 * @throws JMSException
	 */
	protected Message createRequestMessage(Session session, RpcRequest request) throws JMSException {
		BytesMessage requestMessage = session.createBytesMessage();
		requestMessage.writeBytes(SerializationUtil.serialize(request));
		return requestMessage;
	}

	/**
	 * 反序列化Message为RpcResponse
	 * 
	 * @param responseMessage
	 * @return
	 * @throws JMSException
	 */
	protected RpcResponse extractInvocationResult(Message responseMessage) throws JMSException {
		BytesMessage bytesMessage = (BytesMessage) responseMessage;
		byte messByte[] = new byte[(int) bytesMessage.getBodyLength()];
		bytesMessage.readBytes(messByte);
		RpcResponse rpcResponse = SerializationUtil.deserialize(messByte, RpcResponse.class);
		return rpcResponse;
	}

	/**
	 * 向jms消息队列发送消息
	 * 
	 * @param session
	 * @param queue
	 * @param requestMessage
	 * @return
	 * @throws JMSException
	 */
	protected Message doExecuteRequest(Session session, Queue queue, Message requestMessage) throws JMSException {
		TemporaryQueue responseQueue = null;
		MessageProducer producer = null;
		MessageConsumer consumer = null;
		try {
			responseQueue = session.createTemporaryQueue();
			producer = session.createProducer(queue);
			consumer = session.createConsumer(responseQueue);
			requestMessage.setJMSReplyTo(responseQueue);
			requestMessage.setJMSPriority(priority);
			producer.send(requestMessage);

			long timeout = getReceiveTimeout();
			return (timeout > 0 ? consumer.receive(timeout) : consumer.receive());
		} finally {
			JmsUtils.closeMessageConsumer(consumer);
			JmsUtils.closeMessageProducer(producer);
			JmsUtils.deleteTemporaryQueue(responseQueue);
		}
	}

	public ConnectionFactory getConnectionFactory() {
		return connectionFactory;
	}

	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	public Queue getQueue() {
		return queue;
	}

	public void setQueue(Queue queue) {
		this.queue = queue;
	}

	public long getReceiveTimeout() {
		return receiveTimeout;
	}

	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

}
