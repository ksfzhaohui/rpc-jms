package zh.rpc.jms.client.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

import zh.rpc.jms.client.RpcProxyBean;
import zh.rpc.jms.common.bean.RpcRequest;

public class ProxyFactory implements IProxy, InvocationHandler {

	private RpcProxyBean proxyBean;

	public ProxyFactory(RpcProxyBean proxyBean) {
		this.proxyBean = proxyBean;
	}

	@Override
	public Object getProxy() {
		return Proxy.newProxyInstance(proxyBean.getServiceInterface().getClassLoader(),
				new Class<?>[] { proxyBean.getServiceInterface() }, this);
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Object retVal = null;
		RpcRequest request = new RpcRequest();
		request.setRequestId(UUID.randomUUID().toString());
		request.setInterfaceName(method.getDeclaringClass().getName());
		request.setServiceVersion(proxyBean.getServiceVesion());
		request.setMethodName(method.getName());
		request.setParameterTypes(method.getParameterTypes());
		request.setParameters(args);

		retVal = proxyBean.invoke(request);
		return retVal;
	}

}
