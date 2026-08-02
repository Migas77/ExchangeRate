package com.miguelbf.exchangerateapi.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDTO(
    @NotBlank
    @Email
    String email,

    @NotBlank
    @Size(min = 6, max = 128, message = "Password must be between 6 and 128 characters long")
    String password
) {
}
