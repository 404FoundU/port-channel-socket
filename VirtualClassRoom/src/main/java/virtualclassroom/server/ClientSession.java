package virtualclassroom.server;

public class ClientSession {
	
	private ClientType clientType = ClientType.UNKNOWN;
	private String clientNickname = "";

	private enum ClientType {
		UNKNOWN,
		STUDENT,
		TEACHER
	}
	
	public ClientSession() {
	}
	
	public boolean clientNotIdentified() {
		return clientType == ClientType.UNKNOWN;
	}

	public boolean clientIsTeacher() {
		return clientType == ClientType.TEACHER;
	}

	public void setClientIsTeacher(boolean isTeacher) {
		clientType = isTeacher ? ClientType.TEACHER : ClientType.STUDENT;
	}
	
	public String getClientNickname() {
		return clientNickname;
	}
	
	public void setClientNickname(String nickname) {
		if(nickname == null)
			throw new IllegalArgumentException("nickname");
		this.clientNickname = nickname;
	}
}
