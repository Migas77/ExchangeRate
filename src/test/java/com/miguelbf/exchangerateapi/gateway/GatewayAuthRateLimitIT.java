package com.miguelbf.exchangerateapi.gateway;

import com.miguelbf.exchangerateapi.config.properties.RateLimitProperties;
import com.miguelbf.exchangerateapi.entities.User;
import com.miguelbf.exchangerateapi.entities.UserRole;
import com.miguelbf.exchangerateapi.repository.UserRepository;
import com.miguelbf.exchangerateapi.service.IJwtService;
import com.miguelbf.exchangerateapi.utilities.stubs.StubController;
import com.miguelbf.exchangerateapi.utilities.stubs.StubService;
import com.redis.testcontainers.RedisContainer;
import io.github.bucket4j.distributed.serialization.InternalSerializationHelper;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.autoconfigure.exclude=" +
            "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
            "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration," +
            "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
    }
)
@Testcontainers
@AutoConfigureWebTestClient
@Import(StubController.class)
class GatewayAuthRateLimitIT {

    private static final String ALL_BUCKET_KEYS_PATTERN = "b4j::*";
    private static final String USER_KEY_PREFIX = "b4j::user:";
    private static final String IP_KEY_PREFIX = "b4j::ip:";

    @Container
    public static RedisContainer redisContainer = new RedisContainer(DockerImageName.parse("redis:8.10.0-alpine"));

    @Autowired
    private StatefulRedisConnection<String, byte[]> bucket4jRedisConnection;

    @Autowired
    private RateLimitProperties rateLimitProperties;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private IJwtService jwtService;

    @MockitoBean
    private StubService stubService;

    @MockitoBean
    private UserRepository userRepository;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getRedisHost);
        registry.add("spring.data.redis.port", redisContainer::getRedisPort);
    }

    @BeforeEach
    void clearCache() {
        bucket4jRedisConnection.sync().flushall();
    }

    @Test
    void givenFreeTierUser_whenPremiumOnlyEndpoint_thenStatusForbiddenAndWWWAuthenticateHeadersAndEmptyBody() {
        webTestClient.get()
            .uri("/gw/stub/premium")
            .headers(httpHeaders -> httpHeaders.setBearerAuth(freshUserToken(UserRole.FREE_TIER)))
            .exchange()
            .expectStatus().isForbidden()
            .expectHeader().valueEquals(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"insufficient_scope\", " +
                "error_description=\"The request requires higher privileges than provided by the access token.\", " +
                "error_uri=\"https://tools.ietf.org/html/rfc6750#section-3.1\"")
            .expectBody().isEmpty();

        verify(stubService, never()).premiumCall();
    }

    @Test
    void givenPremiumTierUser_whenPremiumOnlyEndpoint_thenStatusOkAndServiceReached() {
        webTestClient.get()
            .uri("/gw/stub/premium")
            .headers(httpHeaders -> httpHeaders.setBearerAuth(freshUserToken(UserRole.PREMIUM_TIER)))
            .exchange()
            .expectStatus().isOk()
            .expectHeader().doesNotExist(HttpHeaders.WWW_AUTHENTICATE);

        verify(stubService, times(1)).premiumCall();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void givenUnlimitedRateRoute_whenCalledRepeatedlyForAuthAndAnonymousUser_thenNeverRateLimitedAndNoBucketCreated(
        boolean isAuthenticated
    ) {
        HttpHeaders headers = new HttpHeaders();
        if (isAuthenticated) {
            User exhaustedUser = getNewUser();
            headers.setBearerAuth(jwtService.generateToken(exhaustedUser));
        }

        for (int i = 0; i < rateLimitProperties.getCapacity() + 10; i++) {
            webTestClient.get()
                .uri("/gw/v3/api-docs")
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().doesNotExist(HttpHeaders.WWW_AUTHENTICATE);
        }

        List<String> buckets = bucket4jRedisConnection.sync().keys(ALL_BUCKET_KEYS_PATTERN);
        assertTrue(buckets.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void givenRateLimitedRoute_whenBucketCapacityExceededForAuthAndAnonymousUser_thenTooManyRequests(
        boolean isAuthenticated
    ) {
        HttpHeaders headers = new HttpHeaders();
        String bucketKey = IP_KEY_PREFIX + "127.0.0.1";
        if (isAuthenticated) {
            User exhaustedUser = getNewUser();
            bucketKey = USER_KEY_PREFIX + exhaustedUser.getUsername();
            headers.setBearerAuth(jwtService.generateToken(exhaustedUser));
        }

        for (int i = 0; i < rateLimitProperties.getCapacity(); i++) {
            long expectedRemainingTokens = rateLimitProperties.getCapacity() - i - 1;

            // This call triggers bad request because the body is not present, but this is not the point of the test
            webTestClient.post()
                .uri("/gw/api/auth/login")
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().doesNotExist(HttpHeaders.WWW_AUTHENTICATE)
                .expectHeader().valueEquals("X-RateLimit-Remaining", String.valueOf(expectedRemainingTokens));

            assertEquals(expectedRemainingTokens, getAvailableTokens(bucketKey));
        }

        webTestClient.post()
            .uri("/gw/api/auth/login")
            .headers(httpHeaders -> httpHeaders.addAll(headers))
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
            .expectHeader().doesNotExist(HttpHeaders.WWW_AUTHENTICATE)
            .expectHeader().valueEquals("X-RateLimit-Remaining", "0")
            .expectBody().isEmpty();

        assertEquals(0, getAvailableTokens(bucketKey));
    }

    private long getAvailableTokens(String bucketKey) {
        RedisCommands<String, byte[]> commands = bucket4jRedisConnection.sync();
        assertEquals(List.of(bucketKey), commands.keys(ALL_BUCKET_KEYS_PATTERN));
        return InternalSerializationHelper.deserializeState(commands.get(bucketKey)).getAvailableTokens();
    }

    private User getNewUser() {
        return new User(UUID.randomUUID() + "@test.com", "irrelevant", UserRole.FREE_TIER);
    }

    private String freshUserToken(UserRole role) {
        return jwtService.generateToken(new User(UUID.randomUUID() + "@test.com", "irrelevant", role));
    }

}
