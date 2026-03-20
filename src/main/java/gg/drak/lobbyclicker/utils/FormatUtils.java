package gg.drak.lobbyclicker.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class FormatUtils {
    private static final String[] SUFFIXES = {
            "", "K", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No", "Dc",
            "UDc", "DDc", "TDc", "QaDc", "QiDc", "SxDc", "SpDc", "OcDc", "NoDc", "Vg",
            "UVg", "DVg", "TVg", "QaVg", "QiVg", "SxVg", "SpVg", "OcVg", "NoVg", "Tg"
    };

    public static String format(BigDecimal value) {
        if (value.signum() < 0) return "-" + format(value.negate());
        if (value.compareTo(new BigDecimal("1000")) < 0) {
            return value.setScale(0, RoundingMode.FLOOR).toPlainString();
        }

        // Determine the exponent group (each group is 3 digits / 10^3)
        String plain = value.setScale(0, RoundingMode.FLOOR).toPlainString();
        int digits = plain.length();
        int exp = (digits - 1) / 3;

        if (exp >= SUFFIXES.length) {
            // Scientific notation for extremely large numbers
            BigDecimal scaled = value.movePointLeft((digits - 1));
            return scaled.setScale(2, RoundingMode.FLOOR).toPlainString() + "e" + (digits - 1);
        }

        BigDecimal divisor = BigDecimal.TEN.pow(exp * 3);
        BigDecimal scaled = value.divide(divisor, 10, RoundingMode.FLOOR);

        if (scaled.compareTo(new BigDecimal("100")) >= 0) {
            return scaled.setScale(0, RoundingMode.FLOOR).toPlainString() + SUFFIXES[exp];
        } else if (scaled.compareTo(BigDecimal.TEN) >= 0) {
            return scaled.setScale(1, RoundingMode.FLOOR).toPlainString() + SUFFIXES[exp];
        } else {
            return scaled.setScale(2, RoundingMode.FLOOR).toPlainString() + SUFFIXES[exp];
        }
    }

    public static String format(double value) {
        return format(BigDecimal.valueOf(value));
    }

    public static String format(long value) {
        return format(BigDecimal.valueOf(value));
    }

    /**
     * Parse shorthand amount strings like "2.5m", "20000", "5k", "1b", "100".
     * Returns null if the input cannot be parsed.
     */
    public static BigDecimal parseShorthand(String input) {
        if (input == null || input.isBlank()) return null;
        input = input.trim().toLowerCase().replace(",", "");

        BigDecimal multiplier = BigDecimal.ONE;
        // Check for suffix
        if (input.endsWith("k")) {
            multiplier = new BigDecimal("1000");
            input = input.substring(0, input.length() - 1);
        } else if (input.endsWith("m")) {
            multiplier = new BigDecimal("1000000");
            input = input.substring(0, input.length() - 1);
        } else if (input.endsWith("b")) {
            multiplier = new BigDecimal("1000000000");
            input = input.substring(0, input.length() - 1);
        } else if (input.endsWith("t")) {
            multiplier = new BigDecimal("1000000000000");
            input = input.substring(0, input.length() - 1);
        }

        try {
            BigDecimal base = new BigDecimal(input);
            if (base.signum() < 0) return null;
            return base.multiply(multiplier).setScale(0, RoundingMode.FLOOR);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
