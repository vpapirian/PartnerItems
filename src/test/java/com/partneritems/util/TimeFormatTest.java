package com.partneritems.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TimeFormatTest {

    @Test
    public void zeroAndNegative() {
        assertEquals("0.0s", TimeFormat.format(0));
        assertEquals("0.0s", TimeFormat.format(-500));
    }

    @Test
    public void roundsUpUnderAMinute() {
        assertEquals("0.1s", TimeFormat.format(1));
        assertEquals("4.2s", TimeFormat.format(4_200));
        assertEquals("4.3s", TimeFormat.format(4_201));
        assertEquals("59.9s", TimeFormat.format(59_900));
    }

    @Test
    public void switchesToMinutes() {
        assertEquals("1m 00s", TimeFormat.format(59_950));
        assertEquals("1m 30s", TimeFormat.format(90_000));
        assertEquals("15m 05s", TimeFormat.format(905_000));
    }

    @Test
    public void hours() {
        assertEquals("1h 00m", TimeFormat.format(3_600_000));
        assertEquals("2h 15m", TimeFormat.format(8_100_000));
    }
}
