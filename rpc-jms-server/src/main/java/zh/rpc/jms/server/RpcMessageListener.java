package zh.rpc.jms.server;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.jms.BytesMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueSender;
import javax.jms.QueueSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zh.rpc.jms.common.bean.RpcRequest;
import zh.rpc.jms.common.bean.RpcResponse;
import zh.rpc.jms.common.util.SerializationUtil;

public class RpcMessageListener implements MessageListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(RpcMessageListener.class);

	private QueueSession qSession;
	/**
	 * 存放 服务名 与 服务对象 之间的映射关系
	 */
	private Map<String, Object> serviceMap = new HashMap<String, Object>();

	public RpcMessageListener(QueueSession qSession, Map<String, Object> serviceMap) {
		this.qSession = qSession;
		this.serviceMap = serviceMap;
	}

	@Override
	public void onMessage(Message message) {
		try {
			LOGGER.info("receiver message : " + message.getJMSMessageID());
			RpcResponse response = new RpcResponse();
			BytesMessage responeByte = qSession.createBytesMessage();
			responeByte.setJMSCorrelationID(message.getJMSMessageID());
			QueueSender sender = qSession.createSender((Queue) message.getJMSReplyTo());
			try {
				BytesMessage byteMessage = (BytesMessage) message;
				byte messByte[] = new byte[(int) byteMessage.getBodyLength()];
				byteMessage.readBytes(messByte);
				RpcRequest rpcRequest = SerializationUtil.deserialize(messByte, RpcRequest.class);

				response.setRequestId(rpcRequest.getRequestId());

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
				Object result = method.invoke(serviceBean, parameters);
				response.setResult(result);
			} catch (Exception e) {
				response.setException(e);
				LOGGER.error("onMessage error", e);
			}
			responeByte.writeBytes(SerializationUtil.serialize(response));
			sender.send(responeByte);
		} catch (Exception e) {
			LOGGER.error("send message error", e);
		}
	}

}
