package zh.rpc.jms.common.util;

import javax.jms.Connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionFactoryUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(JmsUtils.class);

	public static void releaseConnection(Connection con) {
		if (con == null) {
			return;
		}
		try {
			con.close();
		} catch (Throwable ex) {
			LOGGER.debug("Could not close JMS Connection", ex);
		}
	}
}
