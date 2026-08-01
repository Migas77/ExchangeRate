package com.miguelbf.exchangerateapi.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.miguelbf.exchangerateapi.entities.User;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public record AuthResponseDTO(
    @JsonProperty(required = true) UUID id,
    @JsonProperty(required = true) String email,
    @JsonProperty(required = true) String accessToken,
    @JsonProperty(required = true) String refreshToken,
    @JsonProperty(required = true) long expiresIn
) {

    public static AuthResponseDTO fromEntityAndTokenInfo(
        User user,
        String accessToken,
        String refreshToken,
        Instant expiresIn
    ) {
        return new AuthResponseDTO(
            user.getId(),
            user.getUsername(),
            accessToken,
            refreshToken,
            Duration.between(Instant.now(), expiresIn).toSeconds()
        );
    }
}
