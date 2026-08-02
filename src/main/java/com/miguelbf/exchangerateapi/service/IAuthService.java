package com.miguelbf.exchangerateapi.service;

import com.miguelbf.exchangerateapi.model.dto.*;
import org.jspecify.annotations.Nullable;

public interface IAuthService {

    @Nullable AuthResponseDTO signup(SignUpRequestDTO signUpRequestDTO);

    @Nullable AuthResponseDTO login(LoginRequestDTO loginRequestDTO);

    @Nullable JwtRefreshResponseDTO refresh(JwtRefreshRequestDTO jwtRefreshRequestDTO);

}
