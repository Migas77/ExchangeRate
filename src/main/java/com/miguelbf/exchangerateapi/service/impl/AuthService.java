package com.miguelbf.exchangerateapi.service.impl;

import com.miguelbf.exchangerateapi.entities.User;
import com.miguelbf.exchangerateapi.entities.UserRole;
import com.miguelbf.exchangerateapi.model.dto.AuthResponseDTO;
import com.miguelbf.exchangerateapi.model.dto.SignUpRequestDTO;
import com.miguelbf.exchangerateapi.service.IAuthService;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthService implements IAuthService {

    private final JwtService jwtService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public @Nullable AuthResponseDTO signup(SignUpRequestDTO signUpRequestDTO) {
        if (userService.getUserByEmail(signUpRequestDTO.email()) != null) {
            // user already exists with same email
            return null;
        }
        User newUser = userService.createUser(new User(
            signUpRequestDTO.email(),
            passwordEncoder.encode(signUpRequestDTO.password()),
            UserRole.FREE_TIER
        ));
        String accessToken = jwtService.generateToken(newUser);
        return AuthResponseDTO.fromEntityAndTokenInfo(
            newUser,
            accessToken,
            jwtService.generateRefreshToken(newUser),
            jwtService.extractExpiration(accessToken)
        );
    }

}
