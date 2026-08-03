package com.miguelbf.exchangerateapi.service;

import com.miguelbf.exchangerateapi.config.properties.JwtProperties;
import com.miguelbf.exchangerateapi.config.security.SecurityConfig;
import com.miguelbf.exchangerateapi.entities.User;
import com.miguelbf.exchangerateapi.entities.UserRole;
import com.miguelbf.exchangerateapi.model.dto.*;
import com.miguelbf.exchangerateapi.service.impl.AuthService;
import com.miguelbf.exchangerateapi.service.impl.JwtService;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(JwtProperties.class)
@ContextConfiguration(classes = JwtProperties.class)
@TestPropertySource(locations = "classpath:application.properties")
class AuthServiceMockServicesTest {

    @Autowired
    JwtProperties jwtProperties;

    @Spy
    PasswordEncoder passwordEncoder = new SecurityConfig(null, null).passwordEncoder();

    @Spy
    JwtDecoder refreshJwtDecoder;

    @InjectMocks
    AuthService authService;

    @Mock
    IUserService userService;

    @Mock
    IJwtService jwtService;

    @Mock
    AuthenticationManager authenticationManager;

    @Mock
    Authentication authentication;

    @BeforeEach
    void setUp() {
        this.refreshJwtDecoder = new SecurityConfig(this.jwtProperties, null).refreshJwtDecoder();
        ReflectionTestUtils.setField(authService, "refreshJwtDecoder", this.refreshJwtDecoder);
    }

    @Test
    void givenExistingUserWithEmail_whenSignup_thenReturnNull() {
        SignUpRequestDTO signUpRequestDTO = new SignUpRequestDTO("user@example.com", "password");
        User user = mock(User.class);
        when(userService.getUserByEmail(signUpRequestDTO.email())).thenReturn(user);

        AuthResponseDTO authResponseDTO = authService.signup(signUpRequestDTO);

        assertNull(authResponseDTO);
        verify(userService, times(1)).getUserByEmail(signUpRequestDTO.email());
        verify(userService, never()).createUser(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
        verify(jwtService, never()).generateToken(any(User.class));
        verify(jwtService, never()).generateRefreshToken(any(User.class));
        verify(jwtService, never()).extractExpiration(anyString());
    }

    @Test
    void givenNoExistingUserWithEmail_whenSignup_thenCreateUserGenerateTokensAndReturnAuthResponse() {
        SignUpRequestDTO signUpRequestDTO = new SignUpRequestDTO("user@example.com", "password");
        String accessToken = "access.access.access";
        String refreshToken = "refresh.refresh.refresh";
        long expiresIn = 300;
        when(userService.getUserByEmail(signUpRequestDTO.email())).thenReturn(null);
        when(userService.createUser(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn(accessToken);
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn(refreshToken);
        when(jwtService.extractExpiration(accessToken)).thenReturn(Instant.now().plus(expiresIn, ChronoUnit.SECONDS));

        AuthResponseDTO authResponseDTO = authService.signup(signUpRequestDTO);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        assertNotNull(authResponseDTO);
        assertEquals(signUpRequestDTO.email(), authResponseDTO.email());
        assertEquals(accessToken, authResponseDTO.accessToken());
        assertEquals(refreshToken, authResponseDTO.refreshToken());
        assertTrue(authResponseDTO.expiresIn() > expiresIn - 10);
        verify(userService, times(1)).getUserByEmail(signUpRequestDTO.email());
        verify(userService, times(1)).createUser(userCaptor.capture());
        verify(passwordEncoder, times(1)).encode(signUpRequestDTO.password());
        User createdUser = userCaptor.getValue();
        assertEquals(signUpRequestDTO.email(), createdUser.getUsername());
        assertNotEquals(signUpRequestDTO.password(), createdUser.getPassword());
        assertTrue(passwordEncoder.matches(signUpRequestDTO.password(), createdUser.getPassword()));
        assertEquals(UserRole.FREE_TIER.name(), createdUser.getAuthorities().iterator().next().getAuthority());
        verify(jwtService, times(1)).generateToken(createdUser);
        verify(jwtService, times(1)).generateRefreshToken(createdUser);
        verify(jwtService, times(1)).extractExpiration(accessToken);
    }

    @Test
    void givenInvalidCredentials_whenLogin_thenThrowAuthenticationException() {
        LoginRequestDTO loginRequestDTO = new LoginRequestDTO("user@example.com", "wrong-password");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequestDTO));

        ArgumentCaptor<UsernamePasswordAuthenticationToken> authCaptor = ArgumentCaptor
            .forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager, times(1)).authenticate(authCaptor.capture());
        UsernamePasswordAuthenticationToken capturedAuth = authCaptor.getValue();
        assertEquals(loginRequestDTO.email(), capturedAuth.getPrincipal());
        assertEquals(loginRequestDTO.password(), capturedAuth.getCredentials());
        verify(authentication, never()).getPrincipal();
        verify(jwtService, never()).generateToken(any(User.class));
        verify(jwtService, never()).generateRefreshToken(any(User.class));
        verify(jwtService, never()).extractExpiration(anyString());
    }

    @Test
    void givenValidCredentialsUnexpectedSubjected_whenLogin_thenReturnNull() {
        LoginRequestDTO loginRequestDTO = new LoginRequestDTO("user@example.com", "password");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(null);

        AuthResponseDTO authResponseDTO = authService.login(loginRequestDTO);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> authCaptor = ArgumentCaptor
            .forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager, times(1)).authenticate(authCaptor.capture());
        UsernamePasswordAuthenticationToken capturedAuth = authCaptor.getValue();
        assertEquals(loginRequestDTO.email(), capturedAuth.getPrincipal());
        assertEquals(loginRequestDTO.password(), capturedAuth.getCredentials());
        assertNull(authResponseDTO);
        verify(authentication, times(1)).getPrincipal();
        verify(jwtService, never()).generateToken(any(User.class));
        verify(jwtService, never()).generateRefreshToken(any(User.class));
        verify(jwtService, never()).extractExpiration(anyString());
    }

    @Test
    void givenValidCredentials_whenLogin_thenLoginUserGenerateTokensAndReturnAuthResponse() {
        LoginRequestDTO loginRequestDTO = new LoginRequestDTO("user@example.com", "password");
        User newUser = new User("user@example.com", "password", UserRole.FREE_TIER);
        String accessToken = "access.access.access";
        String refreshToken = "refresh.refresh.refresh";
        long expiresIn = 300;

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(newUser);
        when(jwtService.generateToken(any(User.class))).thenReturn(accessToken);
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn(refreshToken);
        when(jwtService.extractExpiration(accessToken)).thenReturn(Instant.now().plus(expiresIn, ChronoUnit.SECONDS));

        AuthResponseDTO authResponseDTO = authService.login(loginRequestDTO);

        assertNotNull(authResponseDTO);
        assertEquals(loginRequestDTO.email(), authResponseDTO.email());
        assertEquals(accessToken, authResponseDTO.accessToken());
        assertEquals(refreshToken, authResponseDTO.refreshToken());
        assertTrue(authResponseDTO.expiresIn() > expiresIn - 10);
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(authentication, times(1)).getPrincipal();
        verify(jwtService, times(1)).generateToken(newUser);
        verify(jwtService, times(1)).generateRefreshToken(newUser);
        verify(jwtService, times(1)).extractExpiration(accessToken);
    }

    @Test
    void givenInvalidJwt_whenRefreshToken_thenThrowsInvalidBearerTokenException() {
        JwtRefreshRequestDTO jwtRefreshRequestDTO = new JwtRefreshRequestDTO("invalid.invalid.invalid");

        InvalidBearerTokenException ex = assertThrows(
            InvalidBearerTokenException.class,
            () -> authService.refresh(jwtRefreshRequestDTO)
        );

        assertEquals("An error occurred while attempting to decode the Jwt: Malformed token", ex.getMessage());
        assertInstanceOf(BadJwtException.class, ex.getCause());
    }

    @Test
    @SuppressWarnings("NullAway")
    void givenInvalidJwt_whenRefreshToken_ThenThrowsInvalidBearerTokenExceptionWithNullMessage() {
        JwtRefreshRequestDTO jwtRefreshRequestDTO = new JwtRefreshRequestDTO("invalid.invalid.invalid");
        JwtDecoder delegatingDecoder = mock(JwtDecoder.class, AdditionalAnswers.delegatesTo(this.refreshJwtDecoder));
        BadJwtException thrownException = new BadJwtException(null);
        doThrow(thrownException).when(delegatingDecoder).decode(jwtRefreshRequestDTO.refreshToken());
        ReflectionTestUtils.setField(authService, "refreshJwtDecoder", delegatingDecoder);

        InvalidBearerTokenException ex = assertThrows(
            InvalidBearerTokenException.class,
            () -> authService.refresh(jwtRefreshRequestDTO)
        );

        assertEquals("Invalid token", ex.getMessage());
        assertEquals(thrownException, ex.getCause());
        verify(delegatingDecoder, times(1)).decode(jwtRefreshRequestDTO.refreshToken());
        verify(userService, never()).getUserByEmail(anyString());
        verify(jwtService, never()).generateToken(any(User.class));
        verify(jwtService, never()).extractExpiration(anyString());
    }


    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Unexpected error"})
    @SuppressWarnings("NullAway")
    void givenInvalidJwt_whenRefreshTokenAndUnexpectedError_thenThrowsJwtException(@Nullable String errorMessage) {
        JwtRefreshRequestDTO jwtRefreshRequestDTO = new JwtRefreshRequestDTO("unexpected.unexpected.unexpected");
        JwtDecoder delegatingDecoder = mock(JwtDecoder.class, AdditionalAnswers.delegatesTo(this.refreshJwtDecoder));
        JwtException thrownException = new JwtException(errorMessage);
        doThrow(thrownException).when(delegatingDecoder).decode(jwtRefreshRequestDTO.refreshToken());
        ReflectionTestUtils.setField(authService, "refreshJwtDecoder", delegatingDecoder);

        AuthenticationServiceException ex = assertThrows(
            AuthenticationServiceException.class,
            () -> authService.refresh(jwtRefreshRequestDTO)
        );

        assertEquals(errorMessage != null ? errorMessage : "Invalid token", ex.getMessage());
        assertEquals(thrownException, ex.getCause());
        verify(delegatingDecoder, times(1)).decode(jwtRefreshRequestDTO.refreshToken());
        verify(userService, never()).getUserByEmail(anyString());
        verify(jwtService, never()).generateToken(any(User.class));
        verify(jwtService, never()).extractExpiration(anyString());
    }

    @Test
    void givenValidJwtAccessToken_whenRefreshToken_thenThrowsJwtException() {
        JwtService jwtServiceToGenerateToken = new JwtService(this.jwtProperties);
        User user = new User("user@example.com", "password", UserRole.FREE_TIER);
        JwtRefreshRequestDTO jwtRefreshRequestDTO = new JwtRefreshRequestDTO(jwtServiceToGenerateToken.generateToken(user));

        InvalidBearerTokenException ex = assertThrows(
            InvalidBearerTokenException.class,
            () -> authService.refresh(jwtRefreshRequestDTO)
        );

        assertEquals("Only refresh tokens can be used to refresh access token", ex.getMessage());
        assertInstanceOf(BadJwtException.class, ex.getCause());
        verify(userService, never()).getUserByEmail(anyString());
        verify(jwtService, never()).generateToken(any(User.class));
        verify(jwtService, never()).extractExpiration(anyString());
    }

    @Test
    void givenValidJwtRefreshToken_whenRefreshToken_thenGenerateTokensAndReturn() {
        User user = new User("user@example.com", "password", UserRole.FREE_TIER);
        String accessToken = "access.access.access";
        long expiresIn = 300;
        JwtService jwtServiceToGenerateToken = new JwtService(this.jwtProperties);
        String generatedToken = jwtServiceToGenerateToken.generateRefreshToken(user);
        JwtRefreshRequestDTO jwtRefreshRequestDTO = new JwtRefreshRequestDTO(generatedToken);
        when(userService.getUserByEmail(user.getUsername())).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn(accessToken);
        when(jwtService.extractExpiration(accessToken)).thenReturn(Instant.now().plus(expiresIn, ChronoUnit.SECONDS));

        JwtRefreshResponseDTO jwtRefreshResponseDTO = authService.refresh(jwtRefreshRequestDTO);

        assertNotNull(jwtRefreshResponseDTO);
        assertEquals(accessToken, jwtRefreshResponseDTO.accessToken());
        assertTrue(jwtRefreshResponseDTO.expiresIn() > expiresIn - 10);
        verify(userService, times(1)).getUserByEmail(user.getUsername());
        verify(jwtService, times(1)).generateToken(user);
        verify(jwtService, times(1)).extractExpiration(accessToken);
    }

    @Test
    @SuppressWarnings("NullAway")
    void givenValidJwtRefreshTokenWithNullSubject_whenRefreshToken_thenReturnNull() {
        User user = new User(null, "password", UserRole.FREE_TIER);
        JwtService jwtServiceToGenerateToken = new JwtService(this.jwtProperties);
        String generatedToken = jwtServiceToGenerateToken.generateRefreshToken(user);
        JwtRefreshRequestDTO jwtRefreshRequestDTO = new JwtRefreshRequestDTO(generatedToken);

        JwtRefreshResponseDTO jwtRefreshResponseDTO = authService.refresh(jwtRefreshRequestDTO);

        assertNull(jwtRefreshResponseDTO);
        verify(userService, never()).getUserByEmail(anyString());
        verify(jwtService, never()).generateToken(any(User.class));
        verify(jwtService, never()).extractExpiration(anyString());
    }

    @Test
    void givenValidJwtRefreshTokenWithoutCorrespondingUser_whenRefreshToken_thenReturnNull() {
        User user = new User("user@example.com", "password", UserRole.FREE_TIER);
        JwtService jwtServiceToGenerateToken = new JwtService(this.jwtProperties);
        String generatedToken = jwtServiceToGenerateToken.generateRefreshToken(user);
        JwtRefreshRequestDTO jwtRefreshRequestDTO = new JwtRefreshRequestDTO(generatedToken);
        when(userService.getUserByEmail(user.getUsername())).thenReturn(null);

        JwtRefreshResponseDTO jwtRefreshResponseDTO = authService.refresh(jwtRefreshRequestDTO);

        assertNull(jwtRefreshResponseDTO);
        verify(userService, times(1)).getUserByEmail(user.getUsername());
        verify(jwtService, never()).generateToken(any(User.class));
        verify(jwtService, never()).extractExpiration(anyString());
    }

}
