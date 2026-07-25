package com.miguelbf.exchangerateapi.model.clients.exchangerates;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

public record ErrorStatus(
	@JsonProperty(required = true) int code,
	@JsonProperty(required = true) String info,
	@JsonProperty @Nullable String type
) {
	// property type is not documented in the api docs, but it is included here as nullable,
	// since it has been observed in actual responses
}
