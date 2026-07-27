package com.miguelbf.exchangerateapi.service;

import com.miguelbf.exchangerateapi.client.IExchangeRatesClientService;
import com.miguelbf.exchangerateapi.exception.exception.RatesUpstreamDataException;
import com.miguelbf.exchangerateapi.model.clients.exchangerates.Currency;
import com.miguelbf.exchangerateapi.model.clients.exchangerates.LiveRates;
import com.miguelbf.exchangerateapi.model.dto.RatesResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;


@Service
public class ExchangeRatesService implements IExchangeRatesService {

    private final IExchangeRatesClientService exchangeRatesRepository;

    public ExchangeRatesService(IExchangeRatesClientService exchangeRatesRepository) {
        this.exchangeRatesRepository = exchangeRatesRepository;
    }

    @Override
    public RatesResponse getRates(Currency from, @Nullable Currency target) {
        LiveRates liveRates = this.exchangeRatesRepository.getLiveRates(from, target);
        Currency source = liveRates.getSource();
        Map<Currency, BigDecimal> quotes = liveRates.getQuotes();

        if (!from.equals(source)) {
            throw new RatesUpstreamDataException.UnexpectedSource(
                "Expected source currency %s, got %s".formatted(from, source), from, target, liveRates);
        } else if (target != null) {
            if (quotes.size() != 1) {
                throw new RatesUpstreamDataException.UnexpectedQuoteCount(
                    "Expected 1 quote from %s to %s, got %s".formatted(from, target, quotes.size()), from, target, liveRates);
            } else if (!quotes.containsKey(target)) {
                throw new RatesUpstreamDataException.UnexpectedTarget(
                    "Expected 1 quote from %s to %s, got other %s".formatted(from, target, quotes.size()), from, target, liveRates);
            }
        } else if (quotes.size() <= 1) {
            throw new RatesUpstreamDataException.UnexpectedQuoteCount(
                "Expected multiple quotes from %s to %s, got %s".formatted(from, null, quotes.size()), from, null, liveRates);
        }

        return new RatesResponse(liveRates.getTimestamp(), source, quotes);
    }
}
