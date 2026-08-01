package com.miguelbf.exchangerateapi.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record JwtRefreshRequestDTO(
    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$",
        message = "Malformed refresh token")
    @Size(max = 2048)
    String refreshToken
) {
}
