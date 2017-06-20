package zh.rpc.jms.test.client;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import zh.rpc.jms.test.api.IHelloService;
import zh.rpc.jms.test.api.Person;

public class ClientPressTest {

	private static ApplicationContext context;

	public static void main(String[] args) throws Exception {
		context = new ClassPathXmlApplicationContext("spring-client.xml");
		IHelloService helloService = (IHelloService) context.getBean("rpcService");
		long startTime = System.currentTimeMillis();
		int times = 1000;
		for (int i = 0; i < times; i++) {
			Person person = new Person("zhao", "hui" + i);
			String result2 = helloService.hello(person);
			System.out.println("第" + i + "次收到返回值 = " + result2);
		}
		long endTime = System.currentTimeMillis();
		System.out.println("循环发送" + times + "次，耗时 = " + (endTime - startTime) + "ms");
		System.exit(0);
	}
}
