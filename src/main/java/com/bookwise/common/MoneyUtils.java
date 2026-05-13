package com.bookwise.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class MoneyUtils {

    public long toMinorUnits(BigDecimal majorAmount, String currencyCode) {
        Currency currency = Currency.getInstance(currencyCode);
        int fractionDigits = Math.max(currency.getDefaultFractionDigits(), 0);
        return majorAmount.setScale(fractionDigits, RoundingMode.HALF_UP)
                .movePointRight(fractionDigits)
                .longValueExact();
    }

    public String formatMinorUnits(long minorAmount, String currencyCode) {
        Currency currency = Currency.getInstance(currencyCode);
        int fractionDigits = Math.max(currency.getDefaultFractionDigits(), 0);
        BigDecimal major = BigDecimal.valueOf(minorAmount, fractionDigits);
        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        format.setMinimumFractionDigits(fractionDigits);
        format.setMaximumFractionDigits(fractionDigits);
        return format.format(major) + " " + currencyCode;
    }
}
