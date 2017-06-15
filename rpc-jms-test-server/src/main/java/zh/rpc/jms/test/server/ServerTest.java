package zh.rpc.jms.test.server;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ServerTest {

	@SuppressWarnings("resource")
	public static void main(String[] args) {
		new ClassPathXmlApplicationContext("spring-server.xml");
	}
}
