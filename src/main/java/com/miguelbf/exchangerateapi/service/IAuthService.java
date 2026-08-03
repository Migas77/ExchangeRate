package com.miguelbf.exchangerateapi.service;

import com.miguelbf.exchangerateapi.model.dto.*;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

public interface IAuthService {

    @Nullable AuthResponseDTO signup(SignUpRequestDTO signUpRequestDTO);

    @Nullable AuthResponseDTO login(LoginRequestDTO loginRequestDTO) throws AuthenticationException;

    @Nullable JwtRefreshResponseDTO refresh(JwtRefreshRequestDTO jwtRefreshRequestDTO) throws InvalidBearerTokenException, AuthenticationServiceException;

}
