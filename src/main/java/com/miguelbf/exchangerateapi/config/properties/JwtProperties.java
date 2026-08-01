package com.miguelbf.exchangerateapi.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "app.jwt")
@Validated
@Getter
@Setter
public class JwtProperties {

    @NotBlank
    @Length(min = 32, message = "Signing key must be at least 32 bytes (256 bits) for HS256")
    private String jwtSigningKey;

    @NotNull
    private Duration accessExpTime;

    @NotNull
    private Duration refreshExpTime;

}
