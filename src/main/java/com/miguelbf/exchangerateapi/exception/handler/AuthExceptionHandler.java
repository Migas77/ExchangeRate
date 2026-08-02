package com.miguelbf.exchangerateapi.exception.handler;

import com.miguelbf.exchangerateapi.controller.AuthController;
import com.miguelbf.exchangerateapi.exception.ProblemDetails;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.util.stream.Collectors;

@RestControllerAdvice(assignableTypes = AuthController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class AuthExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    protected ProblemDetail handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String message = "Failed to read request.";
        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException ife) {
            String fieldName = ife.getPath().isEmpty() ? "unknown" :
                ife.getPath().getLast().getPropertyName();
            message = String.format("Invalid value '%s' for field '%s'.", ife.getValue(), fieldName);
        } else if (cause instanceof MismatchedInputException mie) {
            String fieldName = mie.getPath().isEmpty() ? "unknown" :
                mie.getPath().getLast().getPropertyName();
            message = String.format("Missing or invalid value for field '%s'.", fieldName);
        }
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

    @ApiResponse(
        responseCode = "401",
        description = "Invalid email or password.",
        content = @Content(
            mediaType = "application/problem+json",
            schema = @Schema(implementation = ProblemDetail.class),
            examples = @ExampleObject(
                name = "Invalid Credentials",
                value = """
                    {
                      "type": "about:blank",
                      "title": "Unauthorized",
                      "status": 401,
                      "detail": "Invalid email or password.",
                      "instance": "/api/auth/login"
                    }
                    """
            )
        )
    )
    @ExceptionHandler(BadCredentialsException.class)
    protected ProblemDetail handledBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        log.atWarn().setMessage("Bad Credentials").setCause(ex).log();
        return ProblemDetails.of(HttpStatus.UNAUTHORIZED, "Invalid email or password.", request);
    }

    @ExceptionHandler({InvalidBearerTokenException.class, AuthenticationServiceException.class})
    protected ProblemDetail handleAuthenticationException(
        AuthenticationException ex, HttpServletRequest request
    ) {
        // These exceptions can be thrown when manually validating refresh access token in /api/auth/refresh endpoint
        // These AuthenticationException subtypes will be rethrown so that ExceptionTranslationFilter
        // (not this handler) produces the RFC 6750 401 + WWW-Authenticate response.
        throw ex;
    }

}
