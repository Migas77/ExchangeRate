package com.miguelbf.exchangerateapi.service;

import com.miguelbf.exchangerateapi.exception.RatesUpstreamDataException;
import com.miguelbf.exchangerateapi.model.clients.exchangerates.Currency;
import com.miguelbf.exchangerateapi.model.clients.exchangerates.LiveRates;
import com.miguelbf.exchangerateapi.model.dto.RatesResponse;
import com.miguelbf.exchangerateapi.client.IExchangeRatesClientService;
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
	public RatesResponse getRates(Currency from, @Nullable Currency to) {
		LiveRates liveRates = this.exchangeRatesRepository.getLiveRates(from, to);
		Currency source = liveRates.getSource();
		Map<Currency, BigDecimal> quotes = liveRates.getQuotes();

		if (!from.equals(source)) {
			throw new RatesUpstreamDataException.UnexpectedSource(
				"Expected source currency %s, got %s".formatted(from, source), from, to, liveRates);
		} else if (quotes.isEmpty()) {
            throw new RatesUpstreamDataException.UnexpectedQuoteCount(
                "Expected at least 1 quote from %s".formatted(from), from, null, liveRates);
        } else if (to != null) {
			if (quotes.size() != 1) {
				throw new RatesUpstreamDataException.UnexpectedQuoteCount(
					"Expected 1 quote from %s to %s, got %s".formatted(from, to, quotes.size()), from, to, liveRates);
			} else if (!quotes.containsKey(to)) {
				throw new RatesUpstreamDataException.UnexpectedTarget(
					"Expected 1 quote from %s to %s, got other %s".formatted(from, to, quotes.size()), from, to, liveRates);
			}
		}

		return new RatesResponse(liveRates.getTimestamp(), source, quotes);
	}
}
