package com.miguelbf.exchangerateapi.controller;

import com.miguelbf.exchangerateapi.config.security.AuthConfig;
import com.miguelbf.exchangerateapi.exception.handler.AuthExceptionHandler;
import com.miguelbf.exchangerateapi.exception.handler.GlobalExceptionHandler;
import com.miguelbf.exchangerateapi.exception.handler.UpstreamExceptionHandler;
import com.miguelbf.exchangerateapi.model.clients.exchangerates.Currency;
import com.miguelbf.exchangerateapi.model.dto.RatesResponse;
import com.miguelbf.exchangerateapi.service.IConversionService;
import com.miguelbf.exchangerateapi.service.IExchangeRatesService;
import com.miguelbf.exchangerateapi.utilities.ArgumentCombinations;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExchangeRatesController.class)
@Import(AuthConfig.class)
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
    void whenUnexpectedException_thenStatusInternalServerErrorAndReturnProblemDetail() throws Exception {
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
    void whenOnlySourceValidQueryParameter_thenStatusOkAndReturnAllRates() throws Exception {
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
    void whenSourceAndTargetValidQueryParameters_thenStatusOkAndReturnSingleRate() throws Exception {
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

    @ParameterizedTest(name = "[{index}] source={0} target={1}")
    @MethodSource("invalidQueryParams")
    void whenInvalidQueryParameters_thenStatusBadRequestAndReturnProblemDetail(Object source, Object target) throws Exception {
        // Default Problem Detail handled by Spring

        MockHttpServletRequestBuilder request = get("/api/rates").contentType(MediaType.APPLICATION_JSON);
        if (source != null) request = request.queryParam("source", source.toString());
        if (target != null) request = request.queryParam("target", target.toString());

        mockMvc
            .perform(request)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.instance", is("/api/rates")))
            .andExpect(jsonPath("$.title", is("Bad Request")))
            .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
            .andExpect(jsonPath("$.detail", not(emptyOrNullString())));

        verify(exchangeRatesService, never()).getRates(any(), any());
    }


    private static Stream<Arguments> invalidQueryParams() {
        List<Object> validSources = List.of(Currency.USD);
        List<Object> validTargets = List.of(Currency.EUR);
        List<Object> invalidSources = Arrays.asList(null, "null", "zzz", 1, "%s,%s".formatted(Currency.USD, Currency.EUR));
        List<Object> invalidTargets = Arrays.asList("null", "zzz", 1, "%s,%s".formatted(Currency.USD, Currency.EUR));

        Stream<Arguments> invalidSourceWithValidTarget = ArgumentCombinations.allCombinations(invalidSources, validTargets);
        Stream<Arguments> validSourceWithInvalidTarget = ArgumentCombinations.allCombinations(validSources, invalidTargets);

        return Stream.concat(invalidSourceWithValidTarget, validSourceWithInvalidTarget);
    }
}
