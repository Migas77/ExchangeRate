package com.miguelbf.exchangerateapi.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.miguelbf.exchangerateapi.model.clients.exchangerates.Currency;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Map;

public record RatesResponse(
    @Schema(description = "Unix timestamp of when rates were fetched", example = "1785667963")
    @JsonProperty(required = true) long timestamp,

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
    @JsonProperty(required = true) Map<Currency, BigDecimal> rates
) {
}
