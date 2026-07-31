package com.miguelbf.exchangerateapi.service;

import com.miguelbf.exchangerateapi.client.ExchangeRatesClientService;
import com.miguelbf.exchangerateapi.config.properties.ExchangeRatesClientProperties;
import com.miguelbf.exchangerateapi.model.clients.exchangerates.Currency;
import com.miguelbf.exchangerateapi.model.clients.exchangerates.LiveRates;
import com.miguelbf.exchangerateapi.model.dto.RatesResponse;
import com.redis.testcontainers.RedisContainer;
import org.awaitility.Awaitility;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
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
class ExchangeRatesServiceRedisCachingIT {

    @Autowired
    ExchangeRatesClientProperties exchangeRatesClientProperties;

    @Autowired
    RedisCacheManager cacheManager;

    @Autowired
    ExchangeRatesService exchangeRatesService;

    @MockitoBean
    ExchangeRatesClientService exchangeRatesClientService;

    @Container
    public static RedisContainer redisContainer = new RedisContainer(DockerImageName.parse("redis:8.10.0-alpine"));

    RedisCache redisCache;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getRedisHost);
        registry.add("spring.data.redis.port", redisContainer::getRedisPort);
    }

    @BeforeEach
    void clearCache() {
        Cache c = cacheManager.getCache("liveRates");
        assertNotNull(c);
        redisCache = (RedisCache) c;
        redisCache.clear();
    }

    @Test
    void testCacheConfigurationTTL() {
        RedisCacheConfiguration cacheConfig = redisCache.getCacheConfiguration();
        assertNotNull(cacheConfig);
        Duration configuredTTL = cacheConfig.getTtlFunction().getTimeToLive("USD", null);
        assertEquals(exchangeRatesClientProperties.getCachingTtl(), configuredTTL);
    }

    @ParameterizedTest
    @MethodSource("serviceArgumentsWithResponse")
    void whenSuccessfulServiceCall_thenResultIsCachedWithExpectedExpiringTTL(
        Currency source, @Nullable Currency target, String cacheKey, LiveRates upstreamResponse
    ) {
        when(exchangeRatesClientService.getLiveRates(source, target)).thenReturn(upstreamResponse);

        exchangeRatesService.getRates(source, target);

        assertNotNull(redisCache);
        RatesResponse rates = redisCache.get(cacheKey, RatesResponse.class);
        assertNotNull(rates);
        assertEquals(upstreamResponse.getTimestamp(), rates.timestamp());
        assertEquals(upstreamResponse.getSource(), rates.source());
        assertEquals(upstreamResponse.getQuotes(), rates.rates());
        Awaitility.await()
            .atMost(exchangeRatesClientProperties.getCachingTtl().plusSeconds(2))
            .pollInterval(Duration.ofMillis(200))
            .untilAsserted(() -> assertNull(redisCache.get(cacheKey, RatesResponse.class)));
        verify(exchangeRatesClientService, times(1)).getLiveRates(source, target);
    }

    @ParameterizedTest
    @MethodSource("serviceArgumentsWithResponse")
    void whenTwoSubsequentCallsWithSameParameters_thenSecondHitsCache(
        Currency source, @Nullable Currency target, String cacheKey, LiveRates upstreamResponse
    ) {
        when(exchangeRatesClientService.getLiveRates(source, target)).thenReturn(upstreamResponse);

        exchangeRatesService.getRates(source, target);
        exchangeRatesService.getRates(source, target);

        assertNotNull(redisCache);
        RatesResponse rates = redisCache.get(cacheKey, RatesResponse.class);
        assertNotNull(rates);
        assertEquals(upstreamResponse.getTimestamp(), rates.timestamp());
        assertEquals(upstreamResponse.getSource(), rates.source());
        assertEquals(upstreamResponse.getQuotes(), rates.rates());
        verify(exchangeRatesClientService, times(1)).getLiveRates(source, target);
    }

    @ParameterizedTest
    @MethodSource("serviceArgumentsWithResponse")
    void whenUnsuccessfulServiceCall_thenNoResultIsCached(
        Currency source, @Nullable Currency target, String cacheKey, LiveRates upstreamResponse
    ) {
        when(exchangeRatesClientService.getLiveRates(source, target)).thenThrow(new RuntimeException("Upstream service error"));

        assertThrows(RuntimeException.class, () -> exchangeRatesService.getRates(source, target));

        assertNotNull(redisCache);
        RatesResponse rates = redisCache.get(cacheKey, RatesResponse.class);
        assertNull(rates);
        verify(exchangeRatesClientService, times(1)).getLiveRates(source, target);
    }

    private static Stream<Arguments> serviceArgumentsWithResponse() {
        return Stream.of(
            Arguments.of(
                Currency.USD,
                null,
                "USD",
                new LiveRates(
                    1L, Currency.USD, Map.of(
                    "USDEUR", new BigDecimal("0.85"),
                    "USDGBP", new BigDecimal("0.74"),
                    "USDJPY", new BigDecimal("158.74")
                ))
            ),
            Arguments.of(
                Currency.EUR,
                Currency.GBP,
                "EUR:GBP",
                new LiveRates(
                    1L, Currency.EUR, Map.of(
                    "EURGBP", new BigDecimal("0.86")
                ))
            )
        );
    }

}
