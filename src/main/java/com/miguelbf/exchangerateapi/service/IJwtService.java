package com.miguelbf.exchangerateapi.service;

import org.springframework.security.core.userdetails.UserDetails;

public interface IJwtService {

    String extractSubject(String token);

    String generateToken(UserDetails user);

    String generateRefreshToken(UserDetails user);

    boolean isTokenValid(String token, UserDetails user);

    boolean isRefreshTokenValid(UserDetails user, String refreshToken);

}
