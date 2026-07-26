package com.miguelbf.exchangerateapi.service;

import com.miguelbf.exchangerateapi.model.clients.exchangerates.Currency;
import com.miguelbf.exchangerateapi.model.dto.RatesResponse;
import org.jspecify.annotations.Nullable;


public interface IExchangeRatesService {

	RatesResponse getRates(Currency source, @Nullable Currency target);

}
