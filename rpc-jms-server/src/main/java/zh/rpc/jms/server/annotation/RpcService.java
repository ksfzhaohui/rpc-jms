package zh.rpc.jms.server.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface RpcService {

	/**
	 * 服务接口类
	 * 
	 * @return
	 */
	Class<?> value();

	/**
	 * 服务版本号
	 * 
	 * @return
	 */
	String version() default "";
}
