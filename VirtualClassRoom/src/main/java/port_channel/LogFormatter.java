package port_channel;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter {
    @Override
    public String format(LogRecord record) {
    	StringBuilder sb = new StringBuilder();
        sb.append(dateFormat.format(new Date(record.getMillis())))
          .append(" (").append(record.getThreadID())
          .append(") [").append(record.getLevel().getName()).append("] ")
          .append(record.getSourceClassName()).append('.').append(record.getSourceMethodName())
          .append(":  ").append(record.getMessage()).append('\n');
        
        Throwable t = record.getThrown();
        if(t != null) {
        	StringWriter sw = new StringWriter();
        	PrintWriter pw = new PrintWriter(sw);
        	t.printStackTrace(pw);
        	sb.append(sw.toString()).append('\n');
        }
        
        return sb.toString();
    }
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
}