package com.miguelbf.exchangerateapi.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;
import java.time.Instant;

public record JwtRefreshResponseDTO(
    @JsonProperty(required = true) String accessToken,
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
