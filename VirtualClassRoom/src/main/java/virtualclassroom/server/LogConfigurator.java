package virtualclassroom.server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public final class LogConfigurator {
	private LogConfigurator() {}
	
	private static final String[] logConfiguration = new String[] {
			"##### BEGIN LOG CONFIGURATION #####",
			"java.util.logging.ConsoleHandler.level = FINE",			
			"java.util.logging.ConsoleHandler.formatter = port_channel.LogFormatter",
			"handlers = java.util.logging.ConsoleHandler",		 
			"##### END LOG CONFIGURATION #####"
	};
	
	public static void configureLog() {
		log.setLevel(Level.FINEST);
		StringBuilder sbldr = new StringBuilder();
		for(String str: logConfiguration)
			sbldr.append(str).append('\n');
		try {
			LogManager lm = LogManager.getLogManager();
			ByteArrayInputStream is = new ByteArrayInputStream(sbldr.toString().getBytes(StandardCharsets.UTF_8));
            lm.readConfiguration(is);
            log.info("Log configuration updated");
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (SecurityException ex) {
            ex.printStackTrace();
        }		
	}
	private static Logger log = Logger.getLogger(LogConfigurator.class.getName());
}
