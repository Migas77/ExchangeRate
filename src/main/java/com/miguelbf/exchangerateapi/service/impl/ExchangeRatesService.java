package com.miguelbf.exchangerateapi.service.impl;

import com.miguelbf.exchangerateapi.client.IExchangeRatesClientService;
import com.miguelbf.exchangerateapi.exception.exception.RatesUpstreamDataException;
import com.miguelbf.exchangerateapi.model.clients.exchangerates.Currency;
import com.miguelbf.exchangerateapi.model.clients.exchangerates.LiveRates;
import com.miguelbf.exchangerateapi.model.dto.RatesResponse;
import com.miguelbf.exchangerateapi.service.IExchangeRatesService;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;


@Service
public class ExchangeRatesService implements IExchangeRatesService {

    private final IExchangeRatesClientService exchangeRatesClientService;

    public ExchangeRatesService(IExchangeRatesClientService exchangeRatesClientService) {
        this.exchangeRatesClientService = exchangeRatesClientService;
    }

    @Override
    @Cacheable(value = "liveRates", key = "#source.name() + (#target != null ? ':' + #target.name() : '')", sync = true)
    public RatesResponse getRates(Currency source, @Nullable Currency target) {
        LiveRates liveRates = this.exchangeRatesClientService.getLiveRates(source, target);
        Currency liveRatesSource = liveRates.getSource();
        Map<Currency, BigDecimal> quotes = liveRates.getQuotes();

        if (!source.equals(liveRatesSource)) {
            throw new RatesUpstreamDataException.UnexpectedSource(
                "Expected source currency %s, got %s".formatted(source, liveRatesSource), source, target, liveRates);
        } else if (target != null) {
            if (quotes.size() != 1) {
                throw new RatesUpstreamDataException.UnexpectedQuoteCount(
                    "Expected 1 quote source %s to %s, got %s".formatted(source, target, quotes.size()), source, target, liveRates);
            } else if (!quotes.containsKey(target)) {
                throw new RatesUpstreamDataException.UnexpectedTarget(
                    "Expected 1 quote source %s to %s, got other %s".formatted(source, target, quotes.size()), source, target, liveRates);
            }
        } else if (quotes.size() <= 1) {
            throw new RatesUpstreamDataException.UnexpectedQuoteCount(
                "Expected multiple quotes source %s to %s, got %s".formatted(source, null, quotes.size()), source, null, liveRates);
        }

        return new RatesResponse(liveRates.getTimestamp(), liveRatesSource, quotes);
    }
}
