package gg.drak.lobbyclicker.utils;

public class FormatUtils {
    private static final String[] SUFFIXES = {"", "K", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No", "Dc"};

    public static String format(double value) {
        if (value < 0) return "-" + format(-value);
        if (value < 1000) return String.valueOf((long) value);

        int exp = (int) (Math.log10(value) / 3);
        if (exp >= SUFFIXES.length) exp = SUFFIXES.length - 1;

        double scaled = value / Math.pow(1000, exp);
        if (scaled >= 100) {
            return String.format("%.0f%s", scaled, SUFFIXES[exp]);
        } else if (scaled >= 10) {
            return String.format("%.1f%s", scaled, SUFFIXES[exp]);
        } else {
            return String.format("%.2f%s", scaled, SUFFIXES[exp]);
        }
    }
}
