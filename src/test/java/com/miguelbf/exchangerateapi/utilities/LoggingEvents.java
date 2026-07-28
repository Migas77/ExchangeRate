package com.miguelbf.exchangerateapi.utilities;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.encoder.Encoder;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;

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

    public static @NonNull Appender<ILoggingEvent> getLogAppender(@NotNull String name) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        assertNotNull(loggerContext);
        Logger rootLogger = loggerContext.getLogger(ROOT_LOGGER_NAME);
        assertNotNull(rootLogger);
        Appender<ILoggingEvent> appender = rootLogger.getAppender(name);
        assertNotNull(appender);
        return appender;
    }

    public static <E extends Encoder<ILoggingEvent>> @NonNull E getAppenderEncoder(
        @NotNull String name,
        @NotNull Class<E> encoderType
    ) {
        Appender<ILoggingEvent> appender = getLogAppender(name);
        OutputStreamAppender<ILoggingEvent> outputAppender = assertInstanceOf(OutputStreamAppender.class, appender);
        return assertInstanceOf(encoderType, outputAppender.getEncoder());
    }

    public static @NonNull ByteArrayOutputStream getLoggerByteOutputStream(@NotNull String name) {
        Appender<ILoggingEvent> appender = getLogAppender(name);
        OutputStreamAppender<ILoggingEvent> outputAppender = (OutputStreamAppender<ILoggingEvent>) appender;
        ByteArrayOutputStream logOutput = new ByteArrayOutputStream();
        outputAppender.setOutputStream(logOutput);
        return logOutput;
    }

    public static @Nullable String getKeyLogAsString(
        @NonNull ByteArrayOutputStream byteArrayOutputStream,
        @NonNull ObjectMapper objectMapper,
        @NonNull String key
    ) {
        // We can distinguish between null (not present) and "null" (in the log)
        String str = byteArrayOutputStream.toString(Charset.defaultCharset());
        JsonNode node = objectMapper.readTree(str).get(key);
        if (node == null) {
            return null;
        }
        return node.toString();
    }

}
