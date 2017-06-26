package zh.rpc.jms.common.converter;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

/**
 * 消息转换器
 * 
 * @author hui.zhao.cfs
 *
 */
public interface MessageConverter {

	/**
	 * 将业务数据转换为JMS Message
	 * 
	 * @param object
	 * @param session
	 * @return
	 * @throws JMSException
	 */
	Message toMessage(Object object, Session session) throws JMSException;

	/**
	 * 将JMS Message转为业务数据
	 * 
	 * @param message
	 * @return
	 * @throws JMSException
	 */
	Object fromMessage(Message message) throws JMSException;

}
