package com.miguelbf.exchangerateapi.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequestDTO(
    @NotBlank
    @Email
    @Schema(description = "User email", example = "user@example.com")
    String email,

    @NotBlank
    @Size(min = 6, max = 128, message = "Password must be between 6 and 128 characters long")
    @Schema(description = "User password", example = "password")
    String password
) {
}
