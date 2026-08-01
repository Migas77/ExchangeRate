package com.miguelbf.exchangerateapi.exception.handler;

import com.miguelbf.exchangerateapi.controller.AuthController;
import com.miguelbf.exchangerateapi.exception.ProblemDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.util.stream.Collectors;

@RestControllerAdvice(assignableTypes = AuthController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
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

}
