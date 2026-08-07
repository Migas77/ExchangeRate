package com.miguelbf.exchangerateapi.exception;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.miguelbf.exchangerateapi.config.security.AuthConfig;
import com.miguelbf.exchangerateapi.exception.exception.RatesUpstreamAPIException;
import com.miguelbf.exchangerateapi.exception.exception.RatesUpstreamDataException;
import com.miguelbf.exchangerateapi.exception.handler.UpstreamExceptionHandler;
import com.miguelbf.exchangerateapi.model.clients.exchangerates.Currency;
import com.miguelbf.exchangerateapi.model.clients.exchangerates.LiveRates;
import com.miguelbf.exchangerateapi.model.dto.RatesResponse;
import com.miguelbf.exchangerateapi.utilities.LoggingEvents;
import com.miguelbf.exchangerateapi.utilities.stubs.StubController;
import com.miguelbf.exchangerateapi.utilities.stubs.StubService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StubController.class)
@Import(AuthConfig.class)
class UpstreamExceptionHandlerMockExceptionsTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StubService stubService;

    private static final Logger logger = (Logger) LoggerFactory.getLogger(UpstreamExceptionHandler.class);
    private ListAppender<ILoggingEvent> logAppender;
    private ByteArrayOutputStream logOutput;

    @BeforeEach
    void attachLogAppender() {
        this.logOutput = LoggingEvents.getLoggerByteOutputStream("CONSOLE");
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void detachLogAppender() {
        logger.detachAppender(logAppender);
        logAppender.stop();
    }

    @Test
    void whenControllerLoaded_thenUpstreamExceptionHandlerIsPresent() {
        assertDoesNotThrow(() -> context.getBean(UpstreamExceptionHandler.class));
    }

    @Test
    void whenSocketTimeoutException_thenGatewayTimeout() throws Exception {
        String detailMessage = "The upstream service did not respond in time. Please try again later.";
        SocketTimeoutException cause = new SocketTimeoutException("read timed out");
        doThrow(new ResourceAccessException("timeout", cause)).when(stubService).call();

        mockMvc
            .perform(get("/stub"))
            .andExpect(status().isGatewayTimeout())
            .andExpect(jsonPath("$.instance", is("/stub")))
            .andExpect(jsonPath("$.title", is("Gateway Timeout")))
            .andExpect(jsonPath("$.status", is(HttpStatus.GATEWAY_TIMEOUT.value())))
            .andExpect(jsonPath("$.detail", is(detailMessage)));

        ILoggingEvent event = logAppender.list.getFirst();
        Throwable throwable = LoggingEvents.getThrowable(event);
        assertEquals(1, logAppender.list.size(), "Expected exactly one log event");
        assertEquals(Level.WARN, event.getLevel());
        assertEquals("Upstream timeout", event.getMessage());
        assertInstanceOf(ResourceAccessException.class, throwable);
        verify(stubService, times(1)).call();
    }

    @ParameterizedTest
    @MethodSource("unreachableCausesAndMessages")
    void whenUnreachableException_thenBadGateway(IOException cause, String message) throws Exception {
        String detailMessage = "The upstream service is currently unreachable. Please try again later.";
        doThrow(new ResourceAccessException("unreachable", cause)).when(stubService).call();

        mockMvc
            .perform(get("/stub"))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.instance", is("/stub")))
            .andExpect(jsonPath("$.title", is("Bad Gateway")))
            .andExpect(jsonPath("$.status", is(HttpStatus.BAD_GATEWAY.value())))
            .andExpect(jsonPath("$.detail", is(detailMessage)));

        ILoggingEvent event = logAppender.list.getFirst();
        Throwable throwable = LoggingEvents.getThrowable(event);
        assertEquals(1, logAppender.list.size(), "Expected exactly one log event");
        assertEquals(Level.WARN, event.getLevel());
        assertEquals(message, event.getMessage());
        assertInstanceOf(ResourceAccessException.class, throwable);
        verify(stubService, times(1)).call();
    }

    @Test
    void whenResourceAccessExceptionUnknownCause_thenInternalServerError() throws Exception {
        String detailMessage = "An unexpected error occurred. Please try again later.";
        IOException cause = new IOException("some io error");
        ResourceAccessException exception = new ResourceAccessException("unknown", cause);
        doThrow(exception).when(stubService).call();

        mockMvc
            .perform(get("/stub"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.instance", is("/stub")))
            .andExpect(jsonPath("$.title", is("Internal Server Error")))
            .andExpect(jsonPath("$.status", is(HttpStatus.INTERNAL_SERVER_ERROR.value())))
            .andExpect(jsonPath("$.detail", is(detailMessage)));

        ILoggingEvent event = logAppender.list.getFirst();
        Throwable throwable = LoggingEvents.getThrowable(event);
        assertEquals(1, logAppender.list.size(), "Expected exactly one log event");
        assertEquals(Level.ERROR, event.getLevel());
        assertEquals("Unexpected ResourceAccessException", event.getMessage());
        assertInstanceOf(ResourceAccessException.class, throwable);
        verify(stubService, times(1)).call();
    }

    @Test
    void whenRestClientException_thenBadGateway() throws Exception {
        RestClientException exception = new RestClientException("no suitable HttpMessageConverter found");
        String detailMessage = "The upstream service returned an invalid response. Please try again later.";
        doThrow(exception).when(stubService).call();

        mockMvc
            .perform(get("/stub"))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.instance", is("/stub")))
            .andExpect(jsonPath("$.title", is("Bad Gateway")))
            .andExpect(jsonPath("$.status", is(HttpStatus.BAD_GATEWAY.value())))
            .andExpect(jsonPath("$.detail", is(detailMessage)));

        ILoggingEvent event = logAppender.list.getFirst();
        Throwable throwable = LoggingEvents.getThrowable(event);
        assertEquals(1, logAppender.list.size(), "Expected exactly one log event");
        assertEquals(Level.ERROR, event.getLevel());
        assertEquals("Base RestClientException", event.getMessage());
        assertInstanceOf(exception.getClass(), throwable);
        verify(stubService, times(1)).call();
    }

    @ParameterizedTest
    @MethodSource("ratesUpstreamAPIExceptionsAndMessages")
    void whenRatesUpstreamApiException_thenBadGateway(
        RatesUpstreamAPIException exception, String message, Map<String, Object> expectedUpstreamFields
    ) throws Exception {
        String detailMessage = "The upstream service returned an invalid response. Please try again later.";
        doThrow(exception).when(stubService).call();

        mockMvc
            .perform(get("/stub"))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.instance", is("/stub")))
            .andExpect(jsonPath("$.title", is("Bad Gateway")))
            .andExpect(jsonPath("$.status", is(HttpStatus.BAD_GATEWAY.value())))
            .andExpect(jsonPath("$.detail", is(detailMessage)));

        ILoggingEvent event = logAppender.list.getFirst();
        Throwable throwable = LoggingEvents.getThrowable(event);
        assertEquals(1, logAppender.list.size(), "Expected exactly one log event");
        assertEquals(Level.ERROR, event.getLevel());
        assertEquals(message, event.getMessage());
        assertEquals(
            objectMapper.writeValueAsString(exception.getUrl().toString()),
            LoggingEvents.getKeyLogAsString(this.logOutput, objectMapper, "url")
        );
        assertEquals(
            objectMapper.writeValueAsString(exception.getMethod().toString()),
            LoggingEvents.getKeyLogAsString(this.logOutput, objectMapper, "method")
        );
        List<String> upstreamLogKeys = List.of(
            "upstream_error_code", "upstream_error_info", "upstream_error_type", "upstream_error_raw_body"
        );
        for (String key : upstreamLogKeys) {
            Object expectedValue = expectedUpstreamFields.get(key);
            assertEquals(
                expectedValue == null ? null : objectMapper.writeValueAsString(expectedValue),
                LoggingEvents.getKeyLogAsString(this.logOutput, objectMapper, key)
            );
        }
        assertInstanceOf(exception.getClass(), throwable);
        assertNotNull(exception.getCause());
        assertInstanceOf(exception.getCause().getClass(), throwable.getCause());
        verify(stubService, times(1)).call();
    }

    @ParameterizedTest
    @MethodSource("ratesUpstreamDataExceptionsAndMessages")
    void whenRatesUpstreamDataException_thenBadGateway(
        RatesUpstreamDataException exception, String message
    ) throws Exception {
        String detailMessage = "The upstream service returned incoherent domain data. Please try again later.";
        doThrow(exception).when(stubService).call();

        mockMvc
            .perform(get("/stub"))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.instance", is("/stub")))
            .andExpect(jsonPath("$.title", is("Bad Gateway")))
            .andExpect(jsonPath("$.status", is(HttpStatus.BAD_GATEWAY.value())))
            .andExpect(jsonPath("$.detail", is(detailMessage)));

        ILoggingEvent event = logAppender.list.getFirst();
        Throwable throwable = LoggingEvents.getThrowable(event);
        assertEquals(1, logAppender.list.size(), "Expected exactly one log event");
        assertEquals(Level.ERROR, event.getLevel());
        assertEquals(message, event.getMessage());

        assertEquals(
            objectMapper.writeValueAsString(exception.getSource()),
            LoggingEvents.getKeyLogAsString(this.logOutput, objectMapper, "req_source")
        );
        assertEquals(
            objectMapper.writeValueAsString(exception.getTarget()),
            LoggingEvents.getKeyLogAsString(this.logOutput, objectMapper, "req_target")
        );
        assertEquals(
            objectMapper.writeValueAsString(exception.getActSource()),
            LoggingEvents.getKeyLogAsString(this.logOutput, objectMapper, "act_source")
        );
        assertEquals(
            objectMapper.writeValueAsString(exception.getActQuotes()),
            LoggingEvents.getKeyLogAsString(this.logOutput, objectMapper, "act_quotes")
        );
        assertInstanceOf(exception.getClass(), throwable);
        verify(stubService, times(1)).call();
    }


    private static Stream<Arguments> unreachableCausesAndMessages() {
        return Stream.of(
            Arguments.of(new ConnectException("connection refused"), "Upstream connection refused"),
            Arguments.of(new UnknownHostException("unknown host"), "Upstream unreachable, possible misconfiguration"),
            Arguments.of(new NoRouteToHostException("no route"), "Upstream unreachable, possible misconfiguration")
        );
    }

    private static Stream<Arguments> ratesUpstreamAPIExceptionsAndMessages() throws URISyntaxException {
        String documentedBody = """
            {"success":false,"error":{"code":101,"info":"Invalid access key.","type":"invalid_access_key"}}""";
        String undocumentedBody = """
            {"success":false,"error":{"code":1422,"info":"Unexpected unprocessable content."}}""";
        String htmlBody = "<html><body>upstream is down</body></html>";

        return Stream.of(
            Arguments.of(
                new RatesUpstreamAPIException.DocumentedHttpError(
                    "Documented error", buildUpstreamError(HttpStatus.UNAUTHORIZED, documentedBody),
                    new URI("https://placeholder.com"), HttpMethod.GET
                ), "Upstream returned documented http error",
                Map.of(
                    "upstream_error_code", 101,
                    "upstream_error_info", "Invalid access key.",
                    "upstream_error_type", "invalid_access_key"
                )
            ),
            Arguments.of(
                new RatesUpstreamAPIException.UnexpectedHttpError(
                    "Unexpected error", buildUpstreamError(HttpStatus.UNPROCESSABLE_CONTENT, undocumentedBody),
                    new URI("https://placeholder.com"), HttpMethod.POST
                ), "Upstream returned unexpected http error",
                Map.of(
                    "upstream_error_code", 1422,
                    "upstream_error_info", "Unexpected unprocessable content.",
                    "upstream_error_type", ""
                )
            ),
            Arguments.of(
                // body does not map to ExchangeRateApiError, so it is logged as is
                new RatesUpstreamAPIException.UnexpectedHttpError(
                    "Unexpected error with unparseable body", buildUpstreamError(HttpStatus.BAD_GATEWAY, htmlBody),
                    new URI("https://placeholder.com"), HttpMethod.POST
                ), "Upstream returned unexpected http error",
                Map.of("upstream_error_raw_body", htmlBody)
            ),
            Arguments.of(
                new RatesUpstreamAPIException.UnexpectedEmptyResponse(
                    "Unexpected empty response", new RuntimeException("cause 4"),
                    new URI("https://placeholder.com"), HttpMethod.PUT
                ), "Upstream returned unexpected empty response body",
                Map.of()
            )
        );
    }

    private static Stream<Arguments> ratesUpstreamDataExceptionsAndMessages() {
        // This payloads wouldn't ctually trigger any error
        return Stream.of(
            Arguments.of(
                new RatesUpstreamDataException.UnexpectedSource(
                    "Unexpected source", Currency.AED, null,
                    new LiveRates(1L, Currency.AED, Map.of("AEDUSD", new BigDecimal("0.27")))
                ), "Upstream returned unexpected source currency"
            ),
            Arguments.of(
                new RatesUpstreamDataException.UnexpectedTarget(
                    "Unexpected target", Currency.USD, Currency.AED,
                    new LiveRates(2L, Currency.USD, Map.of("USDAED", new BigDecimal("3.67")))
                ), "Upstream returned unexpected target currency"
            ),
            Arguments.of(
                new RatesUpstreamDataException.UnexpectedQuoteCount(
                    "Unexpected quote count", Currency.EUR, Currency.GBP,
                    new LiveRates(3L, Currency.EUR, Map.of(
                        "EURGBP", new BigDecimal("0.86"),
                        "EURUSD", new BigDecimal("1.14")
                    ))
                ), "Upstream returned unexpected quote count"
            ),
            Arguments.of(
                new RatesUpstreamDataException.MissingTarget(
                    "Missing target", Currency.EUR, Currency.GBP,
                    new RatesResponse(4L, Currency.EUR, Map.of(Currency.USD, new BigDecimal("1.14")))
                ), "Upstream response missing target currency"
            )
        );
    }

    private static RestClientResponseException buildUpstreamError(HttpStatus status, String body) {
        ObjectMapper mapper = new ObjectMapper();
        RestClientResponseException exception = new RestClientResponseException(
            status.toString(), status, status.getReasonPhrase(), null,
            body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8
        );
        exception.setBodyConvertFunction(targetType ->
            mapper.readValue(body, mapper.constructType(targetType.getType())));
        return exception;
    }

}
