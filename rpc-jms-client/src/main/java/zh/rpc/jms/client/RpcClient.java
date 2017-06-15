package zh.rpc.jms.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

import javax.jms.BytesMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zh.rpc.jms.common.bean.RpcRequest;
import zh.rpc.jms.common.bean.RpcResponse;
import zh.rpc.jms.common.util.SerializationUtil;

/**
 * RPC 代理（用于创建 RPC 服务代理）
 * 
 * @author zhaohui
 *
 */
@Deprecated
public class RpcClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(RpcClient.class);

	private QueueConnection qConnection;
	private QueueSession qSession;
	private Queue requestQ;
	private Queue responseQ;

	public RpcClient(String rpcFactory, String rpcRequest, String rpcResponse) {
		try {
			Context ctx = new InitialContext();
			QueueConnectionFactory factory = (QueueConnectionFactory) ctx.lookup(rpcFactory);
			qConnection = factory.createQueueConnection();
			qSession = qConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			requestQ = (Queue) ctx.lookup(rpcRequest);
			responseQ = (Queue) ctx.lookup(rpcResponse);
			qConnection.start();
		} catch (Exception e) {
			LOGGER.error("init rpcproxy error", e);
		}
	}

	public <T> T create(final Class<?> interfaceClass) {
		return create(interfaceClass, "");
	}

	@SuppressWarnings("unchecked")
	public <T> T create(final Class<?> interfaceClass, final String serviceVersion) {
		return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class<?>[] { interfaceClass },
				new InvocationHandler() {

					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						RpcRequest request = new RpcRequest();
						request.setRequestId(UUID.randomUUID().toString());
						request.setInterfaceName(method.getDeclaringClass().getName());
						request.setServiceVersion(serviceVersion);
						request.setMethodName(method.getName());
						request.setParameterTypes(method.getParameterTypes());
						request.setParameters(args);

						BytesMessage requestMessage = qSession.createBytesMessage();
						requestMessage.writeBytes(SerializationUtil.serialize(request));
						requestMessage.setJMSReplyTo(responseQ);
						QueueSender qsender = qSession.createSender(requestQ);
						qsender.send(requestMessage);

						String filter = "JMSCorrelationID = '" + requestMessage.getJMSMessageID() + "'";
						QueueReceiver qReceiver = qSession.createReceiver(responseQ, filter);
						BytesMessage responseMessage = (BytesMessage) qReceiver.receive(30000);
						byte messByte[] = new byte[(int) responseMessage.getBodyLength()];
						responseMessage.readBytes(messByte);
						RpcResponse rpcResponse = SerializationUtil.deserialize(messByte, RpcResponse.class);

						if (rpcResponse.hasException()) {
							throw rpcResponse.getException();
						} else {
							return rpcResponse.getResult();
						}
					}
				});
	}
}
