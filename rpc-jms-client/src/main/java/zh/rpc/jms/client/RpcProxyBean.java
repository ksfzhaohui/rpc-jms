package zh.rpc.jms.client;

import zh.rpc.jms.client.proxy.ProxyFactory;
import zh.rpc.jms.client.remote.JmsInvoker;

public class RpcProxyBean extends JmsInvoker {

	private Class<?> serviceInterface;

	// 服务版本 默认为""
	private String serviceVesion = "";

	private Object serviceProxy;

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		if (this.serviceInterface == null || !this.serviceInterface.isInterface()) {
			throw new IllegalArgumentException("'serviceInterface' must be an interface");
		}

		serviceProxy = new ProxyFactory(this).getProxy();
	}

	public Class<?> getServiceInterface() {
		return serviceInterface;
	}

	public void setServiceInterface(Class<?> serviceInterface) {
		this.serviceInterface = serviceInterface;
	}

	public Object getServiceProxy() {
		return serviceProxy;
	}

	public String getServiceVesion() {
		return serviceVesion;
	}

	public void setServiceVesion(String serviceVesion) {
		this.serviceVesion = serviceVesion;
	}

}
