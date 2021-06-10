package virtualclassroom.client;

import port_channel.Message;

public interface ClassroomMessageListener {
	void onClassroomMessage(Message msgData);
}
