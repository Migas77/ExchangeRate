package com.miguelbf.exchangerateapi.config.security;

import com.miguelbf.exchangerateapi.config.properties.JwtProperties;
import io.jsonwebtoken.security.Keys;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.SecretKey;
import java.nio.charset.Charset;

@Configuration
@AllArgsConstructor
@NullMarked
public class JwtDecoderConfig {

    private final JwtProperties jwtProperties;

    @Bean
    public JwtDecoder jwtDecoder() {
        // To be used in every endpoint that requires authentication, to decode the access token
        byte[] keyBytes = this.jwtProperties.getJwtSigningKey().getBytes(Charset.defaultCharset());
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).build();
        return token -> {
            Jwt jwt = decoder.decode(token);
            if ("refresh".equals(jwt.getClaimAsString("type"))) {
                throw new BadJwtException("Refresh tokens cannot be used to access resources");
            }
            return jwt;
        };
    }

    @Bean
    public JwtDecoder refreshJwtDecoder() {
        // To be used for in the /api/auth/refresh endpoint to get new access token
        byte[] keyBytes = this.jwtProperties.getJwtSigningKey().getBytes(Charset.defaultCharset());
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).build();
        // noinspection Convert2Lambda - Anonymous class intentional for testability (e.g. MockitoSpyBean)
        return new JwtDecoder() {
            @Override
            public Jwt decode(String token) throws JwtException {
                Jwt jwt = decoder.decode(token);
                if (!"refresh".equals(jwt.getClaimAsString("type"))) {
                    throw new BadJwtException("Only refresh tokens can be used to refresh access token");
                }
                return jwt;
            }
        };
    }

}
