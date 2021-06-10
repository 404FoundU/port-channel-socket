package virtualclassroom.clientgui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JCheckBox;
import org.eclipse.wb.swing.FocusTraversalOnArray;
import java.awt.Component;
import javax.swing.JPasswordField;

public class ConnectDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private boolean dialogResult = false;
	
	private final JPanel contentPanel = new JPanel();
	private JTextField txtPort;
	private JTextField txtNickname;
	private JTextField txtServer;
	private JCheckBox chckbxTeacher = new JCheckBox("Connect as Teacher");
	private JPasswordField txtTeacherPassword;
	private JPanel buttonPane;
	private JButton btnConnect;
	private JButton btnCancel;

	class ButtonActionListener implements ActionListener {
		private ConnectDialog dialog;
		public ButtonActionListener(ConnectDialog dialog) {
			this.dialog = dialog;
		}
		public void actionPerformed(ActionEvent e) {
			JButton source = (JButton)e.getSource();
			dialog.dialogResult = source.getText().equals("Connect");
			dialog.setVisible(false);
		}
	}

	class TeacherCheckBoxActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			JCheckBox source = (JCheckBox)e.getSource();
			txtTeacherPassword.setEnabled(source.isSelected());
		}
	}

	/**
	 * Create the dialog.
	 */
	public ConnectDialog() {
		setModal(true);
		setModalityType(ModalityType.APPLICATION_MODAL);
		setTitle("Connect to Virtual Class Room");
		setBounds(100, 100, 243, 298);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(null);
		
		JLabel lblServer = new JLabel("Server");
		lblServer.setBounds(10, 11, 86, 14);
		contentPanel.add(lblServer);
		
		JLabel lblPort = new JLabel("Port");
		lblPort.setBounds(10, 58, 86, 14);
		contentPanel.add(lblPort);
		
		txtPort = new JTextField();
		txtPort.setText("6999");
		txtPort.setBounds(10, 71, 86, 20);
		contentPanel.add(txtPort);
		txtPort.setColumns(10);
		
		JLabel lblNickname = new JLabel("Nickname");
		lblNickname.setBounds(10, 106, 86, 14);
		contentPanel.add(lblNickname);
		
		txtNickname = new JTextField();
		txtNickname.setBounds(10, 119, 207, 20);
		contentPanel.add(txtNickname);
		txtNickname.setColumns(10);
		
		txtServer = new JTextField();
		txtServer.setText("127.0.0.1");
		txtServer.setBounds(10, 24, 207, 20);
		contentPanel.add(txtServer);
		txtServer.setColumns(10);
		
		chckbxTeacher.addActionListener(new TeacherCheckBoxActionListener());		
		chckbxTeacher.setBounds(10, 146, 207, 23);
		contentPanel.add(chckbxTeacher);
		
		JLabel lblTeacherPassword = new JLabel("Teacher Password");
		lblTeacherPassword.setBounds(10, 177, 157, 14);
		contentPanel.add(lblTeacherPassword);
		
		txtTeacherPassword = new JPasswordField();
		txtTeacherPassword.setEnabled(false);
		txtTeacherPassword.setBounds(10, 191, 207, 20);
		contentPanel.add(txtTeacherPassword);
		txtTeacherPassword.setColumns(10);
		{
			buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				btnConnect = new JButton("Connect");
				btnConnect.addActionListener(new ButtonActionListener(this));
				buttonPane.add(btnConnect);
				getRootPane().setDefaultButton(btnConnect);
			}
			{
				btnCancel = new JButton("Cancel");
				btnCancel.addActionListener(new ButtonActionListener(this));
				buttonPane.add(btnCancel);
			}
		}
		setFocusTraversalPolicy(new FocusTraversalOnArray(new Component[]{getContentPane(), contentPanel, lblServer, txtServer, lblPort, txtPort, lblNickname, txtNickname, chckbxTeacher, lblTeacherPassword, txtTeacherPassword, buttonPane, btnConnect, btnCancel}));
	}

	public boolean getDialogResult() {
		return dialogResult;
	}
	
	public String getHost() {
		return txtServer.getText().trim();
	}
	
	public int getPort() {
		return Integer.parseInt(txtPort.getText().trim());
	}

	public String getNickname() {
		return txtNickname.getText().trim();
	}
	
	public boolean isTeacherMode() {
		return chckbxTeacher.isSelected();
	}

	public char[] getTeacherPassword() {
		return txtTeacherPassword.getPassword();
	}
}
