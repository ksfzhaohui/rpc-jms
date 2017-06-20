package zh.rpc.jms.common.util;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.TemporaryQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JmsUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(JmsUtils.class);

	public static void closeConnection(Connection con) {
		closeConnection(con, false);
	}

	public static void closeConnection(Connection con, boolean stop) {
		if (con != null) {
			try {
				if (stop) {
					try {
						con.stop();
					} finally {
						con.close();
					}
				} else {
					con.close();
				}
			} catch (javax.jms.IllegalStateException ex) {
				LOGGER.error("Ignoring Connection state exception - assuming already closed: " + ex);
			} catch (JMSException ex) {
				LOGGER.error("Could not close JMS Connection", ex);
			} catch (Throwable ex) {
				LOGGER.error("Unexpected exception on closing JMS Connection", ex);
			}
		}
	}

	public static void closeSession(Session session) {
		if (session != null) {
			try {
				session.close();
			} catch (JMSException ex) {
				LOGGER.error("Could not close JMS Session", ex);
			} catch (Throwable ex) {
				LOGGER.error("Unexpected exception on closing JMS Session", ex);
			}
		}
	}

	public static void closeMessageProducer(MessageProducer producer) {
		if (producer != null) {
			try {
				producer.close();
			} catch (JMSException ex) {
				LOGGER.error("Could not close JMS MessageProducer", ex);
			} catch (Throwable ex) {
				LOGGER.error("Unexpected exception on closing JMS MessageProducer", ex);
			}
		}
	}

	public static void closeMessageConsumer(MessageConsumer consumer) {
		if (consumer != null) {
			boolean wasInterrupted = Thread.interrupted();
			try {
				consumer.close();
			} catch (JMSException ex) {
				LOGGER.error("Could not close JMS MessageConsumer", ex);
			} catch (Throwable ex) {
				LOGGER.error("Unexpected exception on closing JMS MessageConsumer", ex);
			} finally {
				if (wasInterrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	public static void closeQueueBrowser(QueueBrowser browser) {
		if (browser != null) {
			try {
				browser.close();
			} catch (JMSException ex) {
				LOGGER.error("Could not close JMS QueueBrowser", ex);
			} catch (Throwable ex) {
				LOGGER.error("Unexpected exception on closing JMS QueueBrowser", ex);
			}
		}
	}

	public static void deleteTemporaryQueue(TemporaryQueue temporaryQueue) {
		if (temporaryQueue != null) {
			try {
				temporaryQueue.delete();
			} catch (JMSException ex) {
				LOGGER.error("Could not delete JMS temporaryQueue", ex);
			} catch (Throwable ex) {
				LOGGER.error("Unexpected exception on delete JMS temporaryQueue", ex);
			}
		}
	}

}
