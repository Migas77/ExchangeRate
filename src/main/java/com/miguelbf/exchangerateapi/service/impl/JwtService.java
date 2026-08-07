package com.miguelbf.exchangerateapi.service.impl;

import com.miguelbf.exchangerateapi.config.properties.JwtProperties;
import com.miguelbf.exchangerateapi.service.IJwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService implements IJwtService {

    private final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @Override
    public Instant extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration).toInstant();
    }

    @Override
    public String generateToken(UserDetails user) {
        return this.generateJwtToken(Map.of(
            "role", this.extractRole(user)
        ), user, this.jwtProperties.getAccessExpTime());
    }

    @Override
    public String generateRefreshToken(UserDetails user) {
        return this.generateJwtToken(
            Map.of(
                "type", "refresh",
                "role", this.extractRole(user)
            ), user, this.jwtProperties.getRefreshExpTime());
    }

    private String extractRole(UserDetails user) {
        String role = user.getAuthorities().stream()
            .findFirst()
            .map(GrantedAuthority::getAuthority)
            .orElse(null);
        if (role == null) {
            throw new IllegalStateException("User has no authorities");
        }
        return role;
    }

    private String generateJwtToken(Map<String, Object> extraClaims, UserDetails user, Duration expirationTime) {
        Instant now = Instant.now();
        return Jwts.builder()
            .claims()
            .empty()
            .id(UUID.randomUUID().toString())
            .add(extraClaims)
            .subject(user.getUsername())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(expirationTime)))
            .and()
            .signWith(this.getSigningKey())
            .compact();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = this.jwtProperties.getJwtSigningKey().getBytes(Charset.defaultCharset());
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
