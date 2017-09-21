package org.domain.utils;

import org.domain.financial.messages.Message;

public interface Logger {
	static final int LOG_LEVEL_ERROR   = 0x00000001;
	static final int LOG_LEVEL_WARNING = 0x00000002 | 0x00000001;
	static final int LOG_LEVEL_INFO    = 0x00000004 | 0x00000002 | 0x00000001;
	static final int LOG_LEVEL_TRACE   = 0x00000008 | 0x00000004 | 0x00000002 | 0x00000001;
	static final int LOG_LEVEL_DEBUG   = 0x0000000A | 0x00000008 | 0x00000004 | 0x00000002 | 0x00000001;

	public void log(int logLevel, String header, String text, Message obj);

}
