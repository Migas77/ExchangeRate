package com.miguelbf.exchangerateapi.exception.handler;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.miguelbf.exchangerateapi.exception.ProblemDetailsFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

import java.util.stream.Collectors;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ValidationExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    protected ProblemDetail handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String message;
        Throwable cause = ex.getCause();
        message = switch (cause) {
            case null -> "Request body is missing or empty.";
            case UnrecognizedPropertyException upe -> String.format("Unrecognized field '%s'.", upe.getPropertyName());
            case InvalidFormatException ife -> {
                String fieldName = ife.getPath().isEmpty() ? "unknown" : ife.getPath().getLast().getPropertyName();
                yield String.format("Invalid value '%s' for field '%s'.", ife.getValue(), fieldName);
            }
            case InvalidDefinitionException invalidDefinitionException ->
                "Request could not be mapped to the expected structure.";
            case MismatchedInputException mie -> {
                String fieldName = mie.getPath().isEmpty() ? "unknown" : mie.getPath().getLast().getPropertyName();
                yield String.format("Missing or invalid value for field '%s'.", fieldName);
            }
            case StreamReadException streamReadException -> "Malformed JSON in request body.";
            case JsonMappingException jsonMappingException -> "Request could not be mapped to the expected structure.";
            default -> "Failed to read request.";
        };
        return ProblemDetailsFactory.of(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
            .collect(Collectors.joining(", "));
        if (message.isBlank()) {
            message = "Invalid request content.";
        }
        return ProblemDetailsFactory.of(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleHandlerMethodValidationException(HandlerMethodValidationException ex, HttpServletRequest request) {
        String message = ex.getParameterValidationResults().stream()
            .flatMap(result -> result.getResolvableErrors().stream()
                .map(error -> result.getMethodParameter().getParameterName() + ": " + error.getDefaultMessage()))
            .collect(Collectors.joining(", "));
        if (message.isBlank()) {
            message = "Validation Failure. Invalid request parameters.";
        }

        return ProblemDetailsFactory.of(HttpStatus.BAD_REQUEST, message, request);
    }

}
