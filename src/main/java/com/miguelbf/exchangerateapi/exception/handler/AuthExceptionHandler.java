package com.miguelbf.exchangerateapi.exception.handler;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.miguelbf.exchangerateapi.controller.AuthController;
import com.miguelbf.exchangerateapi.exception.ProblemDetails;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

import java.io.IOException;
import java.util.stream.Collectors;

@RestControllerAdvice(assignableTypes = AuthController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
@AllArgsConstructor
public class AuthExceptionHandler {

    private final AuthenticationEntryPoint authenticationEntryPoint;

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
        return ProblemDetails.of(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
            .collect(Collectors.joining(", "));
        if (message.isBlank()) {
            message = "Invalid request content.";
        }
        return ProblemDetails.of(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    protected ProblemDetail handledBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        log.atWarn().setMessage("Bad Credentials").setCause(ex).log();
        return ProblemDetails.of(HttpStatus.UNAUTHORIZED, "Invalid email or password.", request);
    }

    @ExceptionHandler({InvalidBearerTokenException.class, AuthenticationServiceException.class})
    protected void handleAuthenticationException(
        AuthenticationException ex, HttpServletRequest request, HttpServletResponse response
    ) throws IOException, ServletException {
        /*
         * Exceptions from manual refresh-token validation in /api/auth/refresh are delegated directly
         * to the same {@link org.springframework.security.web.access.ExceptionTranslationFilter}
         * AuthenticationEntryPoint, producing the RFC 6750 401 + WWW-Authenticate response inline.
         */
        authenticationEntryPoint.commence(request, response, ex);
    }

}
