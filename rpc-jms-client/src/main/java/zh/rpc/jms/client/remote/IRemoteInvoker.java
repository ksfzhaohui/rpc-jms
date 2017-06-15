package zh.rpc.jms.client.remote;

import zh.rpc.jms.common.bean.RpcRequest;

public interface IRemoteInvoker {

	public Object invoke(RpcRequest request) throws Throwable;

}
