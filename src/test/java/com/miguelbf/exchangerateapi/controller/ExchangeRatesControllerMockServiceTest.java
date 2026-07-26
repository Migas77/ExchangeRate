package com.miguelbf.exchangerateapi.controller;

import com.miguelbf.exchangerateapi.exception.GlobalExceptionHandler;
import com.miguelbf.exchangerateapi.model.clients.exchangerates.Currency;
import com.miguelbf.exchangerateapi.model.dto.RatesResponse;
import com.miguelbf.exchangerateapi.service.IExchangeRatesService;
import com.miguelbf.exchangerateapi.utilities.ArgumentCombinations;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
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
class ExchangeRatesControllerMockServiceTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IExchangeRatesService exchangeRatesService;

    @Test
    void whenExchangeRatesControllerLoaded_thenGlobalExceptionHandlerIsPresent() {
        // Throws NoSuchBeanDefinitionException if GlobalExceptionHandler not configured
        assertDoesNotThrow(() -> context.getBean(GlobalExceptionHandler.class));
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
    void whenOnlyFromValidQueryParameter_thenStatusOkAndReturnAllRates() throws Exception {
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
    void whenFromAndToValidQueryParameters_thenStatusOkAndReturnSingleRate() throws Exception {
        // shall not reject the request despite from and to query parameters being the same
        when(exchangeRatesService.getRates(Currency.EUR, Currency.EUR))
            .thenReturn(new RatesResponse(1L, Currency.EUR, Map.of(Currency.EUR, new BigDecimal("1.0"))));

        mockMvc
            .perform(
                get("/api/rates")
                .queryParam("source", Currency.EUR.name())
                .queryParam("target", Currency.EUR.name())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.timestamp", is(1)))
            .andExpect(jsonPath("$.source", is(Currency.EUR.name())))
            .andExpect(jsonPath("$.rates", aMapWithSize(1)))
            .andExpect(jsonPath("$.rates.EUR", is(1.0)));

        verify(exchangeRatesService, times(1)).getRates(Currency.EUR, Currency.EUR);
    }

    @ParameterizedTest( name = "[{index}] from={0} to={1}")
    @MethodSource("invalidQueryParams")
    void whenInvalidQueryParameters_thenStatusBadRequestAndReturnProblemDetail(Object from, Object to) throws Exception {
        // Default Problem Detail handled by Spring

        MockHttpServletRequestBuilder request = get("/api/rates").contentType(MediaType.APPLICATION_JSON);
		if (from != null) request = request.queryParam("source", from.toString());
		if (to != null) request = request.queryParam("target", to.toString());

        mockMvc
            .perform(request)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.instance", is("/api/rates")))
            .andExpect(jsonPath("$.title", is("Bad Request")))
            .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
            .andExpect(jsonPath("$.detail", not(emptyOrNullString())));

        verify(exchangeRatesService, never()).getRates(any(),any());
    }


    private static Stream<Arguments> invalidQueryParams() {
        List<Object> validFroms = List.of(Currency.USD);
        List<Object> validTos   = List.of(Currency.EUR);
        List<Object> invalidFroms = Arrays.asList(null, "null", "zzz", 1, "%s,%s".formatted(Currency.USD, Currency.EUR));
        List<Object> invalidTos   = Arrays.asList("null", "zzz", 1, "%s,%s".formatted(Currency.USD, Currency.EUR));

        Stream<Arguments> invalidFromWithValidTo = ArgumentCombinations.allCombinations(invalidFroms, validTos);
        Stream<Arguments> validFromWithInvalidTo = ArgumentCombinations.allCombinations(validFroms, invalidTos);

        return Stream.concat(invalidFromWithValidTo, validFromWithInvalidTo);
    }
}
