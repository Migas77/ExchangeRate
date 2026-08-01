package com.miguelbf.exchangerateapi.service;

import com.miguelbf.exchangerateapi.model.dto.AuthResponseDTO;
import com.miguelbf.exchangerateapi.model.dto.LoginRequestDTO;
import com.miguelbf.exchangerateapi.model.dto.SignUpRequestDTO;
import org.jspecify.annotations.Nullable;

public interface IAuthService {

    @Nullable AuthResponseDTO signup(SignUpRequestDTO signUpRequestDTO);

    @Nullable AuthResponseDTO login(LoginRequestDTO loginRequestDTO);

}
