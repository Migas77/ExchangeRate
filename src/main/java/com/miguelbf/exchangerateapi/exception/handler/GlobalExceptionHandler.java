package com.miguelbf.exchangerateapi.exception.handler;

import com.miguelbf.exchangerateapi.exception.ProblemDetails;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.io.IOException;

@Slf4j
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
@AllArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final AccessDeniedHandler accessDeniedHandler;

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
        String detail = "An unexpected error occurred. Please try again later.";
        return ProblemDetails.of(httpStatus, detail, request);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public void handleAuthorizationDenied(
        AuthorizationDeniedException ex, HttpServletRequest request, HttpServletResponse response
    ) throws IOException, ServletException {
        /*
         * For some reason without this exception handler
         * the default {@link org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler}
         * configured in {@link com.miguelbf.exchangerateapi.config.security.SecurityConfig}
         * isn't triggered (triggering 500 INTERNAL_SERVER_ERROR instead).
         */
        accessDeniedHandler.handle(request, response, ex);
    }

}
