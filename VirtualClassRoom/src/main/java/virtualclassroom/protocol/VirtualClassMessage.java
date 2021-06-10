package virtualclassroom.protocol;

import port_channel.Message;

public class VirtualClassMessage extends Message {

	private String nickname;
	private char[] teacherPassword;
	private static final long serialVersionUID = 1L;

	public static final int MESSAGETYPE_HELLO = 2;
	public static final int MESSAGETYPE_CHAT = 3;
	public static final int MESSAGETYPE_ADMIN = 4;
	
	public static VirtualClassMessage createVirtualClassMessage(int messageType) {
		return new VirtualClassMessage(Message.getNextMessageId(), messageType);
	}

	protected VirtualClassMessage() {
		super();
	}
	
	protected VirtualClassMessage(long messageId, int messageType) {
		super(messageId, messageType);
	}
	
	public String getNickname() {
		return nickname;
	}
	
	public void setNickname(String nickname) {
		this.nickname = nickname;
	}
	
	public char[] getTeacherPassword() {
		return teacherPassword;
	}

	public void setTeacherPassword(char[] teacherPassword) {
		this.teacherPassword = teacherPassword;
	}
}
