package com.miguelbf.exchangerateapi.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.miguelbf.exchangerateapi.entities.User;

import java.util.UUID;

public record UserResponseDTO(
    @JsonProperty(required = true) UUID id,
    @JsonProperty(required = true) String email
) {

    public static UserResponseDTO fromEntity(User user) {
        return new UserResponseDTO(
            user.getId(),
            user.getUsername()
        );
    }

}
