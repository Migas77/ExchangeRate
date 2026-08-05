package com.miguelbf.exchangerateapi.config.properties;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "app.rate-limit")
@Validated
@Getter
@Setter
public class RateLimitProperties {

    @Positive
    private long capacity = 10;

    @NotNull
    private Duration period = Duration.ofMinutes(1);

    @NotNull
    private Duration timeout = Duration.ofMillis(500);

}