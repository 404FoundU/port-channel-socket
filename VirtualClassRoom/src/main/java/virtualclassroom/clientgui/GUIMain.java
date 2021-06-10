package virtualclassroom.clientgui;

import java.awt.EventQueue;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import java.awt.BorderLayout;
import java.awt.Cursor;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.GridLayout;
import java.awt.Rectangle;

import javax.swing.border.BevelBorder;
import java.awt.event.ActionListener;
import java.util.Date;
import java.awt.event.ActionEvent;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import port_channel.Message;
import virtualclassroom.client.ClassroomClient;
import virtualclassroom.client.ClassroomMessageListener;
import virtualclassroom.protocol.VirtualClassMessage;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.text.SimpleDateFormat;

import javax.swing.ListSelectionModel;

public class GUIMain implements ClassroomMessageListener {
	
	private static final int MAX_MESSAGE_COUNT_IN_TABLE = 5000;
	private static final int CONNECTION_TIMEOUT = 5000;
	private static final String INFO_LABEL = "<Info>";
	private static final String ERROR_LABEL = "<Error>";

	private JFrame frame;
	private JMenuItem mntmConnectToClass = new JMenuItem("Connect to class");
	private JMenuItem mntmDisconnectFromClass = new JMenuItem("Disconnect");
	private JLabel lblStatusText = new JLabel(" ");
	private JTable messageTable;
	private JScrollPane messageTablePane;
	private JTextPane txtMessage;
	private JButton btnSendMessage;
	
	private boolean connected = false;
	private String host;
	private int port = 0;
	private String nickname = "";
	private boolean isTeacher = false;
	private ClassroomClient classroomClient;
	private javax.swing.Timer connectionTimer = new javax.swing.Timer(CONNECTION_TIMEOUT, new ActionListener() {
	      public void actionPerformed(ActionEvent evt) {
	    	  updateConnectionState();
	      }
	  });
	
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GUIMain window = new GUIMain();
					window.frame.setVisible(true);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});
	}

	class ConnectActionListener implements ActionListener {
		private GUIMain window;
		public ConnectActionListener(GUIMain window) {
			this.window = window;
		}
		public void actionPerformed(ActionEvent e) {
			ConnectDialog connectDialog = new ConnectDialog();
			connectDialog.setLocationRelativeTo(frame);
			try {
				connectDialog.setVisible(true);
				boolean result = connectDialog.getDialogResult();
				if(result) {
					window.connectToServer(
							connectDialog.getHost(), 
							connectDialog.getPort(), 
							connectDialog.getNickname(),
							connectDialog.isTeacherMode(),
							connectDialog.getTeacherPassword());
				}
			} finally {
				connectDialog.dispose();
			}
		}
	}

	class DisconnectActionListener implements ActionListener {
		private GUIMain window;
		public DisconnectActionListener(GUIMain window) {
			this.window = window;
		}
		public void actionPerformed(ActionEvent e) {
			window.disconnect();
		}
	}

	/**
	 * Create the application.
	 */
	public GUIMain() {
		initialize();
		setConnectedState(false);
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setTitle("Virtual Class Client");
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		JMenuItem mntmExit = new JMenuItem("Exit");
		mntmExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				System.exit(0);
			}
		});
		mnFile.add(mntmExit);
		
		JMenu mnSession = new JMenu("Session");
		menuBar.add(mnSession);
		
		mntmConnectToClass.addActionListener(new ConnectActionListener(this));
		mnSession.add(mntmConnectToClass);
		
		mntmDisconnectFromClass.setEnabled(false);
		mntmDisconnectFromClass.addActionListener(new DisconnectActionListener(this));
		mnSession.add(mntmDisconnectFromClass);
		
		JMenu mnMessages = new JMenu("Messages");
		menuBar.add(mnMessages);
		
		JMenuItem mntmClearMessages = new JMenuItem("Clear messages");
		mntmClearMessages.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DefaultTableModel model = (DefaultTableModel) messageTable.getModel();
				while(model.getRowCount() > 0) 
					model.removeRow(model.getRowCount() - 1);
			}
		});
		mnMessages.add(mntmClearMessages);
		
		JMenu mnHelp = new JMenu("Help");
		menuBar.add(mnHelp);
		
		JMenuItem mntmAbout = new JMenuItem("About...");
		mntmAbout.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				AboutBox aboutBox = new AboutBox();
				aboutBox.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				aboutBox.setLocationRelativeTo(frame);
				aboutBox.setVisible(true);
			}
		});
		mnHelp.add(mntmAbout);
		
		JPanel statusBar = new JPanel();
		statusBar.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		frame.getContentPane().add(statusBar, BorderLayout.SOUTH);
		statusBar.setLayout(new GridLayout(0, 1, 0, 0));
		
		lblStatusText.setHorizontalAlignment(SwingConstants.LEFT);
		statusBar.add(lblStatusText);
		
		JSplitPane splitPane = new JSplitPane();
		splitPane.setResizeWeight(0.8);
		splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		frame.getContentPane().add(splitPane, BorderLayout.CENTER);
		
		JPanel pnlSendMessage = new JPanel();
		splitPane.setRightComponent(pnlSendMessage);
		pnlSendMessage.setLayout(new GridLayout(0, 1, 0, 0));
		
		JSplitPane splMessages = new JSplitPane();
		splMessages.setResizeWeight(0.95);
		pnlSendMessage.add(splMessages);
		
		btnSendMessage = new JButton("Send");
		btnSendMessage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String text = txtMessage.getText();
				txtMessage.setText("");
				btnSendMessage.setEnabled(false);
				if(!text.isEmpty())
					postMessageToServer(text);
			}
		});
		btnSendMessage.setEnabled(false);
		splMessages.setRightComponent(btnSendMessage);
		
		txtMessage = new JTextPane();
		txtMessage.setToolTipText("Message");
		txtMessage.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				String text = txtMessage.getText();
				btnSendMessage.setEnabled(connected && !text.isEmpty());
			}
		});
		splMessages.setLeftComponent(txtMessage);
		
		JPanel pnlMessages = new JPanel();
		splitPane.setLeftComponent(pnlMessages);
		pnlMessages.setLayout(new GridLayout(1, 0, 0, 0));
		
		messageTable = new JTable();
		messageTable.setShowVerticalLines(false);
		messageTable.setShowHorizontalLines(false);
		messageTable.setShowGrid(false);
		messageTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		messageTable.setModel(
			new DefaultTableModel(
			new Object[][] {
			},
			new String[] {
				"Timestamp", "Sender", "Message"
			}
		) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			Class<?>[] columnTypes = new Class[] {
				String.class, String.class, String.class
			};
			public Class<?> getColumnClass(int columnIndex) {
				return columnTypes[columnIndex];
			}
			boolean[] columnEditables = new boolean[] {
				false, false, false
			};
			public boolean isCellEditable(int row, int column) {
				return columnEditables[column];
			}
		}
		);
		messageTable.getColumnModel().getColumn(0).setPreferredWidth(30);
		messageTable.getColumnModel().getColumn(1).setPreferredWidth(45);
		messageTable.getColumnModel().getColumn(2).setPreferredWidth(100);
		messageTablePane = new JScrollPane(messageTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED); 
		pnlMessages.add(messageTablePane);
	}

	@Override
	public void onClassroomMessage(Message msgData) {
		int messageType = msgData.getMessageType();
		switch(messageType) {
			case VirtualClassMessage.MESSAGETYPE_CHAT: {
				VirtualClassMessage vcMsgData = (VirtualClassMessage)msgData;
				String nickname = vcMsgData.getNickname();
				if(nickname == null) nickname = "<Unknown>";
				String text = vcMsgData.getText();
				if(text != null)
					pushIncomingMessage(nickname, text);
				break;
			}
			
			case VirtualClassMessage.MESSAGETYPE_ADMIN: {
				String text = msgData.getText();
				if(text != null)
					pushIncomingMessage("<ClassRoomServer>", text);
				break;
			}
			
			case VirtualClassMessage.MESSAGE_TYPE_CLIENT_CONNECTED: {
				connectionTimer.stop();
				updateConnectionState();
				pushIncomingMessage(INFO_LABEL, "Connected to server");
				break;
			}

			case VirtualClassMessage.MESSAGE_TYPE_CLIENT_DISCONNECTED: {
				disconnect();
				pushIncomingMessage(INFO_LABEL, "Disconnected from server");
				break;
			}

			case VirtualClassMessage.MESSAGE_TYPE_HEARTBEAT: {
				pushIncomingMessage(INFO_LABEL, "Got a heartbeat from server");
				break;
			}

			case VirtualClassMessage.MESSAGE_TYPE_COMMUNICATION_ERROR: {
				String text = msgData.getText();
				if(text != null) text = "";
				pushIncomingMessage(ERROR_LABEL, text);
				break;
			}
			
			default: break;
		}
	}

	private static final SimpleDateFormat messageTableDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
	
	private void pushIncomingMessage(String sender, String text) {
		String lines[] = text.split("\\r?\\n");
		
		DefaultTableModel model = (DefaultTableModel)messageTable.getModel();
		
		int expectedCount = model.getRowCount() + lines.length;
		while(model.getRowCount() > 0 && expectedCount > MAX_MESSAGE_COUNT_IN_TABLE) {
			model.removeRow(0);
			--expectedCount;
		}
		
		for(int i = 0; i < lines.length && model.getRowCount() < MAX_MESSAGE_COUNT_IN_TABLE; ++i)
			model.addRow(new Object[] { messageTableDateFormat.format(new Date()), sender, lines[i] });
		
		if(model.getRowCount() > 0) {
			Rectangle rect = messageTable.getCellRect(model.getRowCount() - 1, 0, true);
			messageTable.scrollRectToVisible(rect);
		}
	}
	

	public void connectToServer(String host, int port, String nickname, boolean isTeacher, char[] teacherPassword){
		if(host.isEmpty()) {
			JOptionPane.showMessageDialog(frame, "Invalid host", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		else if(port < 1024 || port > 65536) {
			JOptionPane.showMessageDialog(frame, "Invalid port", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		else if(nickname.isEmpty() || nickname.length() > 64) {
			JOptionPane.showMessageDialog(frame, "Invalid nickname", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		else if(isTeacher && (teacherPassword.length == 0 || teacherPassword.length > 64)) {
			JOptionPane.showMessageDialog(frame, "Invalid teacher password", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
        try {
            frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    		this.host = host;
    		this.port = port;
    		this.nickname = nickname;
    		this.isTeacher = isTeacher;
    		this.classroomClient = isTeacher 
    			? new ClassroomClient(host, port, nickname, teacherPassword, this)
    			: new ClassroomClient(host, port, nickname, this);
    		connectionTimer.setRepeats(false);
    		lblStatusText.setText("Connecting...");
    		Thread clientThread = new Thread(this.classroomClient);
    		clientThread.start();
    		connectionTimer.start();
        } finally {
            frame.setCursor(Cursor.getDefaultCursor());
        }
	}
	
	public void disconnect() {
		ClassroomClient client;
		synchronized(this) {
			client = this.classroomClient;
			this.classroomClient = null;
		}
		if(client != null) {
			client.requestTermination();
			client = null;
		}
		setConnectedState(false);
	}
	
	private void setConnectedState(boolean state) {
		connected = state;
		mntmConnectToClass.setEnabled(!connected);
		mntmDisconnectFromClass.setEnabled(connected);
		String statusText = connected 
				? "Connected to " + host + ":" + port + " as " + (isTeacher ? "teacher" : "student") + " @" + nickname
				: "Disconnected"; 
		lblStatusText.setText(statusText);
		if(!connected)
			btnSendMessage.setEnabled(false);
	}

	private void updateConnectionState() {
  	  boolean state = false;
  	  if(classroomClient != null)
  		  state = classroomClient.isConnected();
  	  setConnectedState(state);
	}
	
	private void postMessageToServer(String text) {
	  	  if(classroomClient != null) {
			try {
				classroomClient.sendChatMessage(text);
			} catch (IOException e) {
				e.printStackTrace();
				pushIncomingMessage("<Error>", e.getMessage());
				disconnect();
			}
	  	  }
	}
}
