package com.miguelbf.exchangerateapi.controller;


import com.miguelbf.exchangerateapi.model.clients.exchangerates.Currency;
import com.miguelbf.exchangerateapi.model.dto.RatesResponse;
import com.miguelbf.exchangerateapi.service.IExchangeRatesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(value = "/api/rates")
@Tag(name = "Rates", description = "Provides currency exchange rates valid with up to a minute of delay")
public class ExchangeRatesController {

	private final IExchangeRatesService exchangeRatesService;

	public ExchangeRatesController(IExchangeRatesService exchangeRatesService) {
		this.exchangeRatesService = exchangeRatesService;
	}

	@Operation(
		summary = "Get currency exchange rates",
		description = """
			Returns most recent exchange rates from the specified `source` currency. \
			By default, rates are returned against all supported currencies. You may optionally specify a
			`target` currency to restrict the response to a single target currency."""
	)
	@ApiResponses(
		@ApiResponse(
			responseCode = "200",
			description = "Latest, but **possibly cached** rates snapshot. Inspect `timestamp` to determine freshness of `rates`."
		)
	)
	@GetMapping
	@ResponseStatus(HttpStatus.OK)
	public RatesResponse getExchangeRates(
		@Parameter(description = "Source (base) currency to which all `rates` are relative", required = true)
		@RequestParam(required = true) Currency from,

		@Parameter(description = "Target currency to which all `rates` are relative")
		@RequestParam(required = false) @Nullable Currency to
	) {
		return this.exchangeRatesService.getRates(from, to);
	}

}
