package com.miguelbf.exchangerateapi.client;

import com.miguelbf.exchangerateapi.config.ExchangeRatesClientProperties;
import com.miguelbf.exchangerateapi.config.RestClientConfig;
import com.miguelbf.exchangerateapi.exception.RatesUpstreamAPIException;
import com.miguelbf.exchangerateapi.model.clients.exchangerates.*;
import com.miguelbf.exchangerateapi.utilities.models.TestLiveRates;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.DefaultResponseCreator;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.ValueInstantiationException;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(
    components = ExchangeRatesClientService.class,
    includeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {RestClientConfig.class}
    )
)
class ExchangeRatesClientMockUpstreamAPITest {

    @Autowired
    private ExchangeRatesClientService exchangeRatesClientService;

    @Autowired
    private MockRestServiceServer server;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExchangeRatesClientProperties exchangeRatesClientProperties;

    @Test
    void givenNoTarget_whenGetLiveRates_thenCurrenciesParamIsAbsentAndSuccess() throws URISyntaxException {
        TestLiveRates testLiveRates = new TestLiveRates(
            1L, Currency.USD, Map.of(
            "USDEUR", new BigDecimal("0.85"),
            "USDGBP", new BigDecimal("0.74"),
            "USDJPY", new BigDecimal("158.74")
        ));
        testLiveRates.setTerms(new URI("https://tests.terms.com"));
        testLiveRates.setPrivacy(new URI("https://tests.privacy.com"));
        ExchangeRateApiSuccess<LiveRates> responsePayload = new ExchangeRateApiSuccess<>(true, testLiveRates);
        String jsonResponse = objectMapper.writeValueAsString(responsePayload);

        server.expect(once(), requestTo(startsWith(exchangeRatesClientProperties.getBaseUrl() + "/live")))
            .andExpect(method(HttpMethod.GET))
            .andExpect(queryParam("access_key", exchangeRatesClientProperties.getAccessKey()))
            .andExpect(queryParam("source", responsePayload.data().getSource().name()))
            .andExpect(request -> assertThat(request.getURI().getQuery()).doesNotContain("currencies"))
            .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        LiveRates result = exchangeRatesClientService.getLiveRates(responsePayload.data().getSource(), null);

        assertEquals(testLiveRates.getTerms(), result.getTerms());
        assertEquals(testLiveRates.getSource(), result.getSource());
        assertEquals(testLiveRates.getTimestamp(), result.getTimestamp());
        assertEquals(testLiveRates.getQuotes(), result.getQuotes());
        server.verify();
    }

    @Test
    void givenSuppliedTarget_whenGetLiveRates_thenCurrenciesParamIsPresentAndSuccess() throws URISyntaxException {
        Currency targetCurr = Currency.EUR;
        TestLiveRates testLiveRates = new TestLiveRates(
            1L, Currency.USD, Map.of(
            "USDEUR", new BigDecimal("0.85")
        ));
        testLiveRates.setTerms(new URI("https://tests.terms.com"));
        testLiveRates.setPrivacy(new URI("https://tests.privacy.com"));
        ExchangeRateApiSuccess<LiveRates> responsePayload = new ExchangeRateApiSuccess<>(true, testLiveRates);
        String jsonResponse = objectMapper.writeValueAsString(responsePayload);

        server.expect(once(), requestTo(startsWith(exchangeRatesClientProperties.getBaseUrl() + "/live")))
            .andExpect(method(HttpMethod.GET))
            .andExpect(queryParam("access_key", exchangeRatesClientProperties.getAccessKey()))
            .andExpect(queryParam("source", responsePayload.data().getSource().name()))
            .andExpect(queryParam("currencies", targetCurr.name()))
            .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        LiveRates result = exchangeRatesClientService.getLiveRates(responsePayload.data().getSource(), targetCurr);

        assertEquals(testLiveRates.getTerms(), result.getTerms());
        assertEquals(testLiveRates.getSource(), result.getSource());
        assertEquals(testLiveRates.getTimestamp(), result.getTimestamp());
        assertEquals(testLiveRates.getQuotes(), result.getQuotes());
        server.verify();
    }

    @ParameterizedTest
    @MethodSource("documentedErrorScenarios")
    void whenDocumentedUnsuccessfulRequest_thenThrowsRatesUpstreamAPIDocumentedHttpErrorException(
	    HttpStatus status, int code, String info, @Nullable String type
    ) {
        Currency sourceCurr = Currency.USD;
        Currency targetCurr = Currency.EUR;
        ErrorStatus errorStatus = new ErrorStatus(code, info, type);
        ExchangeRateApiError responsePayload = new ExchangeRateApiError(false, errorStatus);
        String jsonResponse = objectMapper.writeValueAsString(responsePayload);
        String url = exchangeRatesClientProperties.getBaseUrl() + "/live";

        server.expect(once(), requestTo(startsWith(url)))
            .andExpect(method(HttpMethod.GET))
            .andExpect(queryParam("access_key", exchangeRatesClientProperties.getAccessKey()))
            .andExpect(queryParam("source", sourceCurr.name()))
            .andExpect(queryParam("currencies", targetCurr.name()))
            .andRespond(withStatus(status).body(jsonResponse).contentType(MediaType.APPLICATION_JSON));

        RatesUpstreamAPIException.DocumentedHttpError ex = assertThrows(
            RatesUpstreamAPIException.DocumentedHttpError.class,
            () -> exchangeRatesClientService.getLiveRates(sourceCurr, targetCurr)
        );
        assertThat(ex.getUrl().toString()).contains(url);
        assertEquals(HttpMethod.GET, ex.getMethod());
        assertNotNull(ex.getMessage());
        assertInstanceOf(RestClientResponseException.class, ex.getCause());
        assertEquals(((RestClientResponseException) ex.getCause()).getResponseBodyAsString(), jsonResponse);
        server.verify();
    }

    @ParameterizedTest
    @MethodSource("undocumentedErrorScenarios")
    void whenUndocumentedUnsuccessfulRequest_thenThrowsRatesUpstreamAPIUnexpectedHttpErrorException(
        HttpStatus status, int code, String info, @Nullable String type
    ) {
        Currency sourceCurr = Currency.USD;
        Currency targetCurr = Currency.EUR;
        ErrorStatus errorStatus = new ErrorStatus(code, info, type);
        ExchangeRateApiError responsePayload = new ExchangeRateApiError(false, errorStatus);
        String jsonResponse = objectMapper.writeValueAsString(responsePayload);
        String url = exchangeRatesClientProperties.getBaseUrl() + "/live";

        server.expect(once(), requestTo(startsWith(url)))
            .andExpect(method(HttpMethod.GET))
            .andExpect(queryParam("access_key", exchangeRatesClientProperties.getAccessKey()))
            .andExpect(queryParam("source", sourceCurr.name()))
            .andExpect(queryParam("currencies", targetCurr.name()))
            .andRespond(withStatus(status).body(jsonResponse).contentType(MediaType.APPLICATION_JSON));

        RatesUpstreamAPIException.UnexpectedHttpError ex = assertThrows(
            RatesUpstreamAPIException.UnexpectedHttpError.class,
            () -> exchangeRatesClientService.getLiveRates(sourceCurr, targetCurr)
        );
        assertThat(ex.getUrl().toString()).contains(url);
        assertEquals(HttpMethod.GET, ex.getMethod());
        assertNotNull(ex.getMessage());
        assertInstanceOf(RestClientResponseException.class, ex.getCause());
        assertEquals(((RestClientResponseException) ex.getCause()).getResponseBodyAsString(), jsonResponse);
        server.verify();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"null"})
    void whenUpstreamReturnsEmptyResponse_thenThrowsRatesUpstreamAPIUnexpectedEmptyResponseException(
        String response
    ) {
        Currency sourceCurr = Currency.USD;
        Currency targetCurr = Currency.EUR;
        String url = exchangeRatesClientProperties.getBaseUrl() + "/live";
        DefaultResponseCreator responseCreator = withSuccess().contentType(MediaType.APPLICATION_JSON);
        if (response != null) {
            responseCreator = responseCreator.body(response);
        }

        server.expect(once(), requestTo(startsWith(url)))
            .andExpect(method(HttpMethod.GET))
            .andExpect(queryParam("access_key", exchangeRatesClientProperties.getAccessKey()))
            .andExpect(queryParam("source", sourceCurr.name()))
            .andExpect(queryParam("currencies", targetCurr.name()))
            .andRespond(responseCreator);

        RatesUpstreamAPIException.UnexpectedEmptyResponse ex = assertThrows(
            RatesUpstreamAPIException.UnexpectedEmptyResponse.class,
            () -> exchangeRatesClientService.getLiveRates(sourceCurr, targetCurr)
        );
        assertThat(ex.getUrl().toString()).contains(url);
        assertEquals(HttpMethod.GET, ex.getMethod());
        assertNotNull(ex.getMessage());
        assertInstanceOf(RestClientResponseException.class, ex.getCause());
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"invalid\":true}",
        "{\"invalid\":true",
        "<html>nope</html>"
    })
    void whenUpstreamReturnsSuccessInvalidResponse_thenThrowsRestClientException(String invalidResponse) {
        Currency sourceCurr = Currency.USD;
        Currency targetCurr = Currency.EUR;
        String url = exchangeRatesClientProperties.getBaseUrl() + "/live";

        server.expect(once(), requestTo(startsWith(url)))
            .andExpect(method(HttpMethod.GET))
            .andExpect(queryParam("access_key", exchangeRatesClientProperties.getAccessKey()))
            .andExpect(queryParam("source", sourceCurr.name()))
            .andExpect(queryParam("currencies", targetCurr.name()))
            .andRespond(withSuccess(invalidResponse, MediaType.APPLICATION_JSON));

        RestClientException ex = assertThrows(
            RestClientException.class,
            () -> exchangeRatesClientService.getLiveRates(sourceCurr, targetCurr)
        );
        assertInstanceOf(HttpMessageNotReadableException.class, ex.getCause());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"success\": true, \"timestamp\": 1699900000, \"source\": \"USD\", \"quotes\": {}}",
        "{\"success\": true, \"timestamp\": 1699900000, \"source\": \"USD\", \"quotes\":{\"EURGBP\": 0.85}}",
        "{\"success\": false, \"timestamp\": 1699900000, \"source\": \"USD\", \"quotes\":{\"USDEUR\": 0.85}}",
    })
    void whenUpstreamReturnsSuccessButMalformedQuotes_thenThrowsRestClientExceptionWrappingIllegalArgumentException(
        String invalidResponse
    ) {
        Currency sourceCurr = Currency.USD;
        String url = exchangeRatesClientProperties.getBaseUrl() + "/live";
        server.expect(once(), requestTo(startsWith(url)))
            .andExpect(method(HttpMethod.GET))
            .andExpect(queryParam("access_key", exchangeRatesClientProperties.getAccessKey()))
            .andExpect(queryParam("source", sourceCurr.name()))
            .andRespond(withSuccess(invalidResponse, MediaType.APPLICATION_JSON));

        RestClientException ex = assertThrows(
            RestClientException.class,
            () -> exchangeRatesClientService.getLiveRates(sourceCurr, null)
        );
        assertInstanceOf(HttpMessageNotReadableException.class, ex.getCause());
        assertInstanceOf(IllegalArgumentException.class, ex.getRootCause());
    }

    private static Stream<Arguments> documentedErrorScenarios() {
        return Stream.of(
            // Documented 400, 401, 429
            Arguments.of(
                HttpStatus.BAD_REQUEST,
                301,
                "User did not specify a date. [historical]",
                "invalid_source_currency"
            ),
            Arguments.of(
                HttpStatus.UNAUTHORIZED,
                101,
                "User did not supply an access key or supplied an invalid access key.",
                "missing_access_key"
            ),
            Arguments.of(
                HttpStatus.TOO_MANY_REQUESTS,
                104,
                "Your monthly usage limit has been reached. Please upgrade your subscription plan.",
                "too_many_requests_type"
            ),

            // Documented 5XX
            Arguments.of(
                HttpStatus.INTERNAL_SERVER_ERROR,
                500,
                "Internal server error.",
                "internal_server_error_type"
            ),
            Arguments.of(
                HttpStatus.SERVICE_UNAVAILABLE,
                1503,
                "Service unavailable.",
                null
            )
        );
    }

    private static Stream<Arguments> undocumentedErrorScenarios() {
        return Stream.of(
            // Not 200 or documented Error Scenario
            Arguments.of(
                HttpStatus.SEE_OTHER,
                1303,
                "Unexpected see other.",
                "see_other_type"
            ),
            Arguments.of(
                HttpStatus.UNPROCESSABLE_CONTENT,
                1422,
                "Unexpected unprocessable content.",
                null
            )
        );
    }

}
