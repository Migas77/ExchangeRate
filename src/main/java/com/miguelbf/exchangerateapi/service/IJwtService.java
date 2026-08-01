package com.miguelbf.exchangerateapi.service;

import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;

public interface IJwtService {

    Instant extractExpiration(String token);

    String generateToken(UserDetails user);

    String generateRefreshToken(UserDetails user);

}
