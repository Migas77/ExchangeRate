package com.miguelbf.exchangerateapi.model.dto;

import com.miguelbf.exchangerateapi.model.clients.exchangerates.Currency;

import java.math.BigDecimal;
import java.util.Map;

public record RatesResponse (
    long timestamp,
    Currency source,
    Map<Currency, BigDecimal> rates
) {}
