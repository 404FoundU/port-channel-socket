package port_channel;

import java.nio.channels.SelectionKey;

public class MessageWithSource {
	
	public MessageWithSource(Message message) {
		if(message == null) 
			throw new IllegalArgumentException("message");
		this.sourceKey = null;
		this.message = message;
	}

	public MessageWithSource(SelectionKey sourceKey, Message message) {
		if(sourceKey == null) 
			throw new IllegalArgumentException("sourceKey");
		if(message == null) 
			throw new IllegalArgumentException("message");
		this.sourceKey = sourceKey;
		this.message = message;
	}
	
	public SelectionKey getSourceKey() {
		return sourceKey;
	}
	
	public Message getMessage() {
		return message; 
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[IncomingMessage: <").append(sourceKey == null ? "" : sourceKey.toString());
		sb.append("> ").append(message.toString()).append(" ]");
		return sb.toString();
	}
	
	private SelectionKey sourceKey;
	private Message message;
}
