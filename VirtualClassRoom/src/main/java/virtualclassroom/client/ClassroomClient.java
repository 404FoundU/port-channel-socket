package virtualclassroom.client;

import java.io.IOException;

import port_channel.ChannelEndpoint;
import port_channel.Message;
import virtualclassroom.protocol.Parameters;
import virtualclassroom.protocol.VirtualClassMessage;

public class ClassroomClient implements Runnable {

	private String host;
	private int port;
	private String nickname;
	private char[] teacherPassword;
	private ClassroomMessageListener listener;
	private boolean terminateRequested = false;
	private ChannelEndpoint endpoint;
	
	public ClassroomClient(String host, int port, String nickname, ClassroomMessageListener listener) {
		this.host = host;
		this.port = port;
		this.nickname = nickname;
		this.listener = listener;
	}
	
	public ClassroomClient(String host, int port, String nickname, char[] teacherPassword, ClassroomMessageListener listener) {
		this.host = host;
		this.port = port;
		this.nickname = nickname;
		this.teacherPassword = teacherPassword;	
		this.listener = listener;
	}
	
	public synchronized boolean isConnected() {
		return endpoint != null && endpoint.isConnected();
	}
	
	public synchronized void sendChatMessage(String chatMessage) throws IOException {
		if(isConnected()) {
			VirtualClassMessage msg = VirtualClassMessage.createVirtualClassMessage(VirtualClassMessage.MESSAGETYPE_CHAT);
			msg.setText(chatMessage);
			endpoint.send(msg);
		}
		else throw new IOException("Not connected");
	}

	@Override
	public void run() {
	
		synchronized(this) {
			endpoint = new ChannelEndpoint(host, port, Parameters.MAX_MESSAGE_LENGTH);
		}
		
		try {
			endpoint.open();
			VirtualClassMessage msgData = VirtualClassMessage.createVirtualClassMessage(VirtualClassMessage.MESSAGETYPE_HELLO);
			msgData.setNickname(nickname);
			if(teacherPassword != null)
				msgData.setTeacherPassword(teacherPassword);
			endpoint.send(msgData);
		} catch(IOException ex) {
			ex.printStackTrace();
			notifyError(ex);
			try {
				endpoint.close();
			} catch (IOException ex2) {
				notifyError(ex2);
				ex2.printStackTrace();
			}
			endpoint = null;
			notifyDisconnected();
			return;
		}
		
		listener.onClassroomMessage(Message.createMessage(VirtualClassMessage.MESSAGE_TYPE_CLIENT_CONNECTED));
		
		while(true){
			try {
				while(!isTerminateRequested() && endpoint.available() < 1) {
					Thread.sleep(200);
				}
				if(isTerminateRequested()) {
					break;
				}
				Message msgData = endpoint.receive();
				listener.onClassroomMessage(msgData);
			} catch(IOException ex) {
				ex.printStackTrace();
				notifyError(ex);
				try {
					endpoint.close();
				} catch (IOException ex2) {
					ex2.printStackTrace();
					notifyError(ex2);
				}
				endpoint = null;
				notifyDisconnected();
				break;
			} catch (InterruptedException ex) {
				if(Thread.currentThread().isInterrupted())
					break;
				else{
					ex.printStackTrace();
					notifyError(ex);
				}
			}
		}
		
		try {
			endpoint.close();
		} catch (IOException ex2) {
			ex2.printStackTrace();
			notifyError(ex2);
		}
		endpoint = null;
		notifyDisconnected();
	}
	
	public synchronized boolean isTerminateRequested() {
		return terminateRequested;
	}

	public synchronized void requestTermination() {
		terminateRequested = true;
	}
	
	private void notifyDisconnected() {
		listener.onClassroomMessage(Message.createMessage(VirtualClassMessage.MESSAGE_TYPE_CLIENT_DISCONNECTED));
	}
	
	private void notifyError(Throwable t) {
		Message msgData = Message.createMessage(VirtualClassMessage.MESSAGE_TYPE_COMMUNICATION_ERROR);
		msgData.setText(t.getMessage());
		listener.onClassroomMessage(msgData);
	}
}
