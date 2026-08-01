package com.miguelbf.exchangerateapi.logging;

import com.miguelbf.exchangerateapi.config.logging.MaskingStructuredLogEncoder;
import com.miguelbf.exchangerateapi.repository.UserRepository;
import com.miguelbf.exchangerateapi.utilities.LoggingEvents;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("no-redis")
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    FlywayAutoConfiguration.class
})
class MaskingLogTest {

    private static final String MASKED_CONSOLE_APPENDER = "CONSOLE";
    private static final Logger logger = LoggerFactory.getLogger(MaskingLogTest.class);

    private ByteArrayOutputStream logOutput;
    private MaskingStructuredLogEncoder maskingStructuredLogEncoder;

    @MockitoBean
    UserRepository userRepository;

    @BeforeEach
    void captureConsoleAppenderOutput() {
        this.logOutput = LoggingEvents.getLoggerByteOutputStream(MASKED_CONSOLE_APPENDER);
        this.maskingStructuredLogEncoder = LoggingEvents.getAppenderEncoder(
            MASKED_CONSOLE_APPENDER, MaskingStructuredLogEncoder.class
        );
    }

    static Stream<Arguments> maskingSamples() {
        // 1. pattern; 2. full example to be masked; 3. secret that should not be exposed (i.e. should be masked)
        return Stream.of(
            // groupless pattern
            Arguments.of(
                "secret-[a-z0-9]{8}",
                "rotated credentials to secret-a1b2c3d4",
                "secret-a1b2c3d4"
            ),
            // group pattern
            Arguments.of(
                "access_key=(.{32})",
                "GET https://tests.com/live?access_key=test0access0key0with32characters&base=EUR",
                "test0access0key0with32characters"
            )
        );
    }

    @Test
    void everyConfiguredPatternHasASample() {
        List<String> samples = maskingSamples().map(arguments -> (String) arguments.get()[0]).sorted().toList();
        assertEquals(configuredPatterns(this.maskingStructuredLogEncoder).stream().sorted().toList(), samples);
    }

    @ParameterizedTest
    @MethodSource("maskingSamples")
    void whenPatternsPresent_MaskThem(String pattern, String message, String secret) {
        assertTrue(configuredPatterns(this.maskingStructuredLogEncoder).contains(pattern));

        logger.info(message);

        String encoded = this.logOutput.toString(Charset.defaultCharset());
        assertFalse(encoded.contains(secret));
        assertTrue(encoded.contains("*".repeat(secret.length())));
    }

    @Test
    void whenPatternsAbsent_LeaveEventUntouched() {
        logger.info("nothing sensitive here");

        String encoded = this.logOutput.toString(Charset.defaultCharset());
        assertFalse(encoded.contains("*"));
        assertTrue(encoded.contains("nothing sensitive here"));
    }

    private List<String> configuredPatterns(MaskingStructuredLogEncoder logEncoder) {
        return logEncoder.getCompiledPatterns().stream().map(Pattern::pattern).toList();
    }

}