package com.miguelbf.exchangerateapi.exception.handler;

import com.miguelbf.exchangerateapi.exception.ProblemDetails;
import com.miguelbf.exchangerateapi.exception.exception.RatesUpstreamAPIException;
import com.miguelbf.exchangerateapi.exception.exception.RatesUpstreamDataException;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@ApiResponses(value = {
    @ApiResponse(
        responseCode = "502",
        description = "Bad Gateway.",
        content = @Content(
            schema = @Schema(implementation = ProblemDetail.class),
            mediaType = "application/problem+json",
            examples = {
                @ExampleObject(
                    name = "Upstream Unreachable",
                    description = "The upstream service could not be reached (e.g. connection refused).",
                    value = """
                        {
                          "type": "about:blank",
                          "title": "Bad Gateway",
                          "status": 502,
                          "detail": "The upstream service is currently unreachable. Please try again later.",
                          "instance": "/api/rates"
                        }
                        """
                ),
                @ExampleObject(
                    name = "Upstream Invalid response",
                    description = "The upstream service returned an unexpected or malformed HTTP response.",
                    value = """
                        {
                          "type": "about:blank",
                          "title": "Bad Gateway",
                          "status": 502,
                          "detail": "The upstream service returned an invalid response. Please try again later.",
                          "instance": "/api/rates"
                        }
                        """
                ),
                @ExampleObject(
                    name = "Upstream Incoherent Data",
                    description = "The upstream service returned domain data that is inconsistent with the request.",
                    value = """
                        {
                          "type": "about:blank",
                          "title": "Bad Gateway",
                          "status": 502,
                          "detail": "The upstream service returned incoherent domain data. Please try again later.",
                          "instance": "/api/rates"
                        }
                        """
                )
            }
        )
    ),
    @ApiResponse(
        responseCode = "504",
        description = "Gateway Timeout.",
        content = @Content(
            schema = @Schema(implementation = ProblemDetail.class),
            mediaType = "application/problem+json",
            examples = @ExampleObject(
                name = "Upstream Service Timeout",
                description = "The upstream service did not respond within the configured timeout.",
                value = """
                    {
                      "type": "about:blank",
                      "title": "Gateway Timeout",
                      "status": 504,
                      "detail": "The upstream service did not respond in time. Please try again later.",
                      "instance": "/api/rates"
                    }
                    """
            )
        )
    )
})
public class UpstreamExceptionHandler {


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
    public ProblemDetail handleRatesUpstreamApiException(
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
            case RatesUpstreamDataException.MissingTarget missingTarget -> "Upstream response missing target currency";
        };
        log.atError().setMessage(message).setCause(ex).log();
        return ProblemDetails.of(httpStatus, detail, request);
    }

}
