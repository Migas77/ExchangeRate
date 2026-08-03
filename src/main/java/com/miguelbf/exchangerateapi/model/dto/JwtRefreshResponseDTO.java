package com.miguelbf.exchangerateapi.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Duration;
import java.time.Instant;

public record JwtRefreshResponseDTO(
    @Schema(description = "JWT Access Token")
    @JsonProperty(required = true) String accessToken,

    @Schema(description = "Access Token expiration time in seconds", example = "899")
    @JsonProperty(required = true) long expiresIn
) {

    public static JwtRefreshResponseDTO fromTokenInfo(
        String accessToken,
        Instant expiresIn
    ) {
        return new JwtRefreshResponseDTO(
            accessToken,
            Duration.between(Instant.now(), expiresIn).toSeconds()
        );
    }
}
