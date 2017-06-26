package zh.rpc.jms.common.converter;

import java.io.Serializable;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;

import zh.rpc.jms.common.exception.RpcJmsException;

public class DefaultMessageConverter implements MessageConverter {

	@Override
	public Message toMessage(Object object, Session session) throws JMSException {
		if (object instanceof Message) {
			return (Message) object;
		} else if (object instanceof String) {
			return createMessageForString((String) object, session);
		} else if (object instanceof byte[]) {
			return createMessageForByteArray((byte[]) object, session);
		} else if (object instanceof Serializable) {
			return createMessageForSerializable(((Serializable) object), session);
		} else {
			throw new RpcJmsException("Cannot convert object of type [" + object != null ? object.getClass().getName()
					: "null" + "] to JMS message. Supported message "
							+ "payloads are: String, byte array, Serializable object.");
		}
	}

	@Override
	public Object fromMessage(Message message) throws JMSException {
		if (message instanceof TextMessage) {
			return extractStringFromMessage((TextMessage) message);
		} else if (message instanceof BytesMessage) {
			return extractByteArrayFromMessage((BytesMessage) message);
		} else if (message instanceof ObjectMessage) {
			return extractSerializableFromMessage((ObjectMessage) message);
		} else {
			return message;
		}
	}

	private TextMessage createMessageForString(String text, Session session) throws JMSException {
		return session.createTextMessage(text);
	}

	private BytesMessage createMessageForByteArray(byte[] bytes, Session session) throws JMSException {
		BytesMessage message = session.createBytesMessage();
		message.writeBytes(bytes);
		return message;
	}

	private ObjectMessage createMessageForSerializable(Serializable object, Session session) throws JMSException {
		return session.createObjectMessage(object);
	}

	private String extractStringFromMessage(TextMessage message) throws JMSException {
		return message.getText();
	}

	private byte[] extractByteArrayFromMessage(BytesMessage message) throws JMSException {
		byte[] bytes = new byte[(int) message.getBodyLength()];
		message.readBytes(bytes);
		return bytes;
	}

	private Serializable extractSerializableFromMessage(ObjectMessage message) throws JMSException {
		return message.getObject();
	}

}
