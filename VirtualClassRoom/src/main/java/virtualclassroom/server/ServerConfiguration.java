package virtualclassroom.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ServerConfiguration {

	private static final String CONFIG_PARAM_PORT = "port";
	private static final String CONFIG_PARAM_TEACHER_PASSWORD = "teacher-password";

	private static final String DEFAULT_CONFIG_PARAM_PORT = "6999";
	private static final String DEFAULT_CONFIG_PARAM_TEACHER_PASSWORD = "teach";
	
	public static ServerConfiguration loadConfig(String configFileName) throws IOException {
		Properties  properties = loadProperties(configFileName);
		return new ServerConfiguration(properties);
	}
	
	public static ServerConfiguration getDefaultConfig() {
		return new ServerConfiguration(createDefaultProperties());
	}
	
	private ServerConfiguration(Properties config) {
		String port = config.getProperty(CONFIG_PARAM_PORT);
		this.port = Integer.parseInt(port != null ? port : DEFAULT_CONFIG_PARAM_PORT);
		String teacherPassword = config.getProperty(CONFIG_PARAM_TEACHER_PASSWORD);
		teacherPassword = teacherPassword != null ? teacherPassword : DEFAULT_CONFIG_PARAM_TEACHER_PASSWORD;
		if(teacherPassword.isEmpty()) teacherPassword = DEFAULT_CONFIG_PARAM_TEACHER_PASSWORD;
		this.teacherPassword = new char[teacherPassword.length()];
		teacherPassword.getChars(0, teacherPassword.length(), this.teacherPassword, 0);
	}
	
	private static Properties loadProperties(String configFile) throws IOException {
		Properties props = new Properties();
		FileInputStream in = new FileInputStream(configFile);
		props.load(in);
		return props;
	}

	private static Properties createDefaultProperties() {
		Properties props = new Properties();
		props.setProperty(CONFIG_PARAM_PORT, DEFAULT_CONFIG_PARAM_PORT);
		props.setProperty(CONFIG_PARAM_TEACHER_PASSWORD, DEFAULT_CONFIG_PARAM_TEACHER_PASSWORD);
		return props;
	}
	
	public int getPort() {
		return port;
	}
	
	public char[] getTeacherPassword() {
		return teacherPassword;
	}

	private int port;
	private char[] teacherPassword;

}
