package com.miguelbf.exchangerateapi.service;

import com.miguelbf.exchangerateapi.config.properties.JwtProperties;
import com.miguelbf.exchangerateapi.entities.User;
import com.miguelbf.exchangerateapi.entities.UserRole;
import com.miguelbf.exchangerateapi.service.impl.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(JwtProperties.class)
@ContextConfiguration(classes = {JwtService.class})
@TestPropertySource(locations = "classpath:application.properties")
class JwtServiceTest {

    @Autowired
    IJwtService jwtService;

    @Test
    void whenValidUser_thenCorrectlyGenerateAccessToken() {
        User user = new User("user@example.com", "password", UserRole.FREE_TIER);

        String accessToken = jwtService.generateToken(user);

        Claims claims = ReflectionTestUtils.invokeMethod(jwtService, "extractAllClaims", accessToken);
        assertNotNull(claims);
        assertNull(claims.get("type", String.class));
        assertNotNull(claims.getId());
        assertEquals(claims.getId(), claims.get("jti", String.class));
        assertEquals(user.getUsername(), claims.getSubject());
        assertEquals(user.getUsername(), claims.get("sub", String.class));
        assertEquals(user.getAuthorities().iterator().next().getAuthority(), claims.get("role", String.class));
        assertNotNull(claims.getIssuedAt());
        assertEquals(claims.getIssuedAt(), claims.get("iat", Date.class));
        assertNotNull(claims.getExpiration());
        assertEquals(claims.getExpiration(), claims.get("exp", Date.class));
        assertTrue(claims.getExpiration().toInstant().isAfter(claims.getIssuedAt().toInstant()));
    }

    @Test
    void whenValidUser_thenCorrectlyGenerateRefreshToken() {
        User user = new User("user@example.com", "password", UserRole.FREE_TIER);

        String refreshToken = jwtService.generateRefreshToken(user);

        Claims claims = ReflectionTestUtils.invokeMethod(jwtService, "extractAllClaims", refreshToken);
        assertNotNull(claims);
        assertEquals("refresh", claims.get("type", String.class));
        assertNotNull(claims.getId());
        assertEquals(claims.getId(), claims.get("jti", String.class));
        assertEquals(user.getUsername(), claims.getSubject());
        assertEquals(user.getUsername(), claims.get("sub", String.class));
        assertEquals(user.getAuthorities().iterator().next().getAuthority(), claims.get("role", String.class));
        assertNotNull(claims.getIssuedAt());
        assertEquals(claims.getIssuedAt(), claims.get("iat", Date.class));
        assertNotNull(claims.getExpiration());
        assertEquals(claims.getExpiration(), claims.get("exp", Date.class));
        assertTrue(claims.getExpiration().toInstant().isAfter(claims.getIssuedAt().toInstant()));
    }

    @Test
    void givenInvalidUserWithNullRole_whenGenerateAccessToken_ThrowsIllegalStateException() {
        User user = mock(User.class);
        when(user.getAuthorities()).thenReturn(List.of());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> jwtService.generateToken(user));

        assertNotNull(ex);
        assertEquals("User has no authorities", ex.getMessage());
    }

    @Test
    void whenValidToken_thenExtractExpirationCorrectly() {
        User user = new User("user@example.com", "password", UserRole.FREE_TIER);

        String accessToken = jwtService.generateToken(user);

        Claims claims = ReflectionTestUtils.invokeMethod(jwtService, "extractAllClaims", accessToken);
        Instant expiration = jwtService.extractExpiration(accessToken);

        assertNotNull(expiration);
        assertNotNull(claims);
        assertEquals(claims.getExpiration().toInstant(), expiration);
    }
}
