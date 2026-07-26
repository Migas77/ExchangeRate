package com.miguelbf.exchangerateapi.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.miguelbf.exchangerateapi.model.clients.exchangerates.Currency;

import java.math.BigDecimal;
import java.util.Map;

public record RatesResponse (
	@JsonProperty(required = true) long timestamp,
    @JsonProperty(required = true) Currency source,
    @JsonProperty(required = true) Map<Currency, BigDecimal> rates
) {}
