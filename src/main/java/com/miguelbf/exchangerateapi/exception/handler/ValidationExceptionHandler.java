package com.miguelbf.exchangerateapi.exception.handler;

import com.miguelbf.exchangerateapi.exception.ProblemDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.stream.Collectors;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ValidationExceptionHandler {

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleHandlerMethodValidationException(HandlerMethodValidationException ex, HttpServletRequest request) {
        String message = ex.getParameterValidationResults().stream()
            .flatMap(result -> result.getResolvableErrors().stream()
                .map(error -> result.getMethodParameter().getParameterName() + ": " + error.getDefaultMessage()))
            .collect(Collectors.joining(", "));
        if (message.isBlank()) {
            message = "Validation Failure. Invalid request parameters.";
        }

        return ProblemDetails.of(HttpStatus.BAD_REQUEST, message, request);
    }

}
