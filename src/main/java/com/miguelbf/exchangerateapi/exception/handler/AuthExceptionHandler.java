package com.miguelbf.exchangerateapi.exception.handler;

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
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;

@RestControllerAdvice(assignableTypes = AuthController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
@AllArgsConstructor
public class AuthExceptionHandler {

    private final AuthenticationEntryPoint authenticationEntryPoint;

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
