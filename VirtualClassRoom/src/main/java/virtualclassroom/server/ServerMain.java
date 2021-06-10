package virtualclassroom.server;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import port_channel.*;
import virtualclassroom.protocol.Parameters;
import virtualclassroom.protocol.VirtualClassMessage;

public class ServerMain {

	private ServerConfiguration cfg;

	private HashSet<SelectionKey> allCons = new HashSet<SelectionKey>();
	private HashSet<SelectionKey> teacherCons = new HashSet<SelectionKey>();

	private ChannelPort channelPort = null;

	private static Logger log = Logger.getLogger(ServerMain.class.getName());

	public static void main(String[] args) {		
		try	{
			LogConfigurator.configureLog();
			ServerConfiguration config ;
			if(args.length > 0){
				config = ServerConfiguration.loadConfig(args[0]);
			}else{
				config = ServerConfiguration.getDefaultConfig();
			}

			ServerMain server = new ServerMain(config);
			server.execute();
		}catch(Exception e)	{
			log.log(Level.SEVERE, e.getMessage(), e);
			System.exit(1);
		}
		System.exit(0);
	}

	public ServerMain(ServerConfiguration config) {
		log.setLevel(Level.FINEST);
		this.cfg = config;
	}

	@SuppressWarnings("deprecation")
	public void execute() throws InterruptedException {
		log.info("Virtual Class Server is starting up");

		channelPort = new ChannelPort(cfg.getPort(), Parameters.MAX_MESSAGE_LENGTH);
		CountDownLatch countDownLatch = new CountDownLatch(1);
		channelPort.setCompletionObject(countDownLatch);
		Thread t = new Thread(channelPort);
		t.start();

		log.info("Entering message handling loop");
		displayLoad();

		while(true){
			try {

				MessageWithSource message = channelPort.receive();
				if(message == null) break;

				log.info("Received message: " + message.toString());

				int messageType = message.getMessage().getMessageType();	
				handleMessageType(messageType, message);

			}
			catch(Exception ex) {
				log.log(Level.SEVERE, ex.getMessage(), ex);
			}
		}

		t.stop();
		countDownLatch.await();
	}

	private void handleMessageType(int messageType, MessageWithSource meessage ){	

		try{
			switch(messageType){
			case VirtualClassMessage.MESSAGE_TYPE_CLIENT_CONNECTED: {
				handleClientConnected(meessage);
				break;
			}
			case VirtualClassMessage.MESSAGE_TYPE_CLIENT_DISCONNECTED: {
				handleClientDisconnected(meessage);
				break;
			}
			case VirtualClassMessage.MESSAGETYPE_HELLO: {
				handleHello(meessage);
				break;
			}
			case VirtualClassMessage.MESSAGETYPE_CHAT: {
				handleChat(meessage);
				break;
			}
			default: {
				log.warning("Unknown message type: " + messageType);
				break;
			}
			}
		}catch(IOException e){

		}
	}

	private void handleClientConnected(MessageWithSource msg) {
		SelectionKey key = msg.getSourceKey();
		log.info("Client ('" + key.toString() + "') connected.");
		// create new connection record
		ClientSession conn = new ClientSession();
		key.attach(conn);
		allCons.add(key);
		log.info("Client ('" + key.toString() + "') added.");		
		displayLoad();
	}

	private void handleClientDisconnected(MessageWithSource msg) throws IOException {
		SelectionKey key = msg.getSourceKey();
		log.info("Client ('" + key.toString() + "') disconnected.");
		// remove connection record
		allCons.remove(key);
		teacherCons.remove(key);
		log.info("Client ('" + key.toString() + "') removed.");
		log.info("Broadcasting 'somebody left classroom' notification...");
		displayLoad();		
		ClientSession conn = (ClientSession)key.attachment();
		if(conn != null && !conn.clientNotIdentified())
			broadcastAdminMessage((conn.clientIsTeacher() ? "Teacher" : "Student") + " @" + conn.getClientNickname() + " have left the virtual class room.");
		else
			broadcastAdminMessage("Somebody have left the virtual class room.");
		log.info("Broadcasted 'somebody left classroom' notification.");
	}

	private void handleHello(MessageWithSource msg) throws IOException {
		SelectionKey key = msg.getSourceKey();
		VirtualClassMessage data = (VirtualClassMessage)msg.getMessage();

		// get attached info object
		ClientSession conn = (ClientSession)key.attachment();

		// checked whether we have already had HELLO message
		if(conn != null && !conn.clientNotIdentified())
		{
			log.warning("Client '" + key.toString() + "' already identified as "
					+ (conn.clientIsTeacher() ? "teacher" : "student")
					+ " with nickname @" + conn.getClientNickname()
					+ ". This information cannot be changed."
					);
			return;
		}

		// parse nickname attribute
		String nickname = data.getNickname();
		if(nickname == null)
		{
			log.severe("Client '" + key.toString() + "': Invalid nickname.");
			return;
		}

		for(SelectionKey key1: allCons) {
			ClientSession conn1 = (ClientSession)key1.attachment();
			if(!conn1.clientNotIdentified() && conn1.getClientNickname().equals(nickname)) {
				log.warning("Client '" + key.toString() + "': nickname '" + nickname + "' is not unique.");
				sendAdminMessage(key, "Your nickname is not unique. Please reconnect with different nickname");
				return;
			}
		}

		conn.setClientNickname(nickname);

		// parse "is teacher" attribute
		boolean isTeacher = false;
		char[] teacherPwd = data.getTeacherPassword();
		if(teacherPwd != null)
		{
			char[] correctPwd = cfg.getTeacherPassword();
			isTeacher = Arrays.equals(teacherPwd, correctPwd);
			if(!isTeacher)
			{
				log.warning("Client '" + key.toString() + "' provided teacher password does not match!");
				sendAdminMessage(key, "Wrong teacher password!");
			}
		}

		conn.setClientIsTeacher(isTeacher);
		if(isTeacher)
		{
			teacherCons.add(key);
			displayLoad();
		}

		log.info("Client '" + key.toString() + "' identified as "
				+ (conn.clientIsTeacher() ? "teacher" : "student")
				+ " with nickname @" + conn.getClientNickname()
				+ "."
				);

		sendAdminMessage(key, "Hi, @" + conn.getClientNickname() + "!\nWelcome to the virtual class room!");
		log.info("Broadcasted 'somebody entered classroom' notification...");
		broadcastAdminMessage((isTeacher ? "Teacher" : "Student") + " @" + conn.getClientNickname() + " have entered the virtual class room.");
		log.info("Broadcasted 'somebody entered classroom' notification.");
	}

	private void handleChat(MessageWithSource msg) throws IOException {
		SelectionKey source = msg.getSourceKey();

		// get attached info object
		ClientSession conn = (ClientSession)source.attachment();

		// ignore not identified users
		if(conn.clientNotIdentified()) {
			log.info("Message from the non-identified client has been ignored.");
			return;
		}

		// add nickname to message
		VirtualClassMessage data = (VirtualClassMessage)msg.getMessage();
		data.setNickname(conn.getClientNickname());

		if(conn.clientIsTeacher()) {
			// broadcast message to everyone
			log.info("Broadcasting message " + msg.toString() + " to everyone...");
			channelPort.broadcast(msg.getMessage());
			log.info("Broadcasted message " + msg.toString() + " to everyone.");
		}else {
			// broadcast message to teachers
			MultiException mex = null;
			log.info("Broadcasting message " + msg.toString() + " to teachers...");
			for(SelectionKey target: teacherCons) {
				try {
					channelPort.send(target, msg.getMessage());
				}
				catch(IOException ex){
					if(mex == null)
						mex = new MultiException();
				}
			}
			if(mex != null)
				throw new IOException(mex);
			log.info("Broadcasted message " + msg.toString() + " to teachers.");
		}		
	}

	private void displayLoad() {
		log.info("CURRENT NUMBER OF CLIENTS: " + allCons.size());
		log.info("CURRENT NUMBER OF TEACHERS: " + teacherCons.size());
	}

	private Message createAdminMessage(String text) {
		VirtualClassMessage msgData = VirtualClassMessage.createVirtualClassMessage(VirtualClassMessage.MESSAGETYPE_ADMIN);
		msgData.setText(text);
		return msgData;
	}

	private void sendAdminMessage(SelectionKey source, String text) throws IOException {
		Message msg = createAdminMessage(text);
		channelPort.send(source, msg);	
	}

	private void broadcastAdminMessage(String text) throws IOException {
		Message msg = createAdminMessage(text);
		channelPort.broadcast(msg);	
	}
}
