package com.miguelbf.exchangerateapi.controller;

import com.miguelbf.exchangerateapi.config.security.AuthConfig;
import com.miguelbf.exchangerateapi.exception.handler.AuthExceptionHandler;
import com.miguelbf.exchangerateapi.exception.handler.GlobalExceptionHandler;
import com.miguelbf.exchangerateapi.exception.handler.UpstreamExceptionHandler;
import com.miguelbf.exchangerateapi.model.dto.*;
import com.miguelbf.exchangerateapi.service.IAuthService;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;
import java.util.stream.Stream;

import static com.miguelbf.exchangerateapi.utilities.CustomMatchers.containsAllStrings;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(AuthConfig.class)
class AuthControllerMockServiceTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IAuthService authService;

    @Test
    void whenExchangeRatesControllerLoaded_thenExceptionHandlersArePresent() {
        // Throws NoSuchBeanDefinitionException if GlobalExceptionHandler not configured
        assertDoesNotThrow(() -> context.getBean(GlobalExceptionHandler.class));
        assertDoesNotThrow(() -> context.getBean(UpstreamExceptionHandler.class));
        assertDoesNotThrow(() -> context.getBean(AuthExceptionHandler.class));
    }

    @ParameterizedTest
    @MethodSource("invalidSignupLoginRequestAndExpectedErrorMessageMatcher")
    void whenInvalidSignupRequest_thenBadRequestAndProblemDetail(
        String body, Matcher<String> expectedErrorMessagesMatcher
    ) throws Exception {
        MockHttpServletRequestBuilder httpRequestBuilder = post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON);
        if (body != null) {
            httpRequestBuilder.content(body);
        }

        mockMvc
            .perform(httpRequestBuilder)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.instance", is("/api/auth/signup")))
            .andExpect(jsonPath("$.title", is("Bad Request")))
            .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
            .andExpect(jsonPath("$.detail", not(emptyOrNullString())))
            .andExpect(jsonPath("$.detail", expectedErrorMessagesMatcher));

        verify(authService, never()).signup(any());
    }


    @ParameterizedTest
    @MethodSource("invalidSignupLoginRequestAndExpectedErrorMessageMatcher")
    void whenInvalidLoginRequest_thenBadRequestAndProblemDetail(
        String body, Matcher<String> expectedErrorMessagesMatcher
    ) throws Exception {
        MockHttpServletRequestBuilder httpRequestBuilder = post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON);
        if (body != null) {
            httpRequestBuilder.content(body);
        }

        mockMvc
            .perform(httpRequestBuilder)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.instance", is("/api/auth/login")))
            .andExpect(jsonPath("$.title", is("Bad Request")))
            .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
            .andExpect(jsonPath("$.detail", not(emptyOrNullString())))
            .andExpect(jsonPath("$.detail", expectedErrorMessagesMatcher));

        verify(authService, never()).login(any());
    }

    @ParameterizedTest
    @MethodSource("invalidRefreshTokenRequestAndExpectedErrorMessage")
    void whenInvalidRefreshTokenRequest_thenBadRequestAndProblemDetail(
        String body, String expectedErrorMessages
    ) throws Exception {
        MockHttpServletRequestBuilder httpRequestBuilder = post("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON);
        if (body != null) {
            httpRequestBuilder.content(body);
        }

        mockMvc
            .perform(httpRequestBuilder)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.instance", is("/api/auth/refresh")))
            .andExpect(jsonPath("$.title", is("Bad Request")))
            .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
            .andExpect(jsonPath("$.detail", not(emptyOrNullString())))
            .andExpect(jsonPath("$.detail", is(expectedErrorMessages)));

        verify(authService, never()).refresh(any());
    }

    @Test
    void givenNonExistingUserWithSameEmail_whenValidSignupRequest_thenStatusOkAndAuthResponse() throws Exception {
        SignUpRequestDTO signUpRequestDTO = new SignUpRequestDTO("user@example.com", "password");
        AuthResponseDTO authResponseDTO = new AuthResponseDTO(
            UUID.randomUUID(), signUpRequestDTO.email(), "accessToken", "refreshToken", 899);
        when(authService.signup(signUpRequestDTO)).thenReturn(authResponseDTO);

        mockMvc
            .perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpRequestDTO)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(authResponseDTO.id().toString())))
            .andExpect(jsonPath("$.email", is(authResponseDTO.email())))
            .andExpect(jsonPath("$.accessToken", is(authResponseDTO.accessToken())))
            .andExpect(jsonPath("$.refreshToken", is(authResponseDTO.refreshToken())))
            .andExpect(jsonPath("$.expiresIn").value(authResponseDTO.expiresIn()));

        verify(authService, times(1)).signup(signUpRequestDTO);
    }

    @Test
    void givenExistingUserWithSameEmail_whenValidSignupRequest_thenStatusUnauthorizedWithEmptyBody() throws Exception {
        SignUpRequestDTO signUpRequestDTO = new SignUpRequestDTO("user@example.com", "password");
        when(authService.signup(signUpRequestDTO)).thenReturn(null);

        mockMvc
            .perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpRequestDTO)))
            .andExpect(status().isUnauthorized())
            .andExpect(content().string(""))
            .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));

        verify(authService, times(1)).signup(signUpRequestDTO);
    }

    @Test
    void givenNonExistingUserWithSuppliedEmail_whenValidLoginRequest_thenStatusUnauthorizedWithEmptyBody() throws Exception {
        LoginRequestDTO loginRequestDTO = new LoginRequestDTO("user@example.com", "password");
        when(authService.login(loginRequestDTO)).thenReturn(null);

        mockMvc
            .perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequestDTO)))
            .andExpect(status().isUnauthorized())
            .andExpect(content().string(""))
            .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));

        verify(authService, times(1)).login(loginRequestDTO);
    }

    @Test
    void givenExistingUserWithSuppliedEmail_whenValidLoginRequestWithWrongPassword_thenUnauthorizedWithBadCredentials() throws Exception {
        LoginRequestDTO loginRequestDTO = new LoginRequestDTO("user@example.com", "wrong-password");
        when(authService.login(loginRequestDTO)).thenThrow(new BadCredentialsException("wrong password"));

        mockMvc
            .perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequestDTO)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.instance", is("/api/auth/login")))
            .andExpect(jsonPath("$.title", is("Unauthorized")))
            .andExpect(jsonPath("$.status", is(HttpStatus.UNAUTHORIZED.value())))
            .andExpect(jsonPath("$.detail", not(emptyOrNullString())))
            .andExpect(jsonPath("$.detail", is("Invalid email or password.")));

        verify(authService, times(1)).login(loginRequestDTO);
    }

    @Test
    void givenExistingUserWithSuppliedEmail_whenValidLoginCredentials_thenStatusOkAndAuthResponse() throws Exception {
        LoginRequestDTO loginRequestDTO = new LoginRequestDTO("user@example.com", "password");
        AuthResponseDTO authResponseDTO = new AuthResponseDTO(
            UUID.randomUUID(), loginRequestDTO.email(), "accessToken", "refreshToken", 899);
        when(authService.login(loginRequestDTO)).thenReturn(authResponseDTO);

        mockMvc
            .perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequestDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(authResponseDTO.id().toString())))
            .andExpect(jsonPath("$.email", is(authResponseDTO.email())))
            .andExpect(jsonPath("$.accessToken", is(authResponseDTO.accessToken())))
            .andExpect(jsonPath("$.refreshToken", is(authResponseDTO.refreshToken())))
            .andExpect(jsonPath("$.expiresIn").value(authResponseDTO.expiresIn()));

        verify(authService, times(1)).login(loginRequestDTO);
    }

    @Test
    void whenTamperedJwtRefreshToken_thenStatusUnauthorizedAndWWWAuthenticateHeadersAndEmptyBody() throws Exception {
        JwtRefreshRequestDTO jwtRefreshRequestDTO = new JwtRefreshRequestDTO("tampered.tampered.tampered");
        InvalidBearerTokenException ex = new InvalidBearerTokenException("invalid bearer");
        when(authService.refresh(jwtRefreshRequestDTO)).thenThrow(ex);

        mockMvc
            .perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(jwtRefreshRequestDTO)))
            .andExpect(status().isUnauthorized())
            .andExpect(content().string(""))
            .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\", " +
                "error_description=\"%s\", ".formatted(ex.getMessage()) +
                "error_uri=\"https://tools.ietf.org/html/rfc6750#section-3.1\", " +
                "resource_metadata=\"http://localhost/.well-known/oauth-protected-resource\""));

        verify(authService, times(1)).refresh(jwtRefreshRequestDTO);
    }

    @Test
    void whenUnexpectedJwtRefreshTokenException_thenStatusUnauthorizedAndGenericWWWAuthenticateHeadersAndEmptyBody() throws Exception {
        JwtRefreshRequestDTO jwtRefreshRequestDTO = new JwtRefreshRequestDTO("unexpected.unexpected.unexpected");
        AuthenticationServiceException ex = new AuthenticationServiceException("another exception");
        when(authService.refresh(jwtRefreshRequestDTO)).thenThrow(ex);

        mockMvc
            .perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(jwtRefreshRequestDTO)))
            .andExpect(status().isUnauthorized())
            .andExpect(content().string(""))
            .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer " +
                "resource_metadata=\"http://localhost/.well-known/oauth-protected-resource\""));

        verify(authService, times(1)).refresh(jwtRefreshRequestDTO);
    }

    @Test
    void whenJwtRefreshTokenPassesValidationWithNoSubject_thenStatusUnauthorizedAndEmptyBody() throws Exception {
        JwtRefreshRequestDTO jwtRefreshRequestDTO = new JwtRefreshRequestDTO("nosubj.nosubj.nosubj");
        when(authService.refresh(jwtRefreshRequestDTO)).thenReturn(null);

        mockMvc
            .perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(jwtRefreshRequestDTO)))
            .andExpect(status().isUnauthorized())
            .andExpect(content().string(""))
            .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));

        verify(authService, times(1)).refresh(jwtRefreshRequestDTO);
    }

    @Test
    void whenValidJwtRefreshTokenRequest_thenStatusOkAndNewAccessToken() throws Exception {
        JwtRefreshRequestDTO jwtRefreshRequestDTO = new JwtRefreshRequestDTO("valid.valid.valid");
        JwtRefreshResponseDTO refreshResponseDTO = new JwtRefreshResponseDTO("new.new.new", 899);
        when(authService.refresh(jwtRefreshRequestDTO)).thenReturn(refreshResponseDTO);

        mockMvc
            .perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(jwtRefreshRequestDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken", is(refreshResponseDTO.accessToken())))
            .andExpect(jsonPath("$.expiresIn").value(refreshResponseDTO.expiresIn()));

        verify(authService, times(1)).refresh(jwtRefreshRequestDTO);
    }

    private static Stream<Arguments> invalidSignupLoginRequestAndExpectedErrorMessageMatcher() {
        return Stream.of(
            Arguments.of(null, containsAllStrings("Request body is missing or empty.")),
            Arguments.of("{}", containsAllStrings("email: must not be blank", "password: must not be blank")),
            Arguments.of("{\"email\": \"invalid\", \"password\": \"inv\"}", containsAllStrings("email: must be a well-formed email address", "password: Password must be between 6 and 128 characters long")),
            Arguments.of("{\"email\": }", containsAllStrings("Malformed JSON in request body.")),
            Arguments.of("{\"email\": {\"nested\": \"object\"}}", containsAllStrings("Missing or invalid value for field 'email'.")),
            Arguments.of("[]", containsAllStrings("Missing or invalid value for field 'unknown'."))
        );
    }

    private static Stream<Arguments> invalidRefreshTokenRequestAndExpectedErrorMessage() {
        return Stream.of(
            Arguments.of(null, "Request body is missing or empty."),
            Arguments.of("{}", "refreshToken: must not be blank"),
            Arguments.of("{\"refreshToken\": \"invalid\"}", "refreshToken: Malformed refresh token"),
            Arguments.of("{\"refreshToken\": }", "Malformed JSON in request body."),
            Arguments.of("{\"refreshToken\": {\"nested\": \"object\"}}", "Missing or invalid value for field 'refreshToken'."),
            Arguments.of("[]", "Missing or invalid value for field 'unknown'.")
        );
    }


}
