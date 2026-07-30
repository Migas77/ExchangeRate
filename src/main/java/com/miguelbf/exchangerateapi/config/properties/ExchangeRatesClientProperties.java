package com.miguelbf.exchangerateapi.config.properties;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "app.clients.exchange-rates")
@Validated
@Getter
@Setter
public class ExchangeRatesClientProperties {

    @NotBlank
    @URL
    private String baseUrl;

    @NotBlank
    @Length(min = 32, max = 32)
    private String accessKey;

    @NotNull
    private Duration connectTimeout = Duration.ofSeconds(5);

    @NotNull
    private Duration readTimeout = Duration.ofSeconds(10);

    @NotNull
    private Duration cachingTTL = Duration.ofMinutes(1);

}
