package com.miguelbf.exchangerateapi.config.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.logging.logback.StructuredLogEncoder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

@NullMarked
public class MaskingStructuredLogEncoder extends StructuredLogEncoder {

    private final List<String> maskPatterns = new ArrayList<>();
    private List<Pattern> compiledPatterns = List.of();

    // Called once per <maskPattern> element found in the XML
    public void addMaskPattern(String pattern) {
        maskPatterns.add(pattern);
    }

    @Override
    public void start() {
        compiledPatterns = maskPatterns.stream().map(Pattern::compile).toList();
        super.start();
    }

    @Override
    public byte[] encode(ILoggingEvent event) {
        byte[] raw = super.encode(event);
        if (compiledPatterns.isEmpty()) {
            return raw;
        }
        String original = new String(raw, StandardCharsets.UTF_8);
        String masked = original;
        for (Pattern pattern : compiledPatterns) {
            masked = pattern.matcher(masked).replaceAll(MaskingStructuredLogEncoder::mask);
        }
        return masked.equals(original) ? raw : masked.getBytes(StandardCharsets.UTF_8);
    }

    private static String mask(MatchResult match) {
        String whole = match.group();
        char[] chars = whole.toCharArray();
        if (match.groupCount() >= 1 && match.group(1) != null) {
            int start = match.start(1) - match.start();
            int end = match.end(1) - match.start();
            Arrays.fill(chars, start, end, '*');
        } else {
            Arrays.fill(chars, '*');
        }
        return new String(chars);
    }

}