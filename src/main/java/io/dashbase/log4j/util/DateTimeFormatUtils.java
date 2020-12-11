package io.dashbase.log4j.util;

public class DateTimeFormatUtils {
    /**
     * If a given dateTime format contains date, return true.
     */
    public static boolean containsDate(String format) {
        var pattern = format.toLowerCase();
        return (pattern.contains("u") || pattern.contains("y")) && pattern.contains("d");
    }

    /**
     * If a given dateTime format contains milli/nano second, return true.
     */
    public static boolean containsMilliSecond(String format) {
        return format.contains("S") || format.contains("n") || format.contains("N") || format.contains("A");
    }

    /**
     * Builds a lenient pattern by modifying two-digit patterns (e.g., "dd") to accept
     * both single-digit and two-digit value.
     */
    public static String toLenientPattern(String format) {
        return format.replace("dd", "d")
            .replace("HH", "H")
            .replace("hh", "h")
            .replace("KK", "K")
            .replace("kk", "k")
            .replace("mm", "m")
            .replace("ss", "s");
    }
}
