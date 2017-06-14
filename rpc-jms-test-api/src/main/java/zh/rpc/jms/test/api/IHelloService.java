package zh.rpc.jms.test.api;

public interface IHelloService {

    String hello(String name);

    String hello(Person person);
}
