package gg.drak.lobbyclicker.math;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class CookieMath {
    public static final MathContext MC = MathContext.DECIMAL128;
    public static final BigDecimal ZERO = BigDecimal.ZERO;
    public static final BigDecimal ONE = BigDecimal.ONE;
    public static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    public static final BigDecimal ONE_THOUSAND = new BigDecimal("1000");

    public static BigDecimal floor(BigDecimal val) {
        return val.setScale(0, RoundingMode.FLOOR);
    }

    public static BigDecimal pow(BigDecimal base, int exponent) {
        if (exponent == 0) return ONE;
        if (exponent == 1) return base;
        return base.pow(exponent, MC);
    }

    public static int digitCount(BigDecimal value) {
        if (value.signum() <= 0) return 0;
        BigDecimal floored = floor(value.abs());
        if (floored.compareTo(ONE) < 0) return 0;
        return floored.toPlainString().length();
    }

    public static int digitCount(long value) {
        if (value < 1) return 0;
        return Long.toString(value).length();
    }

    public static int leadDigit(BigDecimal value) {
        if (value.signum() <= 0) return 0;
        String s = value.abs().toPlainString();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '1' && c <= '9') return c - '0';
        }
        return 0;
    }

    public static int leadDigit(long value) {
        if (value < 1) return 0;
        while (value >= 10) value /= 10;
        return (int) value;
    }

    /**
     * Parse a BigDecimal from a string, falling back to ZERO on null/empty/invalid.
     */
    public static BigDecimal parse(String value) {
        if (value == null || value.isEmpty()) return ZERO;
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            // Try parsing as double for legacy DOUBLE column data
            try {
                return BigDecimal.valueOf(Double.parseDouble(value));
            } catch (NumberFormatException e2) {
                return ZERO;
            }
        }
    }
}
