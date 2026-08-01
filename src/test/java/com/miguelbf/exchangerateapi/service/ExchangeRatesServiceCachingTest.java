package com.miguelbf.exchangerateapi.service;

import com.miguelbf.exchangerateapi.client.ExchangeRatesClientService;
import com.miguelbf.exchangerateapi.model.clients.exchangerates.Currency;
import com.miguelbf.exchangerateapi.model.clients.exchangerates.LiveRates;
import com.miguelbf.exchangerateapi.model.dto.RatesResponse;
import com.miguelbf.exchangerateapi.service.impl.ExchangeRatesService;
import com.miguelbf.exchangerateapi.utilities.beans.TestCacheConfig;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    ExchangeRatesService.class,
    TestCacheConfig.class
})
class ExchangeRatesServiceCachingTest {

    @Autowired
    CacheManager cacheManager;

    @Autowired
    IExchangeRatesService exchangeRatesService;

    @MockitoBean
    ExchangeRatesClientService exchangeRatesClientService;

    Cache cache;

    @BeforeEach
    void clearCache() {
        Cache c = cacheManager.getCache("liveRates");
        assertNotNull(c);
        cache = c;
        cache.clear();
    }

    @ParameterizedTest
    @MethodSource("serviceArgumentsWithResponse")
    void whenSuccessfulServiceCall_thenResultIsCached(
        Currency source, @Nullable Currency target, String cacheKey, LiveRates upstreamResponse
    ) {
        when(exchangeRatesClientService.getLiveRates(source, target)).thenReturn(upstreamResponse);

        exchangeRatesService.getRates(source, target);

        assertNotNull(cache);
        RatesResponse rates = cache.get(cacheKey, RatesResponse.class);
        assertNotNull(rates);
        assertEquals(upstreamResponse.getTimestamp(), rates.timestamp());
        assertEquals(upstreamResponse.getSource(), rates.source());
        assertEquals(upstreamResponse.getQuotes(), rates.rates());
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

        assertNotNull(cache);
        RatesResponse rates = cache.get(cacheKey, RatesResponse.class);
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

        assertNotNull(cache);
        RatesResponse rates = cache.get(cacheKey, RatesResponse.class);
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
