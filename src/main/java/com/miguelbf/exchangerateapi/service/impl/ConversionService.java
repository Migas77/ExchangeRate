package com.miguelbf.exchangerateapi.service.impl;

import com.miguelbf.exchangerateapi.exception.exception.IllegalConversionException;
import com.miguelbf.exchangerateapi.exception.exception.RatesUpstreamDataException;
import com.miguelbf.exchangerateapi.model.clients.exchangerates.Currency;
import com.miguelbf.exchangerateapi.model.dto.ConversionResponse;
import com.miguelbf.exchangerateapi.model.dto.RatesResponse;
import com.miguelbf.exchangerateapi.service.IConversionService;
import com.miguelbf.exchangerateapi.service.IExchangeRatesService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Service
@AllArgsConstructor
@Slf4j
public class ConversionService implements IConversionService {

    private final IExchangeRatesService exchangeRatesService;

    // 12 digits was registered as the maximum number of decimal places from upstream API
    private static final int RESULT_SCALE = 12;

    // Magic Numbers observed empirically from checking the most/least valuable currency exchange rates
    private static final int MAX_RATE_DIGITS = 16;
    private static final int MAX_RATE_INTEGER_DIGITS = 10;
    private static final int MAX_RATE_DECIMAL_DIGITS = 12;

    // Values also defined at the API boundary (validate again)
    private static final int MAX_AMOUNT_DIGITS = 16;
    private static final int MAX_AMOUNT_INTEGER_DIGITS = 10;
    private static final int MAX_AMOUNT_DECIMAL_DIGITS = 6;

    @Override
    public ConversionResponse convertValue(BigDecimal amount, Currency source, Set<Currency> targets) {
        this.validateAmount(amount);

        RatesResponse ratesResponse = this.exchangeRatesService.getRates(
            source, targets.size() == 1 ? targets.iterator().next() : null);

        Map<Currency, BigDecimal> conversionRates = ratesResponse.rates();
        Map<Currency, BigDecimal> filteredRates = new EnumMap<>(Currency.class);
        Map<Currency, BigDecimal> convertedValues = new EnumMap<>(Currency.class);

        Set<Currency> conversionCurrencies = targets.isEmpty() ? conversionRates.keySet() : targets;
        for (Currency currency : conversionCurrencies) {
            BigDecimal rate = conversionRates.get(currency);
            if (rate == null) {
                throw new RatesUpstreamDataException.MissingTarget(
                    "Expected quote for source %s to target %s, got missing".formatted(source, currency),
                    source, currency, ratesResponse);
            }

            // Amount is guaranteed to be under 16 digits
            // From observation, the maximum number of digits registered for the rate is 16 too
            // Thus, DECIMAL128 34 digit precision is probably lossless
            this.validateRate(currency, rate);
            filteredRates.put(currency, rate);
            convertedValues.put(currency, amount.multiply(rate, MathContext.DECIMAL128)
                .setScale(RESULT_SCALE, RoundingMode.HALF_EVEN)
                .stripTrailingZeros());
        }

        return new ConversionResponse(ratesResponse.timestamp(), amount, source, filteredRates, convertedValues);
    }

    private void validateAmount(BigDecimal amount) {
        int decimalDigits = Math.max(amount.scale(), 0);
        int integerDigits = Math.max(amount.precision() - amount.scale(), 0);

        if (amount.precision() > MAX_AMOUNT_DIGITS
            || integerDigits > MAX_AMOUNT_INTEGER_DIGITS
            || decimalDigits > MAX_AMOUNT_DECIMAL_DIGITS
        ) {
            throw new IllegalConversionException("Unexpected amount shape", integerDigits, decimalDigits);
        }
    }

    private void validateRate(Currency currency, BigDecimal rate) {
        int decimalDigits = Math.max(rate.scale(), 0);
        int integerDigits = Math.max(rate.precision() - rate.scale(), 0);

        if (rate.precision() > MAX_RATE_DIGITS
            || integerDigits > MAX_RATE_INTEGER_DIGITS
            || decimalDigits > MAX_RATE_DECIMAL_DIGITS
        ) {
            log.atWarn().setMessage("Unexpected rate shape")
                .addKeyValue("currency", currency)
                .addKeyValue("integerDigits", integerDigits)
                .addKeyValue("decimalDigits", decimalDigits)
                .log();
        }
    }

}
