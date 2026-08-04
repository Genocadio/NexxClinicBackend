package com.nexxserve.nexxclinic.service.billing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Shared monetary helpers for the billing flow. Money is always handled as
 * {@link BigDecimal} with 2 decimal places (HALF_UP); quantities use 4 decimal
 * places. These rules must stay consistent across every billing component.
 */
public final class MoneyUtils {

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private MoneyUtils() {
        // utility class
    }

    /**
     * Normalizes an amount to money: null and negative values collapse to zero,
     * everything else is rounded to 2 decimal places (HALF_UP).
     */
    public static BigDecimal toMoney(BigDecimal value) {
        if (value == null) {
            return ZERO;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return ZERO;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Normalizes a quantity: null and non-positive values collapse to one,
     * everything else is rounded to 4 decimal places (HALF_UP).
     */
    public static BigDecimal toQuantity(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP);
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }
}
