package com.miguelbf.exchangerateapi.utilities.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.miguelbf.exchangerateapi.model.clients.exchangerates.Currency;
import com.miguelbf.exchangerateapi.model.clients.exchangerates.LiveRates;

import java.math.BigDecimal;
import java.util.Map;

public class TestLiveRates extends LiveRates {

    @JsonIgnore
    private final Map<String, BigDecimal> rawQuotes;

    @JsonCreator
    public TestLiveRates(
        @JsonProperty(value = "timestamp", required = true) long timestamp,
        @JsonProperty(value = "source", required = true) Currency source,
        @JsonProperty(value = "quotes", required = true) Map<String, BigDecimal> rawQuotes
    ) {
        super(timestamp, source, rawQuotes);
        this.rawQuotes = rawQuotes;
    }

    // Override serialization so "quotes" writes the raw map instead of the transformed one
    @JsonProperty("quotes")
    public Map<String, BigDecimal> getRawQuotes() {
        return rawQuotes;
    }

}
