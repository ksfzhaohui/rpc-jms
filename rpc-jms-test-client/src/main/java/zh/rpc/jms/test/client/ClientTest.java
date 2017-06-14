package zh.rpc.jms.test.client;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import zh.rpc.jms.client.RpcClient;
import zh.rpc.jms.test.api.IHelloService;
import zh.rpc.jms.test.api.Person;

public class ClientTest {

	private static ApplicationContext context;

	public static void main(String[] args) throws Exception {
		context = new ClassPathXmlApplicationContext("spring-client.xml");
		RpcClient rpcProxy = context.getBean(RpcClient.class);

		IHelloService helloService = rpcProxy.create(IHelloService.class);
		String result = helloService.hello("World");
		System.out.println(result);

		Person person = new Person("zhao", "hui");
		String result2 = helloService.hello(person);
		System.out.println(result2);

		System.exit(0);
	}
}
