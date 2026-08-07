package com.miguelbf.exchangerateapi.model.clients.exchangerates;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

public record ErrorStatus(
    @JsonProperty(required = true) int code,
    @JsonProperty(required = true) String info,
    @JsonProperty @JsonSetter(nulls = Nulls.AS_EMPTY) String type
) {
    // property type is not documented in the api docs, but it is included here as empty string,
    // since it has been observed in actual responses
}
