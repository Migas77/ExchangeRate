package com.miguelbf.exchangerateapi.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record LoginRequestDTO(
    @NotBlank
    @Email
    String email,

    @NotBlank
    @Length(min = 6, message = "Password must be at least 6 characters long")
    String password
) {
}
