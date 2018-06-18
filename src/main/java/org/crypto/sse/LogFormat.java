package org.crypto.sse;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LogFormat extends Formatter{

	@Override
	public String format (LogRecord rec) {
		return rec.getLoggerName() + ": " + rec.getMessage() + "\n";
	}
	
}
