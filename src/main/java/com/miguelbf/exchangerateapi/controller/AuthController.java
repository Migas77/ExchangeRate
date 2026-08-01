package com.miguelbf.exchangerateapi.controller;

import com.miguelbf.exchangerateapi.model.dto.*;
import com.miguelbf.exchangerateapi.service.impl.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponseDTO> signUp(
        @Valid @RequestBody SignUpRequestDTO signUpRequestDTO
    ) {
        AuthResponseDTO authResponseDTO = authService.signup(signUpRequestDTO);
        if (authResponseDTO == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponseDTO);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
        @Valid @RequestBody LoginRequestDTO loginRequestDTO
    ) {
        AuthResponseDTO authResponseDTO = authService.login(loginRequestDTO);
        if (authResponseDTO == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(authResponseDTO);
    }

}
