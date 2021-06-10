package virtualclassroom.clientgui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class AboutBox extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final JPanel contentPanel = new JPanel();

	class ButtonActionListener implements ActionListener {
		private AboutBox dialog;
		public ButtonActionListener(AboutBox dialog) {
			this.dialog = dialog;
		}
		public void actionPerformed(ActionEvent e) {
			dialog.setVisible(false);
		}
	}

	/**
	 * Create the dialog.
	 */
	public AboutBox() {
		setModal(true);
		setResizable(false);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setModalityType(ModalityType.APPLICATION_MODAL);
		setTitle("About Virtual Class Room Client");
		setBounds(100, 100, 426, 195);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		GridBagLayout gbl_contentPanel = new GridBagLayout();
		gbl_contentPanel.columnWidths = new int[]{317, 68, 0};
		gbl_contentPanel.rowHeights = new int[]{47, 25, 0};
		gbl_contentPanel.columnWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		gbl_contentPanel.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		contentPanel.setLayout(gbl_contentPanel);
		{
			JLabel lblProgramName = new JLabel("Virtual Class Room Client");
			lblProgramName.setFont(new Font("Tahoma", Font.BOLD, 20));
			GridBagConstraints gbc_lblProgramName = new GridBagConstraints();
			gbc_lblProgramName.gridwidth = 2;
			gbc_lblProgramName.insets = new Insets(0, 0, 5, 5);
			gbc_lblProgramName.gridx = 0;
			gbc_lblProgramName.gridy = 0;
			contentPanel.add(lblProgramName, gbc_lblProgramName);
		}
		{
			JLabel lblVersion = new JLabel("Version 1.0");
			lblVersion.setFont(new Font("Tahoma", Font.PLAIN, 14));
			GridBagConstraints gbc_lblVersion = new GridBagConstraints();
			gbc_lblVersion.gridwidth = 2;
			gbc_lblVersion.insets = new Insets(0, 0, 0, 5);
			gbc_lblVersion.gridx = 0;
			gbc_lblVersion.gridy = 1;
			contentPanel.add(lblVersion, gbc_lblVersion);
		}
		{
			JPanel buttonPane = new JPanel();
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			buttonPane.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
			{
				JButton okButton = new JButton("OK");
				okButton.addActionListener(new ButtonActionListener(this));
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
		}
	}

}
