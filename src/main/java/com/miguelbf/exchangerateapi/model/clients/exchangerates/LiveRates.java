package com.miguelbf.exchangerateapi.model.clients.exchangerates;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class LiveRates implements Serializable {

	@Serial
	private static final long serialVersionUID = 855532991964951105L;

	@JsonProperty("terms")
	private @Nullable URI terms = null;

	@JsonProperty("privacy")
	private @Nullable URI privacy = null;

	private final long timestamp;
	private final Currency source;
	private final Map<Currency, BigDecimal> quotes;

	@JsonCreator
	public LiveRates(
		@JsonProperty(value = "timestamp", required = true)
		long timestamp,

		@JsonProperty(value = "source", required = true)
		Currency source,

		@JsonProperty(value = "quotes", required = true)
		Map<String, BigDecimal> rawQuotes
	) {
		this.timestamp = timestamp;
		this.source = source;
		this.quotes = buildQuotes(source, rawQuotes);
	}

	private static Map<Currency, BigDecimal> buildQuotes(
		Currency source,
		Map<String, BigDecimal> rawQuotes
	) {
		if (rawQuotes.isEmpty()) {
			throw new IllegalArgumentException("No quotes provided for source currency '%s'".formatted(source.name()));
		}

		String prefix = source.name();
		int prefixLength = prefix.length();

		Map<Currency, BigDecimal> quotes = HashMap.newHashMap(rawQuotes.size());
		rawQuotes.forEach((key, value) -> {
			if (!key.startsWith(prefix)) {
				throw new IllegalArgumentException(
					"Quote key '%s' does not match source prefix '%s'".formatted(key, prefix)
				);
			}
			quotes.put(Currency.valueOf(key.substring(prefixLength)), value);
		});

		return Collections.unmodifiableMap(quotes);
	}
}

