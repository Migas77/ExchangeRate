package com.miguelbf.exchangerateapi.model.clients.exchangerates;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record ExchangeRateApiSuccess<T>(
	@JsonProperty(required = true) boolean success,
	@JsonProperty(required = true) @JsonUnwrapped T data
) {
	public ExchangeRateApiSuccess {
		if (!success) {
			throw new IllegalArgumentException("ExchangeRateApiSuccess expects success=true");
		}
	}
}
