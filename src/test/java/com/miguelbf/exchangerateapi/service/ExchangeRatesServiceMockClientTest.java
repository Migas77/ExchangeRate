package com.miguelbf.exchangerateapi.service;

import com.miguelbf.exchangerateapi.client.IExchangeRatesClientService;
import com.miguelbf.exchangerateapi.exception.RatesUpstreamDataException;
import com.miguelbf.exchangerateapi.model.clients.exchangerates.Currency;
import com.miguelbf.exchangerateapi.model.clients.exchangerates.LiveRates;
import com.miguelbf.exchangerateapi.model.dto.RatesResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class ExchangeRatesServiceMockClientTest {

    @InjectMocks
    ExchangeRatesService exchangeRatesService;

    @Mock
    IExchangeRatesClientService exchangeRatesClientService;

    @Test
    void whenValidBaseCurrencyWithoutTarget_thenReturnAllRates() {
        LiveRates liveRates = new LiveRates(
            1L, Currency.USD, Map.of(
                "USDEUR", new BigDecimal("0.85"),
                "USDGBP", new BigDecimal("0.74"),
                "USDJPY", new BigDecimal("158.74")
            )
        );
        when(exchangeRatesClientService.getLiveRates(Currency.USD, null)).thenReturn(liveRates);

        RatesResponse ratesResponse = exchangeRatesService.getRates(Currency.USD, null);

        assertEquals(ratesResponse.timestamp(), liveRates.getTimestamp());
        assertEquals(ratesResponse.source(), liveRates.getSource());
        assertEquals(ratesResponse.rates(), liveRates.getQuotes());
        verify(exchangeRatesClientService, times(1)).getLiveRates(Currency.USD, null);
    }

    @Test
    void whenValidBaseCurrencyWithTarget_thenReturnCorrectRate() {
        LiveRates liveRates = new LiveRates(
            1L, Currency.USD, Map.of(
                "USDEUR", new BigDecimal("0.85")
            )
        );
        when(exchangeRatesClientService.getLiveRates(Currency.USD, Currency.EUR)).thenReturn(liveRates);

        RatesResponse ratesResponse = exchangeRatesService.getRates(Currency.USD, Currency.EUR);

        assertEquals(ratesResponse.timestamp(), liveRates.getTimestamp());
        assertEquals(ratesResponse.source(), liveRates.getSource());
        assertEquals(ratesResponse.rates(), liveRates.getQuotes());
        verify(exchangeRatesClientService, times(1)).getLiveRates(Currency.USD, Currency.EUR);
    }

    @Test
    void whenUpstreamSourceDoesNotMatchRequestedSource_thenThrowsUnexpectedSourceException() {
        // spy used to bypass JsonCreator (validation of JsonCreator will be tested on different test suite)
        LiveRates spy = spy(new LiveRates(1L, Currency.USD, Map.of("USDEUR", new BigDecimal("0.85"))));
        doReturn(Currency.GBP).when(spy).getSource();
        when(exchangeRatesClientService.getLiveRates(Currency.USD, Currency.EUR)).thenReturn(spy);

        RatesUpstreamDataException.UnexpectedSource ex = assertThrows(
            RatesUpstreamDataException.UnexpectedSource.class,
            () -> exchangeRatesService.getRates(Currency.USD, Currency.EUR)
        );

        assertEquals(Currency.USD, ex.getFrom());
        assertEquals(Currency.EUR, ex.getTo());
        assertEquals(spy, ex.getLiveRates());
        verify(exchangeRatesClientService, times(1)).getLiveRates(Currency.USD, Currency.EUR);
        verify(spy, times(1)).getSource();
    }

    @Test
    void whenUpstreamReturnsMultipleQuotesForSingleTarget_thenThrowsUnexpectedQuoteCountException() {
        LiveRates spy = spy(new LiveRates(1L, Currency.USD, Map.of("USDEUR", new BigDecimal("0.85"))));
        doReturn(Map.of(
            Currency.EUR, new BigDecimal("0.85"),
            Currency.GBP, new BigDecimal("0.74")
        )).when(spy).getQuotes();
        when(exchangeRatesClientService.getLiveRates(Currency.USD, Currency.EUR)).thenReturn(spy);

        RatesUpstreamDataException.UnexpectedQuoteCount ex = assertThrows(
            RatesUpstreamDataException.UnexpectedQuoteCount.class,
            () -> exchangeRatesService.getRates(Currency.USD, Currency.EUR)
        );

        assertEquals(Currency.USD, ex.getFrom());
        assertEquals(Currency.EUR, ex.getTo());
        assertEquals(spy, ex.getLiveRates());
        verify(exchangeRatesClientService, times(1)).getLiveRates(Currency.USD, Currency.EUR);
        verify(spy, times(1)).getQuotes();
    }

    @Test
    void whenUpstreamQuotesDoNotContainRequestedTarget_thenThrowsUnexpectedTargetException() {
        LiveRates spy = spy(new LiveRates(1L, Currency.USD, Map.of("USDEUR", new BigDecimal("0.85"))));
        doReturn(Map.of(Currency.GBP, new BigDecimal("0.74"))).when(spy).getQuotes();
        when(exchangeRatesClientService.getLiveRates(Currency.USD, Currency.EUR)).thenReturn(spy);

        RatesUpstreamDataException.UnexpectedTarget ex = assertThrows(
            RatesUpstreamDataException.UnexpectedTarget.class,
            () -> exchangeRatesService.getRates(Currency.USD, Currency.EUR)
        );

        assertEquals(Currency.USD, ex.getFrom());
        assertEquals(Currency.EUR, ex.getTo());
        assertEquals(spy, ex.getLiveRates());
        verify(exchangeRatesClientService, times(1)).getLiveRates(Currency.USD, Currency.EUR);
        verify(spy, times(1)).getQuotes();
    }

    @Test
    void whenUpstreamReturnsSingleQuoteForNullTarget_thenThrowsUnexpectedQuoteCountException() {
        LiveRates spy = spy(new LiveRates(1L, Currency.USD, Map.of(
            "USDEUR", new BigDecimal("0.85"),
            "USDGBP", new BigDecimal("0.74"),
            "USDJPY", new BigDecimal("158.74")
        )));
        doReturn(Map.of(Currency.EUR, new BigDecimal("0.85"))).when(spy).getQuotes();
        when(exchangeRatesClientService.getLiveRates(Currency.USD, null)).thenReturn(spy);

        RatesUpstreamDataException.UnexpectedQuoteCount ex = assertThrows(
            RatesUpstreamDataException.UnexpectedQuoteCount.class,
            () -> exchangeRatesService.getRates(Currency.USD, null)
        );

        assertEquals(Currency.USD, ex.getFrom());
        assertNull(ex.getTo());
        assertEquals(spy, ex.getLiveRates());
        verify(exchangeRatesClientService, times(1)).getLiveRates(Currency.USD, null);
        verify(spy, times(1)).getQuotes();
    }

}
