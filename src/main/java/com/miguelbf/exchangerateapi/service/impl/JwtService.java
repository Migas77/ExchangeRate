package com.miguelbf.exchangerateapi.service.impl;

import com.miguelbf.exchangerateapi.config.properties.JwtProperties;
import com.miguelbf.exchangerateapi.service.IJwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService implements IJwtService {

    private final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @Override
    public String extractSubject(String token) {
        return this.extractClaim(token, Claims::getSubject);
    }

    @Override
    public String generateToken(UserDetails user) {
        return this.generateJwtToken(Map.of(), user, this.jwtProperties.getAccessExpTime());
    }

    @Override
    public boolean isTokenValid(String token, UserDetails user) {
        return extractSubject(token).equals(user.getUsername()) && !isTokenExpired(token);
    }

    @Override
    public String generateRefreshToken(UserDetails user) {
        return this.generateJwtToken(Map.of("type", "refresh"), user, this.jwtProperties.getRefreshExpTime());
    }

    @Override
    public boolean isRefreshTokenValid(UserDetails user, String refreshToken) {
        final Claims refreshClaims = extractAllClaims(refreshToken);
        return isTokenValid(refreshToken, user)
            && refreshClaims.containsKey("type")
            && refreshClaims.get("type").equals("refresh");
    }

    private String generateJwtToken(Map<String, Object> extraClaims, UserDetails user, Duration expirationTime) {
        Instant now = Instant.now();
        return Jwts.builder()
            .claims()
            .empty()
            .add(extraClaims)
            .subject(user.getUsername())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(expirationTime)))
            .and()
            .signWith(this.getSigningKey())
            .compact();
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).toInstant().isBefore(Instant.now());
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = this.jwtProperties.getJwtSigningKey().getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser().verifyWith(this.getSigningKey()).build().parseSignedClaims(token).getPayload();
    }
}
