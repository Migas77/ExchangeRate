package com.miguelbf.exchangerateapi.exception;

import com.miguelbf.exchangerateapi.model.clients.exchangerates.Currency;
import com.miguelbf.exchangerateapi.model.clients.exchangerates.LiveRates;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

@Getter
public abstract sealed class RatesUpstreamDataException extends RuntimeException {

	private final Currency source;
	private final @Nullable Currency target;
	private final LiveRates liveRates;

	protected RatesUpstreamDataException(String message, Currency source, @Nullable Currency target, LiveRates liveRates) {
		super(message);
		this.source = source;
		this.target = target;
		this.liveRates = liveRates;
	}

	public static final class UnexpectedSource extends RatesUpstreamDataException {
		// upstream API returned unexpected source currency according to the request
		public UnexpectedSource(String message, Currency from, @Nullable Currency target, LiveRates liveRates) {
			super(message, from, target, liveRates);
		}
	}

	public static final class UnexpectedTarget extends RatesUpstreamDataException {
		// upstream API returned unexpected source currency/currencies according to the request
		public UnexpectedTarget(String message, Currency from, @Nullable Currency target, LiveRates liveRates) {
			super(message, from, target, liveRates);
		}
	}

	public static final class UnexpectedQuoteCount extends RatesUpstreamDataException {
		// upstream API returned unexpected quote count according to the request
		public UnexpectedQuoteCount(String message, Currency from, @Nullable Currency target, LiveRates liveRates) {
			super(message, from, target, liveRates);
		}
	}

}
