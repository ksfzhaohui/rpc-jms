package zh.rpc.jms.server;

import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

@Deprecated
public class RpcServer implements ApplicationContextAware, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(RpcServer.class);

	private QueueConnection qConnection;
	private QueueSession qSession;
	private Queue requestQ;

	private String rpcFactory;
	private String rpcRequest;

	/**
	 * 存放 服务名 与 服务对象 之间的映射关系
	 */
	private Map<String, Object> serviceMap = new HashMap<String, Object>();

	public RpcServer(String rpcFactory, String rpcRequest) {
		this.rpcFactory = rpcFactory;
		this.rpcRequest = rpcRequest;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		try {
			Context ctx = new InitialContext();
			QueueConnectionFactory factory = (QueueConnectionFactory) ctx.lookup(rpcFactory);
			qConnection = factory.createQueueConnection();
			qSession = qConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			requestQ = (Queue) ctx.lookup(rpcRequest);
			qConnection.start();

			QueueReceiver receiver = qSession.createReceiver(requestQ);
			receiver.setMessageListener(new RpcMessageListener(qSession, serviceMap));

			LOGGER.info("ready receiver message");
		} catch (Exception e) {
			if (qConnection != null) {
				try {
					qConnection.close();
				} catch (JMSException e1) {
				}
			}
			LOGGER.error("server start error", e);
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext ctx) throws BeansException {
		Map<String, Object> serviceBeanMap = ctx.getBeansWithAnnotation(RpcService.class);
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

}
