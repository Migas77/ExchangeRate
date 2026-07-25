package com.miguelbf.exchangerateapi.controller;


import com.miguelbf.exchangerateapi.model.clients.exchangerates.Currency;
import com.miguelbf.exchangerateapi.model.dto.RatesResponse;
import com.miguelbf.exchangerateapi.service.IExchangeRatesService;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/rates")
public class ExchangeRatesController {

    private final IExchangeRatesService exchangeRatesService;

    public ExchangeRatesController(IExchangeRatesService exchangeRatesService) {
        this.exchangeRatesService = exchangeRatesService;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public RatesResponse getExchangeRates(
        @RequestParam(required = true) Currency from,
        @RequestParam(required = false) @Nullable Currency to
    ){
        return this.exchangeRatesService.getRates(from, to);
    }

}
