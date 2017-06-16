package zh.rpc.jms.common.exception;

public class RpcJmsException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public RpcJmsException(String msg) {
		super(msg);
	}

	public RpcJmsException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public RpcJmsException(Throwable cause) {
		super(cause != null ? cause.getMessage() : null, cause);
	}

}
