package com.miguelbf.exchangerateapi.controller;

import com.miguelbf.exchangerateapi.config.JacksonConfig;
import com.miguelbf.exchangerateapi.config.security.AuthConfig;
import com.miguelbf.exchangerateapi.exception.handler.AuthExceptionHandler;
import com.miguelbf.exchangerateapi.exception.handler.GlobalExceptionHandler;
import com.miguelbf.exchangerateapi.exception.handler.UpstreamExceptionHandler;
import com.miguelbf.exchangerateapi.model.clients.exchangerates.Currency;
import com.miguelbf.exchangerateapi.model.dto.ConversionResponse;
import com.miguelbf.exchangerateapi.model.dto.RatesResponse;
import com.miguelbf.exchangerateapi.service.IConversionService;
import com.miguelbf.exchangerateapi.service.IExchangeRatesService;
import com.miguelbf.exchangerateapi.utilities.CustomMatchers;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExchangeRatesController.class)
@Import({AuthConfig.class, JacksonConfig.class})
class ExchangeRatesControllerMockServiceTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IExchangeRatesService exchangeRatesService;

    @MockitoBean
    private IConversionService conversionService;

    @Test
    void whenExchangeRatesControllerLoaded_thenExceptionHandlersArePresent() {
        // Throws NoSuchBeanDefinitionException if GlobalExceptionHandler not configured
        assertDoesNotThrow(() -> context.getBean(GlobalExceptionHandler.class));
        assertDoesNotThrow(() -> context.getBean(UpstreamExceptionHandler.class));
        assertDoesNotThrow(() -> context.getBean(AuthExceptionHandler.class));
    }

    @Test
    void givenUnexpectedException_WhenGetRates_thenStatusInternalServerErrorAndReturnProblemDetail() throws Exception {
        // Generic Problem Detail handled by GlobalExceptionHandler
        when(exchangeRatesService.getRates(any(), any())).thenThrow(new RuntimeException("Unexpected error"));

        mockMvc
            .perform(
                get("/api/rates")
                    .queryParam("source", Currency.USD.name())
                    .queryParam("target", Currency.EUR.name())
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.instance", is("/api/rates")))
            .andExpect(jsonPath("$.title", is("Internal Server Error")))
            .andExpect(jsonPath("$.status", is(HttpStatus.INTERNAL_SERVER_ERROR.value())))
            .andExpect(jsonPath("$.detail", not(emptyOrNullString())));

        verify(exchangeRatesService, times(1)).getRates(any(), any());
    }

    @Test
    void givenOnlySourceValidQueryParameter_whenGetRates_thenStatusOkAndReturnAllRates() throws Exception {
        when(exchangeRatesService.getRates(Currency.USD, null))
            .thenReturn(new RatesResponse(1L, Currency.USD, Map.of(
                Currency.EUR, new BigDecimal("0.85"),
                Currency.GBP, new BigDecimal("0.74"),
                Currency.JPY, new BigDecimal("158.74")
            )));

        mockMvc
            .perform(
                get("/api/rates")
                    .queryParam("source", Currency.USD.name())
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.timestamp", is(1)))
            .andExpect(jsonPath("$.source", is(Currency.USD.name())))
            .andExpect(jsonPath("$.rates", aMapWithSize(3)))
            .andExpect(jsonPath("$.rates.EUR", is(0.85)))
            .andExpect(jsonPath("$.rates.GBP", is(0.74)))
            .andExpect(jsonPath("$.rates.JPY", is(158.74)));

        verify(exchangeRatesService, times(1)).getRates(Currency.USD, null);
    }

    @Test
    void givenSourceAndTargetValidQueryParameters_whenGetRates_thenStatusOkAndReturnSingleRate() throws Exception {
        when(exchangeRatesService.getRates(Currency.USD, Currency.EUR))
            .thenReturn(new RatesResponse(1L, Currency.USD, Map.of(Currency.EUR, new BigDecimal("0.85"))));

        mockMvc
            .perform(
                get("/api/rates")
                    .queryParam("source", Currency.USD.name())
                    .queryParam("target", Currency.EUR.name())
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.timestamp", is(1)))
            .andExpect(jsonPath("$.source", is(Currency.USD.name())))
            .andExpect(jsonPath("$.rates", aMapWithSize(1)))
            .andExpect(jsonPath("$.rates.EUR", is(0.85)));

        verify(exchangeRatesService, times(1)).getRates(Currency.USD, Currency.EUR);
    }

    @Test
    void givenRepeatedSourceAndTargetQueryParameters_whenGetRates_thenStatusBadRequestWithProblemDetails() throws Exception {
        mockMvc
            .perform(
                get("/api/rates")
                    .queryParam("source", Currency.USD.name())
                    .queryParam("target", Currency.USD.name())
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.instance", is("/api/rates")))
            .andExpect(jsonPath("$.title", is("Bad Request")))
            .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
            .andExpect(jsonPath("$.detail", not(emptyOrNullString())))
            .andExpect(jsonPath("$.detail", is("Source and target currency must not match: USD")));

        verify(exchangeRatesService, times(0)).getRates(Currency.USD, Currency.USD);
    }

    @ParameterizedTest(name = "[{index}] source={0} target={1}")
    @MethodSource("invalidGetApiRatesQueryParams")
    void givenInvalidQueryParameters_whenGetRates_thenStatusBadRequestAndReturnProblemDetail(
        Object source, Object target
    ) throws Exception {
        // Default Problem Detail handled by Spring

        MockHttpServletRequestBuilder request = get("/api/rates").contentType(MediaType.APPLICATION_JSON);
        if (source != null) request.queryParam("source", source.toString());
        if (target != null) request.queryParam("target", target.toString());

        mockMvc
            .perform(request)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.instance", is("/api/rates")))
            .andExpect(jsonPath("$.title", is("Bad Request")))
            .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
            .andExpect(jsonPath("$.detail", not(emptyOrNullString())));

        verify(exchangeRatesService, never()).getRates(any(), any());
    }

    @ParameterizedTest(name = "[{index}] amount={0} source={1} targets={2} ")
    @MethodSource("invalidGetConversionQueryParams")
    void givenInvalidQueryParameters_whenGetConversion_thenStatusBadRequestAndReturnProblemDetail(
        Object amount, Object source, Object targets
    ) throws Exception {
        // Default Problem Detail handled by Spring

        MockHttpServletRequestBuilder request = get("/api/conversions").contentType(MediaType.APPLICATION_JSON);
        if (amount != null) request.queryParam("amount", amount.toString());
        if (source != null) request.queryParam("source", source.toString());
        if (targets != null) request.queryParam("targets", targets.toString());

        mockMvc
            .perform(request)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.instance", is("/api/conversions")))
            .andExpect(jsonPath("$.title", is("Bad Request")))
            .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
            .andExpect(jsonPath("$.detail", not(emptyOrNullString())));

        verify(conversionService, never()).convertValue(any(), any(), any());
    }

    @ParameterizedTest
    @CsvSource({
        "USD, USD",
        "USD, 'USD,EUR'"
    })
    void givenRepeatedSourceAndTargets_whenGetConversion_thenStatusBadRequestAndReturnProblemDetails(
        String source, String targets
    ) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/conversions")
            .contentType(MediaType.APPLICATION_JSON).queryParam("amount", "100");
        if (source != null) request.queryParam("source", source);
        if (targets != null) request.queryParam("targets", targets);

        mockMvc
            .perform(request)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.instance", is("/api/conversions")))
            .andExpect(jsonPath("$.title", is("Bad Request")))
            .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
            .andExpect(jsonPath("$.detail", not(emptyOrNullString())))
            .andExpect(jsonPath("$.detail", is("Source and target currencies must not match: USD")));

        verify(conversionService, never()).convertValue(any(), any(), any());
    }

    @ParameterizedTest
    @MethodSource("invalidConversionAmountsAndExpectedErrorMessageMatcher")
    void givenInvalidAmount_whenGetConversion_thenStatusBadRequestAndReturnProblemDetails(
        String amount, Matcher<String> expectedErrorMessagesMatcher
    ) throws Exception {
        mockMvc
            .perform(
                get("/api/conversions")
                    .queryParam("amount", amount)
                    .queryParam("source", Currency.USD.name())
                    .queryParam("targets", "EUR,GBP")
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.instance", is("/api/conversions")))
            .andExpect(jsonPath("$.title", is("Bad Request")))
            .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
            .andExpect(jsonPath("$.detail", not(emptyOrNullString())))
            .andExpect(jsonPath("$.detail", expectedErrorMessagesMatcher));

        verify(conversionService, never()).convertValue(any(), any(), any());
    }

    @Test
    void givenValidAmountAndSourceAndMissingTargets_whenGetConversion_thenGetMultipleConversions() throws Exception {
        when(conversionService.convertValue(new BigDecimal(100), Currency.USD, Set.of()))
            .thenReturn(new ConversionResponse(1L, new BigDecimal(100), Currency.USD, Map.of(
                Currency.EUR, new BigDecimal("0.85"),
                Currency.GBP, new BigDecimal("0.74"),
                Currency.JPY, new BigDecimal("158.74")
            ), Map.of(
                Currency.EUR, new BigDecimal("85"),
                Currency.GBP, new BigDecimal("74"),
                Currency.JPY, new BigDecimal("158.74")
            )));

        mockMvc
            .perform(
                get("/api/conversions")
                    .queryParam("amount", "100")
                    .queryParam("source", Currency.USD.name())
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.timestamp", is(1)))
            .andExpect(jsonPath("$.amount", is(100)))
            .andExpect(jsonPath("$.source", is(Currency.USD.name())))
            .andExpect(jsonPath("$.rates", aMapWithSize(3)))
            .andExpect(jsonPath("$.rates.EUR", is(0.85)))
            .andExpect(jsonPath("$.rates.GBP", is(0.74)))
            .andExpect(jsonPath("$.rates.JPY", is(158.74)))
            .andExpect(jsonPath("$.convertedValues", aMapWithSize(3)))
            .andExpect(jsonPath("$.convertedValues.EUR", is(85)))
            .andExpect(jsonPath("$.convertedValues.GBP", is(74)))
            .andExpect(jsonPath("$.convertedValues.JPY", is(158.74)));

        verify(conversionService, times(1)).convertValue(new BigDecimal(100), Currency.USD, Set.of());
    }

    @Test
    void givenValidAmountSourceAndTargets_whenGetConversion_thenGetSpecifiedConversions() throws Exception {
        when(conversionService.convertValue(new BigDecimal(100), Currency.USD, Set.of(Currency.EUR, Currency.GBP)))
            .thenReturn(new ConversionResponse(1L, new BigDecimal(100), Currency.USD, Map.of(
                Currency.EUR, new BigDecimal("0.85"),
                Currency.GBP, new BigDecimal("0.74")
            ), Map.of(
                Currency.EUR, new BigDecimal("85"),
                Currency.GBP, new BigDecimal("74")
            )));

        mockMvc
            .perform(
                get("/api/conversions")
                    .queryParam("amount", "100")
                    .queryParam("source", Currency.USD.name())
                    .queryParam("targets", "EUR,GBP")
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.timestamp", is(1)))
            .andExpect(jsonPath("$.amount", is(100)))
            .andExpect(jsonPath("$.source", is(Currency.USD.name())))
            .andExpect(jsonPath("$.rates", aMapWithSize(2)))
            .andExpect(jsonPath("$.rates.EUR", is(0.85)))
            .andExpect(jsonPath("$.rates.GBP", is(0.74)))
            .andExpect(jsonPath("$.convertedValues", aMapWithSize(2)))
            .andExpect(jsonPath("$.convertedValues.EUR", is(85)))
            .andExpect(jsonPath("$.convertedValues.GBP", is(74)));

        verify(conversionService, times(1)).convertValue(
            new BigDecimal(100), Currency.USD, Set.of(Currency.EUR, Currency.GBP));
    }

    @Test
    void givenValidRequest_whenGetConversionAndServiceReturnsScientificNotation_thenResponseHasNoNotation() throws Exception {
        BigDecimal sciNotBigDecimal = new BigDecimal("850").stripTrailingZeros();
        when(conversionService.convertValue(new BigDecimal(1000), Currency.USD, Set.of(Currency.EUR)))
            .thenReturn(new ConversionResponse(1L, new BigDecimal(1000), Currency.USD,
                Map.of(Currency.EUR, new BigDecimal("0.85")), Map.of(Currency.EUR, sciNotBigDecimal)));
        assertEquals("8.5E+2", sciNotBigDecimal.toString());
        assertEquals("850", sciNotBigDecimal.toPlainString());

        mockMvc
            .perform(
                get("/api/conversions")
                    .queryParam("amount", "1000")
                    .queryParam("source", Currency.USD.name())
                    .queryParam("targets", "EUR")
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.timestamp", is(1)))
            .andExpect(jsonPath("$.amount", is(1000)))
            .andExpect(jsonPath("$.source", is(Currency.USD.name())))
            .andExpect(jsonPath("$.rates", aMapWithSize(1)))
            .andExpect(jsonPath("$.rates.EUR", is(0.85)))
            .andExpect(jsonPath("$.convertedValues", aMapWithSize(1)))
            .andExpect(jsonPath("$.convertedValues.EUR", is(850)));

        verify(conversionService, times(1)).convertValue(
            new BigDecimal(1000), Currency.USD, Set.of(Currency.EUR));
    }



    private static Stream<Arguments> invalidGetApiRatesQueryParams() {
        Currency validSource = Currency.USD;
        Currency validTarget = Currency.EUR;
        List<Object> invalidSources = Arrays.asList(null, "null", "zzz", 1, "JPY,AUD", ",JPY");
        List<Object> invalidTargets = Arrays.asList("null", "zzz", 1, "JPY,AUD", ",JPY");

        Stream<Arguments> invalidSourceCases = invalidSources.stream().map(source -> Arguments.of(source, validTarget));
        Stream<Arguments> invalidTargetCases = invalidTargets.stream().map(target -> Arguments.of(validSource, target));

        return Stream.concat(invalidSourceCases, invalidTargetCases);
    }

    private static Stream<Arguments> invalidGetConversionQueryParams() {
        Object validAmount = "100";
        Currency validSource = Currency.USD;
        String validTargets = "EUR,GBP";

        List<Object> invalidAmounts = Arrays.asList(null, "null", -1, "abc", "1,000", "1.2.3");
        List<Object> invalidSources = Arrays.asList(null, "null", "zzz", 1, "JPY,AUD", ",AUD");
        List<Object> invalidTargets = Arrays.asList("null", "zzz", 1, "AUD,zzz", "AUD,111");

        Stream<Arguments> invalidAmountCases = invalidAmounts.stream().map(amount -> Arguments.of(amount, validSource, validTargets));
        Stream<Arguments> invalidSourceCases = invalidSources.stream().map(source -> Arguments.of(validAmount, source, validTargets));
        Stream<Arguments> invalidTargetCases = invalidTargets.stream().map(target -> Arguments.of(validAmount, validSource, target));

        return Stream.concat(Stream.concat(invalidAmountCases, invalidSourceCases), invalidTargetCases);
    }

    private static Stream<Arguments> invalidConversionAmountsAndExpectedErrorMessageMatcher() {
        return Stream.of(
            Arguments.of("1000000001", is("amount: amount must be less than or equal to 1000000000")),
            Arguments.of("0.000000", is("amount: amount must be greater than or equal to 0.000001")),
            Arguments.of("10000000000.0", CustomMatchers.containsAllStrings(
                "amount: amount must be less than or equal to 1000000000",
                "amount: amount must have at most 10 integer digit(s) and 6 fractional digit(s)"
            )),
            Arguments.of("0.0000001", CustomMatchers.containsAllStrings(
                "amount: amount must be greater than or equal to 0.000001",
                "amount: amount must have at most 10 integer digit(s) and 6 fractional digit(s)"))
        );
    }

}
