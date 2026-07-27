package com.miguelbf.exchangerateapi.exception.handler;

import com.miguelbf.exchangerateapi.exception.ProblemDetails;
import com.miguelbf.exchangerateapi.exception.exception.RatesUpstreamAPIException;
import com.miguelbf.exchangerateapi.exception.exception.RatesUpstreamDataException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.*;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UpstreamExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceAccessException.class)
    public ProblemDetail handleResourceAccessException(
        ResourceAccessException ex, HttpServletRequest request
    ) {
        String detail;
        HttpStatus httpStatus;
        Throwable cause = ex.getCause();
        if (cause instanceof SocketTimeoutException) {
            httpStatus = HttpStatus.GATEWAY_TIMEOUT;
            detail = "The upstream service did not respond in time. Please try again later.";
            log.atWarn().setMessage("Upstream timeout").setCause(ex).log();
        } else if (cause instanceof ConnectException) {
            httpStatus = HttpStatus.BAD_GATEWAY;
            detail = "The upstream service is currently unreachable. Please try again later.";
            log.atWarn().setMessage("Upstream connection refused").setCause(ex).log();
        } else if (
            cause instanceof UnknownHostException
                || cause instanceof NoRouteToHostException
        ) {
            httpStatus = HttpStatus.BAD_GATEWAY;
            detail = "The upstream service is currently unreachable. Please try again later.";
            log.atWarn().setMessage("Upstream unreachable, possible misconfiguration").setCause(ex).log();
        } else {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            detail = "An unexpected error occurred. Please try again later.";
            log.atError().setMessage("Unexpected ResourceAccessException").setCause(ex).log();
        }
        return ProblemDetails.of(httpStatus, detail, request);
    }

    @ExceptionHandler(RatesUpstreamAPIException.class)
    public ProblemDetail handleRatesUpstreamBadGatewayException(
        RatesUpstreamAPIException ex, HttpServletRequest request
    ) {
        HttpStatus httpStatus = HttpStatus.BAD_GATEWAY;
        String detail = "The upstream service returned an invalid response. Please try again later.";
        String message = switch (ex) {
            case RatesUpstreamAPIException.DocumentedHttpError documentedHttpError ->
                "Upstream returned documented http error";
            case RatesUpstreamAPIException.UnexpectedHttpError unexpectedHttpError ->
                "Upstream returned unexpected http error";
            case RatesUpstreamAPIException.UnexpectedEmptyResponse unexpectedEmptyResponse ->
                "Upstream returned unexpected empty response body";
        };
        log.atError().setMessage(message).setCause(ex).log();
        return ProblemDetails.of(httpStatus, detail, request);
    }

    @ExceptionHandler(RatesUpstreamDataException.class)
    public ProblemDetail handleRatesUpstreamDataException(
        RatesUpstreamDataException ex, HttpServletRequest request
    ) {
        HttpStatus httpStatus = HttpStatus.BAD_GATEWAY;
        String detail = "The upstream service returned incoherent domain data. Please try again later.";
        String message = switch (ex) {
            case RatesUpstreamDataException.UnexpectedSource unexpectedSource ->
                "Upstream returned unexpected source currency";
            case RatesUpstreamDataException.UnexpectedTarget unexpectedTarget ->
                "Upstream returned unexpected target currency";
            case RatesUpstreamDataException.UnexpectedQuoteCount unexpectedQuoteCount ->
                "Upstream returned unexpected quote count";
        };
        log.atError().setMessage(message).setCause(ex).log();
        return ProblemDetails.of(httpStatus, detail, request);
    }

}
