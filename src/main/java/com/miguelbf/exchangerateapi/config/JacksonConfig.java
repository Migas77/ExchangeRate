package com.miguelbf.exchangerateapi.config;

import com.fasterxml.jackson.annotation.Nulls;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.EnumFeature;

@Configuration
public class JacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer jsonConfig() {
        return builder -> builder
            .changeDefaultNullHandling(handler -> handler
                // override with @JsonSetter
                .withValueNulls(Nulls.FAIL)
                .withContentNulls(Nulls.FAIL)
            )
            .configure(EnumFeature.FAIL_ON_NUMBERS_FOR_ENUMS, true)
            .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
            .configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, true)
            .enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
            ;
    }
}
