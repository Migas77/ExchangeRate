package com.miguelbf.exchangerateapi.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.miguelbf.exchangerateapi.exception.exception.IllegalConversionException;
import com.miguelbf.exchangerateapi.exception.exception.RatesUpstreamDataException;
import com.miguelbf.exchangerateapi.model.clients.exchangerates.Currency;
import com.miguelbf.exchangerateapi.model.dto.ConversionResponse;
import com.miguelbf.exchangerateapi.model.dto.RatesResponse;
import com.miguelbf.exchangerateapi.service.impl.ConversionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class})
class ConversionServiceMockServicesTest {

    @InjectMocks
    ConversionService conversionService;

    @Mock
    IExchangeRatesService exchangeRatesService;

    @ParameterizedTest
    @CsvSource({
        "10000000000000000, 0, 17",
        "10000000000.1, 1, 11",
        "11.0000001, 7, 2"
    })
    void givenInvalidAmounts_whenConvertValue_thenThrowIllegalConversionException(
        String strValue, int decimalDigits, int integerDigits
    ) {
        BigDecimal valueToBeConverted = new BigDecimal(strValue);

        IllegalConversionException illegalConversionException = assertThrows(
            IllegalConversionException.class,
            () -> conversionService.convertValue(valueToBeConverted, Currency.USD, Set.of())
        );

        assertEquals("Unexpected amount shape", illegalConversionException.getMessage());
        assertEquals(decimalDigits, illegalConversionException.getDecimalDigits());
        assertEquals(integerDigits, illegalConversionException.getIntegerDigits());
        verify(exchangeRatesService, never()).getRates(any(), any());
    }

    @Test
    void givenNonEmptyTarget_whenFetchRatesAndMissingRates_thenThrowMissingTargetException() {
        BigDecimal valueToBeConverted = new BigDecimal("12.1");
        when(exchangeRatesService.getRates(Currency.USD, Currency.EUR)).thenReturn(
            new RatesResponse(1L, Currency.USD, Map.of()));

        RatesUpstreamDataException.MissingTarget missingTargetEx = assertThrows(
            RatesUpstreamDataException.MissingTarget.class,
            () -> conversionService.convertValue(valueToBeConverted, Currency.USD, Set.of(Currency.EUR))
        );

        assertEquals("Expected quote for source USD to target EUR, got missing", missingTargetEx.getMessage());
        assertEquals(Currency.USD, missingTargetEx.getSource());
        assertEquals(Currency.EUR, missingTargetEx.getTarget());
        assertEquals(Currency.USD, missingTargetEx.getActSource());
        assertEquals(Map.of(), missingTargetEx.getActQuotes());
        verify(exchangeRatesService, times(1)).getRates(Currency.USD, Currency.EUR);
    }

    @Test
    void givenSingleTarget_whenFetchRateAndValidRate_thenCorrectlyCalculateAndReturnConversionResponse() {
        BigDecimal valueToBeConverted = new BigDecimal("12.1");
        RatesResponse ratesRetrieved = new RatesResponse(1L, Currency.USD, Map.of(Currency.EUR, new BigDecimal("0.85")));
        when(exchangeRatesService.getRates(Currency.USD, Currency.EUR)).thenReturn(ratesRetrieved);

        ConversionResponse conversionResponse = conversionService.convertValue(valueToBeConverted, Currency.USD, Set.of(Currency.EUR));

        assertEquals(ratesRetrieved.timestamp(), conversionResponse.timestamp());
        assertEquals(Currency.USD, conversionResponse.source());
        assertEquals(valueToBeConverted, conversionResponse.amount());
        assertEquals(ratesRetrieved.source(), conversionResponse.source());
        assertEquals(ratesRetrieved.rates(), conversionResponse.rates());
        assertEquals(ratesRetrieved.rates().keySet(), conversionResponse.convertedValues().keySet());
        assertEquals(new BigDecimal("10.285"), conversionResponse.convertedValues().get(Currency.EUR));
        verify(exchangeRatesService, times(1)).getRates(Currency.USD, Currency.EUR);
    }

    @Test
    void givenMultipleTargets_whenFetchAllValidRates_thenFilterRatesAndCorrectlyCalculateAndReturnConversionResponse() {
        BigDecimal valueToBeConverted = new BigDecimal("12.1");
        RatesResponse ratesRetrieved = new RatesResponse(1L, Currency.USD, Map.of(
            Currency.EUR, new BigDecimal("0.85"),
            Currency.GBP, new BigDecimal("0.74"),
            Currency.JPY, new BigDecimal("158.74")
        ));
        when(exchangeRatesService.getRates(Currency.USD, null)).thenReturn(ratesRetrieved);

        ConversionResponse conversionResponse = conversionService.convertValue(
            valueToBeConverted, Currency.USD, Set.of(Currency.EUR, Currency.GBP));

        assertEquals(ratesRetrieved.timestamp(), conversionResponse.timestamp());
        assertEquals(Currency.USD, conversionResponse.source());
        assertEquals(valueToBeConverted, conversionResponse.amount());
        assertEquals(ratesRetrieved.source(), conversionResponse.source());
        assertEquals(2, conversionResponse.rates().size());
        assertEquals(ratesRetrieved.rates().containsKey(Currency.EUR), conversionResponse.convertedValues().containsKey(Currency.EUR));
        assertEquals(ratesRetrieved.rates().containsKey(Currency.GBP), conversionResponse.convertedValues().containsKey(Currency.GBP));
        assertEquals(new BigDecimal("10.285"), conversionResponse.convertedValues().get(Currency.EUR));
        assertEquals(new BigDecimal("8.954"), conversionResponse.convertedValues().get(Currency.GBP));
        verify(exchangeRatesService, times(1)).getRates(Currency.USD, null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"10000000000000000", "10000000000.1", "11.00000000000012"})
    void givenEmptyTarget_whenFetchAllRatesAndInvalidRates_thenLog(String strValue) {
        BigDecimal valueToBeConverted = new BigDecimal("12.1");
        when(exchangeRatesService.getRates(Currency.USD, null)).thenReturn(
            new RatesResponse(1L, Currency.USD, Map.of(Currency.EUR, new BigDecimal(strValue))));
        Logger logger = (Logger) LoggerFactory.getLogger(ConversionService.class);
        ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);

        try {
            conversionService.convertValue(valueToBeConverted, Currency.USD, Set.of());
        } finally {
            logAppender.stop();
            logger.detachAppender(logAppender);
        }

        assertEquals(1, logAppender.list.size());
        ILoggingEvent event = logAppender.list.getFirst();
        assertEquals(Level.WARN, event.getLevel());
        assertEquals("Unexpected rate shape", event.getMessage());
        verify(exchangeRatesService, times(1)).getRates(Currency.USD, null);
    }

    @Test
    void givenEmptyTarget_whenFetchRatesAndValidRates_thenCorrectlyCalculateAndReturnConversionResponse() {
        BigDecimal valueToBeConverted = new BigDecimal("12.1");
        RatesResponse ratesRetrieved = new RatesResponse(1L, Currency.USD, Map.of(
            Currency.EUR, new BigDecimal("0.85"),
            Currency.GBP, new BigDecimal("0.74"),
            Currency.JPY, new BigDecimal("158.74")
        ));
        when(exchangeRatesService.getRates(Currency.USD, null)).thenReturn(ratesRetrieved);

        ConversionResponse conversionResponse = conversionService.convertValue(valueToBeConverted, Currency.USD, Set.of());

        assertEquals(ratesRetrieved.timestamp(), conversionResponse.timestamp());
        assertEquals(Currency.USD, conversionResponse.source());
        assertEquals(valueToBeConverted, conversionResponse.amount());
        assertEquals(ratesRetrieved.source(), conversionResponse.source());
        assertEquals(ratesRetrieved.rates(), conversionResponse.rates());
        assertEquals(ratesRetrieved.rates().keySet(), conversionResponse.convertedValues().keySet());
        assertEquals(new BigDecimal("10.285"), conversionResponse.convertedValues().get(Currency.EUR));
        assertEquals(new BigDecimal("8.954"), conversionResponse.convertedValues().get(Currency.GBP));
        assertEquals(new BigDecimal("1920.754"), conversionResponse.convertedValues().get(Currency.JPY));
        verify(exchangeRatesService, times(1)).getRates(Currency.USD, null);
    }

}
