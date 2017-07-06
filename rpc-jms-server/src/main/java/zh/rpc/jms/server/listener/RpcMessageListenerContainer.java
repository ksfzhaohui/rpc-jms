package zh.rpc.jms.server.listener;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.jms.Connection;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import zh.rpc.jms.common.converter.DefaultMessageConverter;
import zh.rpc.jms.common.converter.MessageConverter;
import zh.rpc.jms.common.exception.RpcJmsException;
import zh.rpc.jms.common.util.ConnectionFactoryUtils;
import zh.rpc.jms.server.annotation.RpcServiceParser;

public class RpcMessageListenerContainer extends AbstractListeningContainer implements ExceptionListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(RpcMessageListenerContainer.class);

	private Queue destination;

	// 消费端并发数量 默认：1
	private int concurrentConsumers = 1;

	private Executor taskExecutor;

	private Set<Session> sessions;

	private Set<MessageConsumer> consumers;

	private RpcServiceParser rpcServiceParser = new RpcServiceParser();

	private MessageConverter messageConverter = new DefaultMessageConverter();

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		rpcServiceParser.parserRpcServices(applicationContext);
	}

	@Override
	public void afterPropertiesSet() {
		if (this.connectionFactory == null) {
			throw new IllegalArgumentException("Property 'connectionFactory' is required");
		}
		if (this.destination == null) {
			throw new IllegalArgumentException("Property 'destination' is required");
		}
		initialize();
	}

	/**
	 * 初始化connection和consumer
	 */
	public void initialize() {
		try {
			doInitialize();
		} catch (JMSException ex) {
			ConnectionFactoryUtils.releaseConnection(this.sharedConnection);
			this.sharedConnection = null;
			throw new RpcJmsException(ex);
		}
	}

	/**
	 * 服务器启动初始化
	 * 
	 * @throws JMSException
	 */
	protected void doInitialize() throws JMSException {
		try {
			establishSharedConnection();
		} catch (JMSException ex) {
			LOGGER.error("Could not connect on initialization - registering message consumers lazily", ex);
			return;
		}
		initializeConsumers();
	}

	/**
	 * 初始化consumer
	 * 
	 * @throws JMSException
	 */
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

	/**
	 * 对Consumer设置监听
	 * 
	 * @param session
	 * @return
	 * @throws JMSException
	 */
	protected MessageConsumer createListenerConsumer(final Session session) throws JMSException {
		MessageConsumer consumer = session.createConsumer(destination);
		consumer.setMessageListener(new RpcMessageListener(session, taskExecutor, messageConverter, rpcServiceParser));
		return consumer;
	}

	@Override
	public void onException(JMSException ex) {
		try {
			this.sessions = null;
			this.consumers = null;
			refreshSharedConnection();
			initializeConsumers();
			LOGGER.info("Successfully refreshed JMS Connection");
		} catch (JMSException recoverEx) {
			LOGGER.debug("Failed to recover JMS Connection", recoverEx);
			LOGGER.error("Encountered non-recoverable JMSException", ex);
		}
	}

	@Override
	protected void prepareSharedConnection(Connection connection) throws JMSException {
		connection.setExceptionListener(this);
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
