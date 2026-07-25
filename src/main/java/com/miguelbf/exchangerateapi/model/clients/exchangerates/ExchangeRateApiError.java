package com.miguelbf.exchangerateapi.model.clients.exchangerates;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ExchangeRateApiError(
	@JsonProperty(required = true) boolean success,
	@JsonProperty(required = true) ErrorStatus error
) {
	public ExchangeRateApiError {
		if (success) {
			throw new IllegalArgumentException("ExchangeRateApiError expects success=false");
		}
	}
}
