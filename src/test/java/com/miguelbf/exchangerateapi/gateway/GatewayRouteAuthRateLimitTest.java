package com.miguelbf.exchangerateapi.gateway;

import com.miguelbf.exchangerateapi.config.properties.JwtProperties;
import com.miguelbf.exchangerateapi.config.properties.RateLimitProperties;
import com.miguelbf.exchangerateapi.entities.User;
import com.miguelbf.exchangerateapi.entities.UserRole;
import com.miguelbf.exchangerateapi.repository.UserRepository;
import com.miguelbf.exchangerateapi.service.IExchangeRatesService;
import com.miguelbf.exchangerateapi.service.IJwtService;
import io.github.bucket4j.Bucket;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {"management.health.redis.enabled=false"}
)
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    FlywayAutoConfiguration.class
})
@ActiveProfiles("no-redis")
@AutoConfigureMockMvc
class GatewayRouteAuthRateLimitTest {

    private static final String USER_KEY_PREFIX = "b4j::user:";
    private static final String IP_KEY_PREFIX = "b4j::ip:";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HandlerFilterFunction<ServerResponse, ServerResponse> rateLimitFilter;

    @Autowired
    private IJwtService jwtService;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private RateLimitProperties rateLimitProperties;

    @MockitoBean
    private IExchangeRatesService exchangeRatesService;

    @MockitoBean
    private UserRepository userRepository;

    @ParameterizedTest
    @CsvSource({
        "/gw/swagger-ui/index.html, /swagger-ui/index.html",
        "/gw/swagger-ui/oauth2-redirect.html, /swagger-ui/oauth2-redirect.html",
        "/gw/v3/api-docs/swagger-config, /v3/api-docs/swagger-config",
        "/gw/v3/api-docs, /v3/api-docs",
        "/gw/v3/api-docs.yaml, /v3/api-docs.yaml"
    })
    void whenUnauthenticatedRequestOnOpenGatewayPath_thenForwardToStrippedPath(String request, String forwardedTo) throws Exception {
        mockMvc
            .perform(get(request))
            .andExpect(forwardedUrl(forwardedTo));
    }

    @ParameterizedTest
    @CsvSource({
        "/gw/api/rates, /api/rates",
        "/gw/api/rates/, /api/rates/",
        "/gw/notfound, /notfound"
    })
    void whenAuthenticatedRequestOnProtectedGatewayPath_thenForwardedToStrippedPath(String request, String forwardedTo) throws Exception {
        mockMvc
            .perform(get(request)
                .header(HttpHeaders.AUTHORIZATION, bearer(freshUserToken())))
            .andExpect(forwardedUrl(forwardedTo));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/gw/api/rates", "/gw/api/rates/", "/gw/notfound"})
    void whenUnauthenticatedRequestOnProtectedGatewayPath_thenUnauthorizedAndWWWAuthenticateHeadersAndEmptyBody(
        String request
    ) throws Exception {
        mockMvc
            .perform(get(request))
            .andExpect(forwardedUrl(null))
            .andExpect(status().isUnauthorized())
            .andExpect(content().string(""))
            .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer " +
                "resource_metadata=\"http://localhost/.well-known/oauth-protected-resource\""));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "invalid", "Basic abc123"})
    void whenMissingJwtToken_thenStatusUnauthorizedAndWWWAuthenticateHeadersAndEmptyBody(
        @Nullable String requestToken
    ) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get("/gw/api/rates");
        if (requestToken != null) {
            requestBuilder.header(HttpHeaders.AUTHORIZATION, requestToken);
        }

        mockMvc
            .perform(requestBuilder)
            .andExpect(forwardedUrl(null))
            .andExpect(status().isUnauthorized())
            .andExpect(content().string(""))
            .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer " +
                "resource_metadata=\"http://localhost/.well-known/oauth-protected-resource\""));
    }

    @Test
    void whenMalformedJwtToken_thenStatusUnauthorizedAndWWWAuthenticateHeadersAndEmptyBody() throws Exception {
        String authHeader = "Bearer invalid";

        mockMvc
            .perform(get("/gw/api/rates")
                .header(HttpHeaders.AUTHORIZATION, authHeader))
            .andExpect(forwardedUrl(null))
            .andExpect(status().isUnauthorized())
            .andExpect(content().string(""))
            .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\", " +
                "error_description=\"An error occurred while attempting to decode the Jwt: Malformed token\", " +
                "error_uri=\"https://tools.ietf.org/html/rfc6750#section-3.1\", " +
                "resource_metadata=\"http://localhost/.well-known/oauth-protected-resource\""));
    }

    @ParameterizedTest
    @CsvSource({
        "header, Malformed token",
        "payload, Malformed payload",
        "signature, Signed JWT rejected: Invalid signature"
    })
    void whenInvalidHeaderPayloadSignatureJwtToken_thenStatusUnauthorizedAndWWWAuthenticateHeadersAndEmptyBody(
        String invalidSegment, String errorMessage
    ) throws Exception {
        String authHeader = bearer(tamperToken(freshUserToken(), invalidSegment));

        mockMvc
            .perform(get("/gw/api/rates")
                .header(HttpHeaders.AUTHORIZATION, authHeader))
            .andExpect(forwardedUrl(null))
            .andExpect(status().isUnauthorized())
            .andExpect(content().string(""))
            .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\", " +
                "error_description=\"An error occurred while attempting to decode the Jwt: %s\", ".formatted(errorMessage) +
                "error_uri=\"https://tools.ietf.org/html/rfc6750#section-3.1\", " +
                "resource_metadata=\"http://localhost/.well-known/oauth-protected-resource\""));
    }

    @Test
    void whenExpiredJwtToken_thenUnauthorizedAndWWWAuthenticateHeadersAndEmptyBody() throws Exception {
        Instant expiredAt = Instant.now().minus(Duration.ofMinutes(5)).truncatedTo(ChronoUnit.SECONDS);

        mockMvc
            .perform(get("/gw/api/rates")
                .header(HttpHeaders.AUTHORIZATION, bearer(expiredUserToken(expiredAt))))
            .andExpect(forwardedUrl(null))
            .andExpect(status().isUnauthorized())
            .andExpect(content().string(""))
            .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\", " +
                "error_description=\"An error occurred while attempting to decode the Jwt: Jwt expired at %s\", ".formatted(expiredAt) +
                "error_uri=\"https://tools.ietf.org/html/rfc6750#section-3.1\", " +
                "resource_metadata=\"http://localhost/.well-known/oauth-protected-resource\""));
    }

    @Test
    void whenRequestWithRefreshJwtToken_thenUnauthorizedAndNotForwarded() throws Exception {
        String refreshToken = jwtService.generateRefreshToken(
            new User(UUID.randomUUID() + "@test.com", "irrelevant", UserRole.FREE_TIER));

        mockMvc.perform(get("/gw/api/rates")
                .header(HttpHeaders.AUTHORIZATION, bearer(refreshToken)))
            .andExpect(forwardedUrl(null))
            .andExpect(status().isUnauthorized())
            .andExpect(content().string(""))
            .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\", " +
                "error_description=\"Refresh tokens cannot be used to access resources\", " +
                "error_uri=\"https://tools.ietf.org/html/rfc6750#section-3.1\", " +
                "resource_metadata=\"http://localhost/.well-known/oauth-protected-resource\""));
    }

    @ParameterizedTest
    @CsvSource({"/api/rates", "/api/auth/login", "/v3/api-docs", "/swagger-ui/index.html", "/notfound"})
    void whenAuthenticatedAndControllerHitDirectlyBypassingGateway_thenForbidden(String requested) throws Exception {
        mockMvc
            .perform(get(requested))
            .andExpect(forwardedUrl(null))
            .andExpect(status().isForbidden())
            .andExpect(content().string(""))
            .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
    }

    @ParameterizedTest
    @CsvSource({"/api/rates", "/api/auth/login", "/v3/api-docs", "/swagger-ui/index.html", "/notfound"})
    void whenNotAuthenticatedAndControllerHitDirectlyBypassingGateway_thenForbidden(String requested) throws Exception {
        mockMvc
            .perform(get(requested)
                .header(HttpHeaders.AUTHORIZATION, bearer(freshUserToken())))
            .andExpect(forwardedUrl(null))
            .andExpect(status().isForbidden())
            .andExpect(content().string(""))
            .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void givenUnlimitedRateRoute_whenCalledRepeatedlyForAuthAndAnonymousUser_thenNeverRateLimitedAndNoBucketCreated(
        boolean isAuthenticated
    ) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get("/gw/v3/api-docs");
        String bucketKey;
        if (isAuthenticated) {
            User exhaustedUser = getNewUser();
            bucketKey = USER_KEY_PREFIX + exhaustedUser.getUsername();
            requestBuilder.header(HttpHeaders.AUTHORIZATION, bearer(jwtService.generateToken(exhaustedUser)));
        } else {
            String ip = "172.16.0.1";
            bucketKey = IP_KEY_PREFIX + ip;
            requestBuilder.with(withRemoteAddr(ip));
        }

        for (int i = 0; i < rateLimitProperties.getCapacity() + 10; i++) {
            mockMvc
                .perform(requestBuilder)
                .andExpect(forwardedUrl("/v3/api-docs"))
                .andExpect(status().isOk());
        }

        assertFalse(getRateBuckets().containsKey(bucketKey));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void givenRateLimitedRoute_whenBucketCapacityExceededForAuthAndAnonymousUser_thenTooManyRequestsAndNotForwarded(
        boolean isAuthenticated
    ) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get("/gw/api/auth/login");
        String bucketKey;
        if (isAuthenticated) {
            User newUser = getNewUser();
            bucketKey = USER_KEY_PREFIX + newUser.getUsername();
            requestBuilder.header(HttpHeaders.AUTHORIZATION, bearer(jwtService.generateToken(newUser)));
        } else {
            String ip = "172.16.0.2";
            bucketKey = IP_KEY_PREFIX + ip;
            requestBuilder.with(withRemoteAddr(ip));
        }
        long capacity = rateLimitProperties.getCapacity();

        assertFalse(getRateBuckets().containsKey(bucketKey));
        for (int i = 0; i < capacity; i++) {
            mockMvc
                .perform(requestBuilder)
                .andExpect(forwardedUrl("/api/auth/login"))
                .andExpect(status().isOk());

            assertEquals(capacity - i - 1, getBucketTokens(bucketKey));
        }

        mockMvc
            .perform(requestBuilder)
            .andExpect(forwardedUrl(null))
            .andExpect(status().isTooManyRequests())
            .andExpect(content().string(""));
        assertEquals(0, getBucketTokens(bucketKey));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Bucket> getRateBuckets() throws IllegalAccessException {
        for (Field field : rateLimitFilter.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            if (field.get(rateLimitFilter) instanceof Map<?, ?> capturedBuckets) {
                return (Map<String, Bucket>) capturedBuckets;
            }
        }
        throw new IllegalStateException("The rate limit filter does not hold an in-memory bucket map");
    }

    private long getBucketTokens(String key) throws IllegalAccessException {
        Bucket bucket = getRateBuckets().get(key);
        assertNotNull(bucket);
        return bucket.getAvailableTokens();
    }

    private static RequestPostProcessor withRemoteAddr(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    private User getNewUser() {
        return new User(UUID.randomUUID() + "@test.com", "irrelevant", UserRole.FREE_TIER);
    }

    private String freshUserToken() {
        return jwtService.generateToken(getNewUser());
    }

    private String expiredUserToken(Instant expiredAt) {
        byte[] keyBytes = jwtProperties.getJwtSigningKey().getBytes(Charset.defaultCharset());
        return Jwts.builder()
            .claims()
            .empty()
            .id(UUID.randomUUID().toString())
            .subject(UUID.randomUUID() + "@test.com")
            .add("role", UserRole.FREE_TIER.name())
            .issuedAt(Date.from(expiredAt.minus(Duration.ofHours(1))))
            .expiration(Date.from(expiredAt))
            .and()
            .signWith(Keys.hmacShaKeyFor(keyBytes))
            .compact();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
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