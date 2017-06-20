package zh.rpc.jms.server.annotation;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

public class RpcServiceParser {

	/**
	 * 存放 服务名 与 服务对象 之间的映射关系
	 */
	private Map<String, Object> serviceMap = new HashMap<String, Object>();

	/**
	 * serviceName和serviceVersion的连接符
	 */
	public static final String CONNECTOR = "-";

	/**
	 * 解析注解的服务实现类
	 * 
	 * @param applicationContext
	 * @throws BeansException
	 */
	public void parserRpcServices(ApplicationContext applicationContext) throws BeansException {
		Map<String, Object> serviceBeanMap = applicationContext.getBeansWithAnnotation(RpcService.class);
		if (serviceBeanMap != null && serviceBeanMap.size() > 0) {
			for (Object serviceBean : serviceBeanMap.values()) {
				RpcService rpcService = serviceBean.getClass().getAnnotation(RpcService.class);
				String serviceName = rpcService.value().getName();
				String serviceVersion = rpcService.version();
				String fullName = getServiceFullName(serviceName, serviceVersion);
				serviceMap.put(fullName, serviceBean);
			}
		}
	}

	/**
	 * 获取服务的全称(serviceName-serviceVersion)
	 * 
	 * @param serviceName
	 * @param serviceVersion
	 * @return
	 */
	private String getServiceFullName(String serviceName, String serviceVersion) {
		if (serviceVersion != null && !serviceVersion.equals("")) {
			return serviceName + CONNECTOR + serviceVersion;
		}
		return serviceName;
	}

	public Object getService(String fullName) {
		return serviceMap.get(fullName);
	}

	public Object getService(String serviceName, String serviceVersion) {
		return serviceMap.get(getServiceFullName(serviceName, serviceVersion));
	}

}
