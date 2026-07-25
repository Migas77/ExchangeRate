package com.miguelbf.exchangerateapi.client;

import com.miguelbf.exchangerateapi.model.clients.exchangerates.LiveRates;
import com.miguelbf.exchangerateapi.model.clients.exchangerates.Currency;
import org.jspecify.annotations.Nullable;

public interface IExchangeRatesClientService {

    LiveRates getLiveRates(Currency from, @Nullable Currency to);

}
