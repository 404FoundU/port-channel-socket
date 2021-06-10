package port_channel;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;

public class MultiException extends Exception {
	public MultiException() {}
	
	public void addException(Exception ex) {
		if(ex == null) 
			throw new IllegalArgumentException("ex");
		else
			exceptions.add(ex);
	}
	
	@Override
	public void printStackTrace(PrintWriter w) {
		int i = 0;
		for(Exception ex: exceptions) {
			w.print("Exception #" + (i + 1) + ": ");
			ex.printStackTrace(w);
			++i;
		}
	}
	
	@Override
	public void printStackTrace(PrintStream s) {
		int i = 0;
		for(Exception ex: exceptions) {
			s.print("Exception #" + (i + 1) + ": ");
			ex.printStackTrace(s);
			++i;
		}
	}
	
	@Override
	public String getMessage() {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for(Exception ex: exceptions) {
			sb.append("Exception #").append(i + 1).append(": ").append(ex.getMessage());
		}
		return sb.toString();
	}

	@Override
	public String getLocalizedMessage() {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for(Exception ex: exceptions) {
			sb.append("Exception #").append(i + 1).append(": ").append(ex.getLocalizedMessage());
		}
		return sb.toString();
	}
	
	private ArrayList<Exception> exceptions = new ArrayList<Exception>();
	private static final long serialVersionUID = 1L;
}
