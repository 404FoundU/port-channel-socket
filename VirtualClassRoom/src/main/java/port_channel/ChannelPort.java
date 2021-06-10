package port_channel;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChannelPort implements Runnable
{
	public ChannelPort(int port, int maxMessageLength) {
		log.setLevel(Level.FINEST);
		this.port = port;
		this.maxMessageLength = maxMessageLength;
	}
	
	public int getPort() { return port; }
	
	public synchronized void setCompletionObject(CountDownLatch completionObject) {
		this.completionObject = completionObject;
	}

	public synchronized CountDownLatch getCompletionObject() {
		return completionObject;
	}
	
	public synchronized MessageWithSource receive() {
		while (queue.isEmpty()) { 
			try {
				wait(); 
			} catch (InterruptedException ex) {
				if(Thread.currentThread().isInterrupted())
					return null;
				else
					log.log(Level.SEVERE, "Error", ex);
			}
		} 
		return queue.poll(); 
	}

	private synchronized void addMessage(MessageWithSource message) {
		queue.offer(message); 
		notifyAll();
	} 

	public void send(SelectionKey target, Message message) throws IOException {
		log.fine("Sending message " + message.toString() + " to " + target.toString());
		sendBytes(target, message.toBytes());
	}
	
	public void broadcast(Message message) throws IOException {
		// Capture active connections
		log.info("Broadcasting message " + message.toString());
		HashSet<SelectionKey> targets = new HashSet<SelectionKey>();
		synchronized(this){
			targets.addAll(connections);
		}
		
		if(!targets.isEmpty()) {
			byte[] rawMessage = message.toBytes();
			MultiException mex = null;
			for(SelectionKey target: targets)
			{
				try {
					log.fine(">>> Broadcasting message " + message.toString() + " to " + target.toString());
					sendBytes(target, rawMessage);
				}
				catch(IOException ex) {
					if(mex == null)
						mex = new MultiException();
					mex.addException(ex);
				}
			}
			
			if(mex != null)
				throw new IOException(mex);
		}
	}

	private void sendBytes(SelectionKey target, byte[] bytes) throws IOException {
		SocketChannel channel = (SocketChannel)target.channel();
		ByteBuffer messageLengthBuffer = ByteBuffer.allocate(4);
		messageLengthBuffer.putInt(bytes.length);
		messageLengthBuffer.rewind();
		channel.write(messageLengthBuffer);
		ByteBuffer messageBuffer = ByteBuffer.wrap(bytes);
		channel.write(messageBuffer);		
	}

	@Override
	public void run() {
		if(setup()) {
			while(handleConnections()) {}
		}
		signalThreadCompletion();
	}
	
	private boolean setup() {
		try {				
			// Create new server channel
			ServerSocketChannel serverChannel = ServerSocketChannel.open(); 
			
			// configure server channel
			serverChannel.configureBlocking(false);
			ServerSocket socket = serverChannel.socket();
			InetSocketAddress address = new InetSocketAddress(port);
			socket.bind(address); 
			
			// register channel in selector 
			selector = Selector.open();
			serverKey = serverChannel.register(selector, SelectionKey.OP_ACCEPT);
			log.info("Listening on the port " + port + "...");
			return true;
		} 
		catch (IOException e) {
			log.log(Level.SEVERE, "Failed in configuration.", e);
			signalThreadCompletion();
			return false;
		} 		
	}
	
	private boolean handleConnections() {
		try {
			// THE blocking multiple-way events listener 
			log.fine("Debug: Calling select()...");
			int readySetSize = selector.select();
			log.fine("Debug: Ready-set size = " + readySetSize);
			if(readySetSize <= 0) return true;
			
			log.info("The current ready-set size = " + readySetSize);

			// looping through the ready set 
			Set<SelectionKey> selectedKeys = selector.selectedKeys();
			Iterator<SelectionKey> it = selectedKeys.iterator();
			while (it.hasNext()) {
				SelectionKey key = (SelectionKey) it.next();
				log.fine("Processing " + key.toString() + "...");
				if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
					log.fine(key.toString() + " indicates new incoming connection...");
					acceptNewConnection(key);
					log.fine("New connection accepted.");
				}
				else if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
					try {
						log.fine(key.toString() + " has something to read...");
						readConnection(key);
						log.fine("Data read successfully.");
					} catch (IOException ex) {
						log.log(Level.SEVERE, ex.getMessage(), ex);
						shutdownChannel(key);
					}
				}
				// Remove from input set
				it.remove();
			} // while(has next key)
		} catch (ClosedChannelException ex) {
			log.warning("Warning: A client terminated.");
		} catch (IOException ex) { 
			log.log(Level.SEVERE, ex.getMessage(), ex);
		} catch (InterruptedException ex) {
			if(Thread.currentThread().isInterrupted()) 
				return false; // exit the loop
			else
				log.log(Level.SEVERE, "Error", ex);
		} 
		return true;
	}
	
	private void acceptNewConnection(SelectionKey key) throws IOException {
		// Get the server channel
		ServerSocketChannel channel = (ServerSocketChannel) key.channel(); 
		
		// Accept the new connection 
		SocketChannel socketChannel = channel.accept(); 
		
		// Configure the channel for communication 
		socketChannel.configureBlocking(false);
		
		// Add the new connection to the same selector
		SelectionKey key1 = socketChannel.register(selector, SelectionKey.OP_READ);
		synchronized(this) {
			connections.add(key1);
		}
		log.info("Incoming connection: " + socketChannel);
		addMessage(new MessageWithSource(key1, Message.createMessage(Message.MESSAGE_TYPE_CLIENT_CONNECTED)));
	}
	
	private void readConnection(SelectionKey key) throws IOException, InterruptedException {
		// get data channel
		if(key.isValid()) {
			sendHeartbeat(key);
			SocketChannel channel = (SocketChannel) key.channel();
			// read message length
			ByteBuffer messageLengthBuffer = readBytes(channel, 4);
			messageLengthBuffer.rewind();
			int messageLength = messageLengthBuffer.getInt();
			if(messageLength < 1 || messageLength > maxMessageLength) {
				log.severe(key.toString() +  ": Invalid message length " + messageLength
						+ ". Closing this connection.");
				shutdownChannel(key);
			}
			else {
				ByteBuffer messageBuffer = readBytes(channel, messageLength);
				//messageBuffer.rewind();
				byte[] bytes = messageBuffer.array();
				ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
				Message data;
				try {
					data = (Message)in.readObject();
					addMessage(new MessageWithSource(key, data));
				}
				catch (ClassNotFoundException e) {
					log.severe(key.toString() +  ": Invalid message class. Details: " + e.getMessage()
							+ ". Closing this connection.");
					shutdownChannel(key);
				}
			}
		}
		else shutdownChannel(key);		
	}
	
	private void signalThreadCompletion() {
		// exit thread
		CountDownLatch latch = getCompletionObject();
		if(latch != null)
			latch.countDown();		
	}
	
	private ByteBuffer readBytes(SocketChannel channel, int length) throws IOException, InterruptedException {
		ByteBuffer buffer = ByteBuffer.allocate(length);
		do {
			if(buffer.remaining() < length) Thread.sleep(1); // enforce synchronous reading
			channel.read(buffer);
		} while(buffer.remaining() > 0);
		return buffer;
	}
	
	private void shutdownChannel(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		synchronized(this) {
			connections.remove(key);
		}
		channel.close();
		addMessage(new MessageWithSource(key, Message.createMessage(Message.MESSAGE_TYPE_CLIENT_DISCONNECTED)));
	}
	
	private void sendHeartbeat(SelectionKey key) throws IOException {
		send(key, Message.createMessage(Message.MESSAGE_TYPE_HEARTBEAT));
	}
	
	public SelectionKey getServerKey() {
		return serverKey;
	}

	private int port;
	private int maxMessageLength;
	private CountDownLatch completionObject = null;
	private Selector selector;
	private HashSet<SelectionKey> connections = new HashSet<SelectionKey>();
	private ConcurrentLinkedQueue<MessageWithSource> queue = new ConcurrentLinkedQueue<MessageWithSource>();
	private SelectionKey serverKey;
	private static Logger log = Logger.getLogger(ChannelPort.class.getName());
}
