package com.miguelbf.exchangerateapi.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.miguelbf.exchangerateapi.model.clients.exchangerates.Currency;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Map;

public record ConversionResponse(

    @Schema(description = "Unix timestamp of when rates were fetched", example = "1785667963")
    @JsonProperty(required = true) long timestamp,

    @Schema(description = "The numerical amount that is converted, expressed in the `source` currency.")
    @JsonProperty(required = true) BigDecimal amount,

    @Schema(description = "Base currency the rates are relative to")
    @JsonProperty(required = true) Currency source,

    @Schema(
        description = "Map of currency code to exchange rate value",
        example = """
            {
              "USD": 0.27,
              "EUR": 0.25
            }
            """
    )
    @JsonProperty(required = true) Map<Currency, BigDecimal> rates,

    @Schema(
        description = "Map of currency code to converted value based on rate specified in `rates`",
        example = """
            {
              "USD": 27.0,
              "EUR": 25.0
            }
            """
    )
    @JsonProperty(required = true) Map<Currency, BigDecimal> convertedValues
) {
}
