package com.miguelbf.exchangerateapi.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.miguelbf.exchangerateapi.entities.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public record AuthResponseDTO(
    @Schema(description = "User Id", example = "1798b49e-761d-4e97-bec9-ddb19239e65d")
    @JsonProperty(required = true) UUID id,

    @Schema(description = "User email", example = "user@example.com")
    @JsonProperty(required = true) String email,

    @Schema(description = "JWT Access Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.KMUFsIDTnFmyG3nMiGM6H9FNFUROf3wh7SmqJp-QV30")
    @JsonProperty(required = true) String accessToken,

    @Schema(description = "JWT Refresh Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.KMUFsIDTnFmyG3nMiGM6H9FNFUROf3wh7SmqJp-QV31")
    @JsonProperty(required = true) String refreshToken,

    @Schema(description = "Access Token expiration time in seconds", example = "899")
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
