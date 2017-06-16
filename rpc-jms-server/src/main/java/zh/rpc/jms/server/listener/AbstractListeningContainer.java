package zh.rpc.jms.server.listener;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;

import zh.rpc.jms.common.exception.RpcJmsException;
import zh.rpc.jms.common.util.ConnectionFactoryUtils;
import zh.rpc.jms.common.util.JmsUtils;

public abstract class AbstractListeningContainer implements SmartLifecycle, InitializingBean, ApplicationContextAware {

	protected Connection sharedConnection;

	protected ConnectionFactory connectionFactory;

	private boolean autoStartup = true;

	private int phase = Integer.MAX_VALUE;

	private boolean sharedConnectionStarted = false;

	private static final Logger LOGGER = LoggerFactory.getLogger(RpcMessageListenerContainer.class);

	@Override
	public void start() {
		try {
			establishSharedConnection();
			startSharedConnection();
		} catch (JMSException e) {
			throw new RpcJmsException(e);
		}
	}

	@Override
	public void stop() {
		try {
			stopSharedConnection();
		} catch (JMSException e) {
			throw new RpcJmsException(e);
		}
	}

	@Override
	public boolean isRunning() {
		return false;
	}

	@Override
	public int getPhase() {
		return this.phase;
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	@Override
	public void stop(Runnable callback) {
		this.stop();
		callback.run();
	}

	protected void establishSharedConnection() throws JMSException {
		if (this.sharedConnection == null) {
			this.sharedConnection = createSharedConnection();
			LOGGER.info("Established shared JMS Connection");
		}
	}

	protected final void refreshSharedConnection() throws JMSException {
		ConnectionFactoryUtils.releaseConnection(this.sharedConnection);
		this.sharedConnection = null;
		this.sharedConnection = createSharedConnection();
		if (isSharedConnectionStarted()) {
			this.sharedConnection.start();
		}
	}

	/**
	 * 创建jms连接
	 * 
	 * @return
	 * @throws JMSException
	 */
	protected Connection createSharedConnection() throws JMSException {
		Connection con = getConnectionFactory().createConnection();
		try {
			prepareSharedConnection(con);
		} catch (JMSException e) {
			JmsUtils.closeConnection(con);
			throw e;
		}
		return con;
	}

	protected abstract void prepareSharedConnection(Connection connection) throws JMSException;

	/**
	 * Connection start
	 * 
	 * @throws JMSException
	 */
	protected void startSharedConnection() throws JMSException {
		if (this.sharedConnection != null) {
			this.sharedConnectionStarted = true;
			try {
				this.sharedConnection.start();
			} catch (javax.jms.IllegalStateException ex) {
				LOGGER.debug("Ignoring Connection start exception - assuming already started: " + ex);
			}
		}
	}

	protected void stopSharedConnection() throws JMSException {
		if (this.sharedConnection != null) {
			this.sharedConnectionStarted = false;
			try {
				this.sharedConnection.stop();
			} catch (javax.jms.IllegalStateException ex) {
				LOGGER.debug("Ignoring Connection stop exception - assuming already stopped: " + ex);
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

	public boolean isSharedConnectionStarted() {
		return sharedConnectionStarted;
	}

	public void setSharedConnectionStarted(boolean sharedConnectionStarted) {
		this.sharedConnectionStarted = sharedConnectionStarted;
	}

}
