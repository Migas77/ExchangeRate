package com.miguelbf.exchangerateapi.exception;

import com.miguelbf.exchangerateapi.config.properties.JwtProperties;
import com.miguelbf.exchangerateapi.config.security.SecurityConfig;
import com.miguelbf.exchangerateapi.entities.User;
import com.miguelbf.exchangerateapi.entities.UserRole;
import com.miguelbf.exchangerateapi.service.IUserService;
import com.miguelbf.exchangerateapi.service.impl.JwtService;
import com.miguelbf.exchangerateapi.utilities.stubs.StubController;
import com.miguelbf.exchangerateapi.utilities.stubs.StubService;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.stream.Stream;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StubController.class)
@Import(SecurityConfig.class)
class AuthExceptionHandlerTest {
    /**
     * To test the default exception handling from the oauth2ResourceServer set up in
     * {@link com.miguelbf.exchangerateapi.config.security.SecurityConfig#securityFilterChain}, which is
     * provided by the spring-boot-starter-oauth2-resource-server dependency.
     * <p>
     * According to the given RFC
     * <a href="https://datatracker.ietf.org/doc/html/rfc6750#section-3">RFC 6750 Section 3 WWW-Authenticate response headers</a>
     */

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StubService stubService;

    @MockitoBean(answers = Answers.RETURNS_MOCKS)
    private IUserService userService;

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "invalid", "Basic abc123"})
    void whenMissingJwtToken_thenStatusUnauthorizedAndWWWAuthenticateHeadersAndEmptyBody(@Nullable String requestToken) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get("/stub");
        if (requestToken != null) {
            requestBuilder.header(HttpHeaders.AUTHORIZATION, requestToken);
        }
        mockMvc
            .perform(requestBuilder)
            .andExpect(status().isUnauthorized())
            .andExpect(content().string(""))
            .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer " +
                "resource_metadata=\"http://localhost/.well-known/oauth-protected-resource\""));

        verify(stubService, never()).call();
    }

    @Test
    void whenMalformedJwtToken_thenStatusUnauthorizedAndWWWAuthenticateHeadersAndEmptyBody() throws Exception {
        String authHeader = "Bearer invalid";
        mockMvc
            .perform(get("/stub")
                .header(HttpHeaders.AUTHORIZATION, authHeader))
            .andExpect(status().isUnauthorized())
            .andExpect(content().string(""))
            .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\", " +
                "error_description=\"An error occurred while attempting to decode the Jwt: Malformed token\", " +
                "error_uri=\"https://tools.ietf.org/html/rfc6750#section-3.1\", " +
                "resource_metadata=\"http://localhost/.well-known/oauth-protected-resource\""));

        verify(stubService, never()).call();
    }

    @ParameterizedTest
    @MethodSource("invalidJwtAndErrorScenarios")
    void whenInvalidHeaderPayloadSignatureJwtToken_thenStatusUnauthorizedAndWWWAuthenticateHeadersAndEmptyBody(
        String invalidSegment, String errorMessage
    ) throws Exception {
        JwtService jwtServiceToGenerateToken = new JwtService(this.jwtProperties);
        String accessToken = jwtServiceToGenerateToken.generateToken(new User("user@example.com", "password", UserRole.FREE_TIER));
        String authHeader = "Bearer " + tamperToken(accessToken, invalidSegment);

        mockMvc
            .perform(get("/stub")
                .header(HttpHeaders.AUTHORIZATION, authHeader))
            .andExpect(status().isUnauthorized())
            .andExpect(content().string(""))
            .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\", " +
                "error_description=\"An error occurred while attempting to decode the Jwt: %s\", ".formatted(errorMessage) +
                "error_uri=\"https://tools.ietf.org/html/rfc6750#section-3.1\", " +
                "resource_metadata=\"http://localhost/.well-known/oauth-protected-resource\""));

        verify(stubService, never()).call();
    }

    @Test
    void whenValidJwtToken_thenStatusOkAndServiceReached() throws Exception {
        JwtService jwtServiceToGenerateToken = new JwtService(this.jwtProperties);
        String accessToken = jwtServiceToGenerateToken.generateToken(new User("user@example.com", "password", UserRole.FREE_TIER));
        String authHeader = "Bearer " + accessToken;

        mockMvc
            .perform(get("/stub")
                .header(HttpHeaders.AUTHORIZATION, authHeader))
            .andExpect(status().isOk());

        verify(stubService, times(1)).call();
    }

    @Test
    void givenFreeTierUser_whenPremiumOnlyEndpoint_thenStatusForbiddenAndWWWAuthenticateHeadersAndEmptyBody() throws Exception {
        JwtService jwtServiceToGenerateToken = new JwtService(this.jwtProperties);
        String accessToken = jwtServiceToGenerateToken.generateToken(new User("user@example.com", "password", UserRole.FREE_TIER));
        String authHeader = "Bearer " + accessToken;

        mockMvc
            .perform(get("/stub/premium")
                .header(HttpHeaders.AUTHORIZATION, authHeader))
            .andExpect(status().isForbidden())
            .andExpect(content().string(""))
            .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"insufficient_scope\", " +
                "error_description=\"The request requires higher privileges than provided by the access token.\", " +
                "error_uri=\"https://tools.ietf.org/html/rfc6750#section-3.1\""));

        verify(stubService, never()).premiumCall();
    }

    @Test
    void givenPremiumTierUser_whenPremiumOnlyEndpoint_thenStatusOkAndServiceReached() throws Exception {
        JwtService jwtServiceToGenerateToken = new JwtService(this.jwtProperties);
        String accessToken = jwtServiceToGenerateToken.generateToken(new User("user@example.com", "password", UserRole.PREMIUM_TIER));
        String authHeader = "Bearer " + accessToken;

        mockMvc
            .perform(get("/stub/premium")
                .header(HttpHeaders.AUTHORIZATION, authHeader))
            .andExpect(status().isOk());

        verify(stubService, times(1)).premiumCall();
    }

    private static Stream<Arguments> invalidJwtAndErrorScenarios() {
        return Stream.of(
            Arguments.of("header", "Malformed token"),
            Arguments.of("payload", "Malformed payload"),
            Arguments.of("signature", "Signed JWT rejected: Invalid signature")
        );
    }

    private static String tamperToken(String jwt, String segment) {
        int tamperBeforeIndex = switch (segment) {
            case "header" -> jwt.indexOf('.');
            case "payload" -> jwt.lastIndexOf('.');
            case "signature" -> jwt.length();
            default -> throw new IllegalArgumentException("Segment must be one of header|payload|signature");
        };

        char[] chars = jwt.toCharArray();
        chars[tamperBeforeIndex - 1] = (chars[tamperBeforeIndex - 1] == 'A') ? 'T' : 'A';
        chars[tamperBeforeIndex - 2] = (chars[tamperBeforeIndex - 2] == 'A') ? 'P' : 'A';

        return new String(chars);
    }
}
