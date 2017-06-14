package zh.rpc.jms.test.client;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import zh.rpc.jms.client.RpcClient;
import zh.rpc.jms.test.api.IHelloService;

public class ClientTestThread {

	private static ApplicationContext context;

	public static void main(String[] args) throws Exception {
		context = new ClassPathXmlApplicationContext("spring-client.xml");
		RpcClient rpcProxy = context.getBean(RpcClient.class);

		final IHelloService helloService = rpcProxy.create(IHelloService.class);
		for (int i = 0; i < 100; i++) {
			final String message = "hello" + i;
			new Thread(new Runnable() {

				@Override
				public void run() {
					String result = helloService.hello(message);
					System.out.println(Thread.currentThread().getName() + "_" + result);
				}
			}, "Thread" + i).start();
		}
	}
}
