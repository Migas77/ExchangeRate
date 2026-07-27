package com.miguelbf.exchangerateapi.utilities;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import org.jspecify.annotations.Nullable;

public class LoggingEvents {

    private LoggingEvents() {
    }

    public static @Nullable Throwable getThrowable(ILoggingEvent event) {
        // Gets throwable from logging event
        if (event.getThrowableProxy() instanceof ThrowableProxy throwableProxy) {
            return throwableProxy.getThrowable();
        }
        return null;
    }

}
