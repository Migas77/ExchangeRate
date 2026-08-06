package com.miguelbf.exchangerateapi.service;

import com.miguelbf.exchangerateapi.model.clients.exchangerates.Currency;
import com.miguelbf.exchangerateapi.model.dto.ConversionResponse;

import java.math.BigDecimal;
import java.util.Set;

public interface IConversionService {

    ConversionResponse convertValue(BigDecimal amount, Currency source, Set<Currency> targets);

}
