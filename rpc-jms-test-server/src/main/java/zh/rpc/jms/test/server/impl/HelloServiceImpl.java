package zh.rpc.jms.test.server.impl;

import zh.rpc.jms.server.annotation.RpcService;
import zh.rpc.jms.test.api.IHelloService;
import zh.rpc.jms.test.api.Person;

@RpcService(IHelloService.class)
public class HelloServiceImpl implements IHelloService {

	@Override
	public String hello(String name) {
		return "REQ+" + name;
	}

	@Override
	public String hello(Person person) {
		return "REQ+" + person.getFirstName() + "_" + person.getLastName();
	}

}
