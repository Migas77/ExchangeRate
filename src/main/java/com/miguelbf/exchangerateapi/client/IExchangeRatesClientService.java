package com.miguelbf.exchangerateapi.client;

import com.miguelbf.exchangerateapi.model.clients.exchangerates.Currency;
import com.miguelbf.exchangerateapi.model.clients.exchangerates.LiveRates;
import org.jspecify.annotations.Nullable;

public interface IExchangeRatesClientService {

    LiveRates getLiveRates(Currency source, @Nullable Currency target);

}
