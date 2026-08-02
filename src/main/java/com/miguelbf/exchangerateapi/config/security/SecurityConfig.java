package com.miguelbf.exchangerateapi.config.security;

import com.miguelbf.exchangerateapi.config.properties.JwtProperties;
import com.miguelbf.exchangerateapi.service.IUserService;
import io.jsonwebtoken.security.Keys;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import java.nio.charset.Charset;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
@AllArgsConstructor
public class SecurityConfig {

    private final JwtProperties jwtProperties;
    private final IUserService userService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationProvider authenticationProvider) {
        return http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(request -> request
                // ALLOW LIST
                .requestMatchers(
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/v3/api-docs.yaml",
                    "/actuator/health",
                    "/api/auth/**"
                ).permitAll()
                // DENY BY DEFAULT
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())))
            .sessionManagement(manager -> manager.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider)
            .exceptionHandling(handling -> handling
                .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                .accessDeniedHandler(new BearerTokenAccessDeniedHandler()))
            .build();
    }

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
        return token -> {
            Jwt jwt = decoder.decode(token);
            if (!"refresh".equals(jwt.getClaimAsString("type"))) {
                throw new BadJwtException("Only refresh tokens can be used to refresh access token");
            }
            return jwt;
        };
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString("role");
            return role == null ? List.of() : List.of(new SimpleGrantedAuthority("ROLE_" + role));
        });
        return converter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userService.userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) {
        return config.getAuthenticationManager();
    }

}
