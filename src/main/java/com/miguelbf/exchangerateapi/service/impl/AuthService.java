package com.miguelbf.exchangerateapi.service.impl;

import com.miguelbf.exchangerateapi.entities.User;
import com.miguelbf.exchangerateapi.entities.UserRole;
import com.miguelbf.exchangerateapi.model.dto.*;
import com.miguelbf.exchangerateapi.service.IAuthService;
import com.miguelbf.exchangerateapi.service.IJwtService;
import com.miguelbf.exchangerateapi.service.IUserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class AuthService implements IAuthService {

    private final IJwtService jwtService;
    private final IUserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Qualifier("refreshJwtDecoder")
    private final JwtDecoder refreshJwtDecoder;

    @Override
    public @Nullable AuthResponseDTO signup(SignUpRequestDTO signUpRequestDTO) {
        if (userService.getUserByEmail(signUpRequestDTO.email()) != null) {
            log.atWarn().setMessage("User email already exists").addKeyValue("email", signUpRequestDTO.email()).log();
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

    @Override
    public @Nullable AuthResponseDTO login(LoginRequestDTO loginRequestDTO) throws AuthenticationException {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(loginRequestDTO.email(), loginRequestDTO.password())
        );
        User user = (User) authentication.getPrincipal();
        if (user == null) {
            log.atWarn().setMessage("Null principal during login").addKeyValue("email", loginRequestDTO.email()).log();
            return null;
        }
        String accessToken = jwtService.generateToken(user);
        return AuthResponseDTO.fromEntityAndTokenInfo(
            user,
            accessToken,
            jwtService.generateRefreshToken(user),
            jwtService.extractExpiration(accessToken)
        );
    }

    @Override
    public @Nullable JwtRefreshResponseDTO refresh(
        JwtRefreshRequestDTO jwtRefreshRequestDTO
    ) throws InvalidBearerTokenException, AuthenticationServiceException {
        Jwt jwt = this.getJwt(jwtRefreshRequestDTO);
        String subject = jwt.getSubject();
        if (subject == null) {
            log.atWarn().setMessage("Refresh token null subject").addKeyValue("jwtId", jwt.getId()).log();
            return null;
        }
        User user = userService.getUserByEmail(subject);
        if (user == null) {
            log.atWarn().setMessage("User not found for jwt subject")
                .addKeyValue("subject", subject).addKeyValue("jwtId", jwt.getId()).log();
            return null;
        }
        String accessToken = jwtService.generateToken(user);
        return JwtRefreshResponseDTO.fromTokenInfo(
            accessToken,
            jwtService.extractExpiration(accessToken)
        );
    }

    private Jwt getJwt(
        JwtRefreshRequestDTO jwtRefreshRequestDTO
    ) throws InvalidBearerTokenException, AuthenticationServiceException {
        // same behavior of org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider
        try {
            return refreshJwtDecoder.decode(jwtRefreshRequestDTO.refreshToken());
        } catch (BadJwtException failed) {
            throw new InvalidBearerTokenException((failed.getMessage() != null) ? failed.getMessage() : "Invalid token",
                failed);
        } catch (JwtException failed) {
            throw new AuthenticationServiceException(
                (failed.getMessage() != null) ? failed.getMessage() : "Invalid token", failed);
        }
    }

}
