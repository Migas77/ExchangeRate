package com.miguelbf.exchangerateapi.config.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;

import java.util.Collections;
import java.util.Map;

public class CustomLoggingJsonMembersCustomizer implements StructuredLoggingJsonMembersCustomizer<ILoggingEvent> {

    @Override
    public void customize(JsonWriter.Members<ILoggingEvent> members) {
        members.addMapEntries(this::extractCustomLogStructureFields);
    }

    private Map<String, Object> extractCustomLogStructureFields(ILoggingEvent event) {
        if (event.getThrowableProxy() instanceof ThrowableProxy throwableProxy
            && throwableProxy.getThrowable() instanceof CustomLogStructureFields structuredException
        ) {
            return structuredException.getLogStructureFields();
        }
        return Collections.emptyMap();
    }

}
