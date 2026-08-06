package com.miguelbf.exchangerateapi.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.miguelbf.exchangerateapi.model.clients.exchangerates.Currency;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Map;

public record RatesResponse(
    @Schema(description = "Unix timestamp of when rates were fetched", example = "1786031345")
    @JsonProperty(required = true) long timestamp,

    @Schema(description = "Base currency the rates are relative to", example = "AED")
    @JsonProperty(required = true) Currency source,

    @Schema(
        description = "Map of currency code to exchange rate value",
        example = """
            {
              "EUR": 0.236283,
              "USD": 0.272294
            }
            """
    )
    @JsonProperty(required = true) Map<Currency, BigDecimal> rates
) {
}
