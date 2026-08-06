package com.miguelbf.exchangerateapi.exception.exception;

import com.miguelbf.exchangerateapi.config.logging.CustomLogStructureFields;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.util.Map;

@Getter
public class IllegalConversionException extends RuntimeException implements CustomLogStructureFields {

    private final int integerDigits;
    private final int decimalDigits;

    public IllegalConversionException(String message, int integerDigits, int decimalDigits) {
        super(message);
        this.integerDigits = integerDigits;
        this.decimalDigits = decimalDigits;
    }

    @Override
    public Map<String, @Nullable Object> getLogStructureFields() {
        return Map.of(
            "integerDigits", this.integerDigits,
            "decimalDigits", this.decimalDigits
        );
    }

}
