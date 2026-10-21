package com.partneritems.util;

public final class TimeFormat {

    private TimeFormat() {
    }

    /**
     * Under a minute: one decimal ("4.2s"), rounded up so it never shows 0.0s while something is still active.
     * Otherwise "1m 05s" or "1h 02m".
     */
    public static String format(long millis) {
        if (millis <= 0) {
            return "0.0s";
        }

        long tenths = (millis + 99) / 100;
        if (tenths < 600) {
            return (tenths / 10) + "." + (tenths % 10) + "s";
        }

        long totalSeconds = (millis + 999) / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%dh %02dm", hours, minutes);
        }
        return String.format("%dm %02ds", minutes, seconds);
    }
}
