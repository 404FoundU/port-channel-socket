package port_channel;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class ChannelEndpoint
{
	public ChannelEndpoint(String host, int port, int maxMessageLength) {
		this.host = host;
		this.port = port;
		this.maxMessageLength = maxMessageLength;
	}
	
	public String getHost() {
		return host; 
	}
	
	public int getPort() { 
		return port;
	}
	
	public void open() throws UnknownHostException, IOException {
		socket = new Socket(host, port); 
		socketInput =  socket.getInputStream(); 
		socketOutput = socket.getOutputStream();
	}
	
	public void close() throws IOException {
		if(socketInput != null)
		{
			socketInput.close();
			socketInput = null;
		}
		if(socketOutput != null)
		{
			socketOutput.close();
			socketOutput = null;
		}
		if(socket != null)
		{
			socket.close();
			socket = null;
		}
	}
	
	public boolean isConnected() {
		return socket != null && socket.isConnected();
	}
	
	public Message receive() throws IOException {
		if(isConnected() && socketOutput != null) {
			byte[] messageLengthBuffer = new byte[4];
			readBytes(messageLengthBuffer);
			int messageLength = ByteBuffer.wrap(messageLengthBuffer).getInt();
			if(messageLength < 1 || messageLength > maxMessageLength) {
				close();
				throw new IOException("Invalid message length (" + messageLength + ")");
			} else {
				byte[] messageBuffer = new byte[messageLength];
				readBytes(messageBuffer);
				ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(messageBuffer));
				Object obj = null;
				Message data = null;
				try {
					obj = in.readObject();
					data = (Message)obj;
				}
				catch (ClassNotFoundException e) {
					close();
					throw new IOException("Invalid message class ("  + (obj != null ? obj.getClass().getName() : "") + ").");
				}
				return data;
			}
		}
		else 
			throw new IOException("ChannelEndpoint is not connected");
	}
	
	private void readBytes(byte[] buffer) throws IOException {
		int len = buffer.length;
		int offset = 0;
		while(len > 0) {
			int rlen = socketInput.read(buffer, offset, len);
			len -= rlen;
		}
	}

	public synchronized void send(Message message) throws IOException {
		if(isConnected() && socketOutput != null) {
			byte[] rawMessage = message.toBytes();
			ByteBuffer messageLengthBuffer = ByteBuffer.allocate(4);
			messageLengthBuffer.putInt(rawMessage.length);
			socketOutput.write(messageLengthBuffer.array());
			socketOutput.write(rawMessage);
		}
		else 
			throw new IOException("ChannelEndpoint is not connected");
	}
	
	public synchronized int available() throws IOException {
		if(isConnected() && socketInput != null) {
			return socketInput.available();
		}
		else return 0;
	}
	
	private String host;
	private int port;
	private int maxMessageLength;
	private Socket socket;
	private InputStream socketInput;
	private OutputStream socketOutput;
}
