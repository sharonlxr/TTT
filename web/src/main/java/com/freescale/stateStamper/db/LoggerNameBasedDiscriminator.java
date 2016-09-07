package com.freescale.stateStamper.db;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.sift.Discriminator;


public class LoggerNameBasedDiscriminator implements Discriminator<ILoggingEvent> {

	private static String KEY = "loggerFileName";

	private boolean started;
	
	public String getDiscriminatingValue(ILoggingEvent iLoggingEvent) {
	    return iLoggingEvent.getLoggerName();
	}

	public String getKey() {
	    return KEY;
	}

	public void start() {
	    started = true;
	}

	public void stop() {
	    started = false;
	}

	public boolean isStarted() {
	    return started;
	}
    
}
