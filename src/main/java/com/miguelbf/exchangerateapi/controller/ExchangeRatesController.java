package com.miguelbf.exchangerateapi.controller;


import com.miguelbf.exchangerateapi.exception.ProblemDetailsFactory;
import com.miguelbf.exchangerateapi.model.clients.exchangerates.Currency;
import com.miguelbf.exchangerateapi.model.dto.ConversionResponse;
import com.miguelbf.exchangerateapi.model.dto.RatesResponse;
import com.miguelbf.exchangerateapi.service.IConversionService;
import com.miguelbf.exchangerateapi.service.IExchangeRatesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Set;


@RestController
@RequestMapping(value = "/api")
@Tag(name = "Rates", description = "Provides value and currency exchange rates valid with up to a minute of delay")
@AllArgsConstructor
public class ExchangeRatesController {

    private final IExchangeRatesService exchangeRatesService;
    private final IConversionService conversionService;

    @Operation(
        summary = "Get currency exchange rates",
        description = """
            Returns exchange rates from the specified `source` currency. \
            By default, rates are returned against all supported currencies. You may optionally specify a
            `target` currency to restrict the response to a single target currency."""
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Latest, but **possibly cached** rates snapshot. Inspect `timestamp` to determine freshness of `rates`."
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request — Invalid request parameters.",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class),
                examples = {
                    @ExampleObject(
                        name = "Invalid Source Field",
                        value = """
                            {
                              "title": "Bad Request",
                              "status": 400,
                              "detail": "Failed to convert 'source' with value: 'null'",
                              "instance": "/api/rates"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Missing Source Field",
                        value = """
                            {
                              "title": "Bad Request",
                              "status": 400,
                              "detail": "Required parameter 'source' is not present.",
                              "instance": "/api/rates"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Matching Source and Target Currencies",
                        value = """
                            {
                              "title": "Bad Request",
                              "status": 400,
                              "detail": "Source and target currency must not match: EUR",
                              "instance": "/api/rates",
                              "type": "about:blank"
                            }
                            """
                    )
                }
            )
        )
    })
    @GetMapping("/rates")
    @ResponseStatus(HttpStatus.OK)
    public RatesResponse getExchangeRates(
        @Parameter(description = "Source (base) currency to which all `rates` are relative", required = true)
        @RequestParam(required = true) Currency source,

        @Parameter(description = "Target currency to which all `rates` are relative to. If omitted it will include exchange rates for all available currencies.")
        @RequestParam(required = false) @Nullable Currency target,

        HttpServletRequest request
    ) {
        if (source.equals(target)) {
            throw new ErrorResponseException(HttpStatus.BAD_REQUEST, ProblemDetailsFactory.of(HttpStatus.BAD_REQUEST,
                "Source and target currency must not match: %s".formatted(source), request), null);
        }

        return this.exchangeRatesService.getRates(source, target);
    }

    @Operation(
        summary = "Get numeric value conversion according to current exchange rates",
        description = """
            Converts the value `amount` from the `source` currency to one or multiple currencies. \
            By default, the converted amount and respective rates are returned against all supported currencies.
            You may optionally specify one or multiple `targets` currencies to restrict the response to the specified currencies."""
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Converted amounts based on the latest, but **possibly cached** rates snapshot. Inspect `timestamp` to determine freshness of `rates`."
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request — Invalid request parameters.",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class),
                examples = {
                    @ExampleObject(
                        name = "Invalid Amount Field",
                        value = """
                            {
                              "title": "Bad Request",
                              "status": 400,
                              "detail": "Failed to convert 'amount' with value: 'abc'",
                              "instance": "/api/conversions"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Amount Field Out of Allowed Range",
                        value = """
                            {
                              "title": "Bad Request",
                              "status": 400,
                              "detail": "amount: amount must be less than or equal to 1000000000, amount: amount must have at most 10 integer digit(s) and 6 fractional digit(s)",
                              "instance": "/api/conversions",
                              "type": "about:blank"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Missing Amount Field",
                        value = """
                            {
                              "title": "Bad Request",
                              "status": 400,
                              "detail": "Required parameter 'amount' is not present.",
                              "instance": "/api/conversions"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Matching Source and Target Currencies",
                        value = """
                            {
                              "title": "Bad Request",
                              "status": 400,
                              "detail": "Source and target currencies must not match: EUR",
                              "instance": "/api/conversions",
                              "type": "about:blank"
                            }
                            """
                    )
                }
            )
        )
    })
    @GetMapping("/conversions")
    @ResponseStatus(HttpStatus.OK)
    public ConversionResponse getValueConversions(
        @Parameter(description = "The numeric value to convert, expressed in the `source` currency.", required = true)
        @DecimalMin(value = "0.000001", message = "amount must be greater than or equal to 0.000001")
        @DecimalMax(value = "1000000000", message = "amount must be less than or equal to 1000000000")
        @Digits(integer = 10, fraction = 6, message = "amount must have at most {integer} integer digit(s) and {fraction} fractional digit(s)")
        @RequestParam(required = true) BigDecimal amount,

        @Parameter(description = "Source (base) currency of the supplied `amount` value. All conversion rates are calculated relative to this currency.", required = true)
        @RequestParam(required = true) Currency source,

        @Parameter(description = "Target currencies used for the supplied `amount` conversion. If omitted, the value `amount` will be converted to all supported currencies.")
        @RequestParam(required = false) @Nullable Set<Currency> targets,

        HttpServletRequest request
    ) {
        if (targets == null) {
            targets = Set.of();
        } else if (targets.contains(source)) {
            throw new ErrorResponseException(HttpStatus.BAD_REQUEST, ProblemDetailsFactory.of(HttpStatus.BAD_REQUEST,
                "Source and target currencies must not match: %s".formatted(source), request), null);
        }
        return this.conversionService.convertValue(amount, source, targets);
    }

}
