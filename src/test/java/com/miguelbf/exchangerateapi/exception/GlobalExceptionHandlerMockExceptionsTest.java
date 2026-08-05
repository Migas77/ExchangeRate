package com.miguelbf.exchangerateapi.exception;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.miguelbf.exchangerateapi.config.security.AuthConfig;
import com.miguelbf.exchangerateapi.exception.handler.GlobalExceptionHandler;
import com.miguelbf.exchangerateapi.utilities.stubs.StubController;
import com.miguelbf.exchangerateapi.utilities.stubs.StubService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StubController.class)
@Import(AuthConfig.class)
class GlobalExceptionHandlerMockExceptionsTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StubService stubService;

    private static final Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void attachLogAppender() {
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void detachLogAppender() {
        logger.detachAppender(logAppender);
    }

    @Test
    void whenControllerLoaded_thenGlobalExceptionHandlerIsPresent() {
        assertDoesNotThrow(() -> context.getBean(GlobalExceptionHandler.class));
    }

    @Test
    void whenUnexpectedException_thenStatusInternalServerError() throws Exception {
        String detailMessage = "An unexpected error occurred. Please try again later.";
        Exception exception = new RuntimeException("Unexpected error");
        doThrow(exception).when(stubService).call();

        mockMvc
            .perform(get("/stub"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.instance", is("/stub")))
            .andExpect(jsonPath("$.title", is("Internal Server Error")))
            .andExpect(jsonPath("$.status", is(HttpStatus.INTERNAL_SERVER_ERROR.value())))
            .andExpect(jsonPath("$.detail", is(detailMessage)));

        assertEquals(1, logAppender.list.size(), "Expected exactly one log event");
        ILoggingEvent event = logAppender.list.getFirst();
        assertEquals(Level.ERROR, event.getLevel());
        assertEquals("Unhandled exception", event.getMessage());
        assertEquals(exception.getClass().getName(), event.getThrowableProxy().getClassName());
        assertEquals(exception.getMessage(), event.getThrowableProxy().getMessage());
        verify(stubService, times(1)).call();
    }

}
