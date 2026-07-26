package com.miguelbf.exchangerateapi.client;

import com.miguelbf.exchangerateapi.exception.RatesUpstreamAPIException;
import com.miguelbf.exchangerateapi.model.clients.exchangerates.Currency;
import com.miguelbf.exchangerateapi.model.clients.exchangerates.ExchangeRateApiSuccess;
import com.miguelbf.exchangerateapi.model.clients.exchangerates.LiveRates;
import org.jspecify.annotations.Nullable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.Optional;

@Service
public class ExchangeRatesClientService implements IExchangeRatesClientService {

	private static final ParameterizedTypeReference<ExchangeRateApiSuccess<LiveRates>> LIVE_RATES_SUCCESS_TYPE =
		new ParameterizedTypeReference<>() {
		};
	private final RestClient restClient;

	public ExchangeRatesClientService(RestClient restClient) {
		this.restClient = restClient;
	}

	@Override
	public LiveRates getLiveRates(Currency source, @Nullable Currency target) {

		ExchangeRateApiSuccess<LiveRates> liveRatesResponse = restClient
			.get()
			.uri(uriBuilder -> uriBuilder
				.path("/live")
				.queryParam("source", source.name())
				.queryParamIfPresent("currencies", Optional.ofNullable(target).map(Currency::name))
				.build()
			)
			.exchange((request, response) -> {
				URI url = request.getURI();
				HttpMethod method = request.getMethod();
				HttpStatusCode httpStatusCode = response.getStatusCode();

				if (this.isDocumentedHttpErrorStatusCode(httpStatusCode)) {
					throw new RatesUpstreamAPIException.DocumentedHttpError(
						"Documented error on %s %s".formatted(method, url), response.createException(), url, method);
				} else if (!httpStatusCode.equals(HttpStatus.OK)) {
					throw new RatesUpstreamAPIException.UnexpectedHttpError(
						"Unexpected error on %s %s".formatted(method, url), response.createException(), url, method);
				}

				ExchangeRateApiSuccess<LiveRates> body = response.bodyTo(LIVE_RATES_SUCCESS_TYPE);
				if (body == null) {
					throw new RatesUpstreamAPIException.UnexpectedEmptyResponse(
						"Empty response body on %s %s".formatted(method, url), response.createException(), url, method);
				}

				return body;
			});

		return liveRatesResponse.data();
	}

	private boolean isDocumentedHttpErrorStatusCode(HttpStatusCode httpStatusCode) {
		// matches HTTP status codes explicitly documented in the exchange rate API's error spec
		return httpStatusCode.is5xxServerError()
			|| httpStatusCode.equals(HttpStatus.BAD_REQUEST)
			|| httpStatusCode.equals(HttpStatus.UNAUTHORIZED)
			|| httpStatusCode.equals(HttpStatus.TOO_MANY_REQUESTS);
	}
}
