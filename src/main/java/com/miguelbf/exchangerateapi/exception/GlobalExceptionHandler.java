package com.miguelbf.exchangerateapi.exception;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(Exception.class)
	@ApiResponse(
		responseCode = "500",
		description = "Internal Server Error.",
		content = @Content(
			schema = @Schema(implementation = ProblemDetail.class),
			mediaType = "application/problem+json",
			examples = @ExampleObject(
				value = """
					{
					  "type": "about:blank",
					  "title": "Internal Server Error",
					  "status": 500,
					  "detail": "An unexpected error occurred. Please try again later.",
					  "instance": "/api/rates"
					}
					"""
			)
		)
	)
    public ProblemDetail handleGeneric(Exception ex, HttpServletRequest request) {
        // Safeline method - Catch-all handler which prevents unhandled exceptions from leaking stack traces
        // although spring.web.error.include-stacktrace=never is set in application.properties
        log.atError().setMessage("Unhandled exception").setCause(ex).log();
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        ProblemDetail problemDetail = ProblemDetail.forStatus(httpStatus);
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setTitle(httpStatus.getReasonPhrase());
        problemDetail.setDetail("An unexpected error occurred. Please try again later.");
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        return problemDetail;
    }

}
