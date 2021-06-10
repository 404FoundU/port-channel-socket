package port_channel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class Message implements Serializable
{
	public static final int MESSAGE_TYPE_HEARTBEAT = 0;
	public static final int MESSAGE_TYPE_UNKNOWN = -1;
	public static final int MESSAGE_TYPE_CLIENT_CONNECTED = -2;
	public static final int MESSAGE_TYPE_CLIENT_DISCONNECTED = -3;
	public static final int MESSAGE_TYPE_COMMUNICATION_ERROR = -4;
	
	public static Message createMessage(int messageType) {
		return new Message(Message.getNextMessageId(), messageType);
	}
	
	protected Message() {
		this.messageId = 0;
		this.messageType = MESSAGE_TYPE_UNKNOWN;
	}
	
	protected Message(long messageId, int messageType) {
		this.messageId = messageId;
		this.messageType = messageType;
	}
		
	public long getMessageId() {
		return messageId;
	}
	
	public int getMessageType() {
		return messageType;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{ Id=").append(messageId);
		sb.append(", Type=").append(messageType).append(" }");
		return sb.toString();
	}

	public byte[] toBytes() throws IOException {
		byte[] result;
		try(ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			try(ObjectOutputStream out = new ObjectOutputStream(bos)) {
				out.writeObject(this);
				result = bos.toByteArray();
			}
		}
		return result;
	}
	
	public static long getNextMessageId() {
		synchronized(nextMessageIdSyncObject) {
			return ++nextMessageId;
		}
	}
	
	private long messageId;
	private int messageType;
	private String text;
	private static final long serialVersionUID = 1L;
	private static Object nextMessageIdSyncObject = new Object();
	private static long nextMessageId = 0;
}
