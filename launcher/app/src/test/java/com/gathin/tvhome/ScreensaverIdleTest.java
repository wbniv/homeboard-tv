package com.gathin.tvhome;

import org.junit.Test;

import static org.junit.Assert.*;

public class ScreensaverIdleTest {
    private static final long MINUTE = 60_000L;

    @Test public void androidDecidesRunsNoHomeboardTimer() {
        ScreensaverIdle.Decision d = ScreensaverIdle.decide(10 * MINUTE, 0, ScreensaverSettings.ANDROID_DECIDES, true, false);
        assertEquals(ScreensaverIdle.Kind.NONE, d.kind);
        assertTrue(d.delayMs < 0);
    }

    @Test public void waitsTheRemainderRebasedOnTheLastInput() {
        ScreensaverIdle.Decision d = ScreensaverIdle.decide(1_000_000L + 90_000L, 1_000_000L, 5, true, false);
        assertEquals(ScreensaverIdle.Kind.WAIT, d.kind);
        assertEquals(5 * MINUTE - 90_000L, d.delayMs);
        // A new input moves the deadline: the full time again from that input.
        d = ScreensaverIdle.decide(1_000_000L + 90_000L, 1_000_000L + 90_000L, 5, true, false);
        assertEquals(5 * MINUTE, d.delayMs);
    }

    @Test public void startsOnceTheIdleTimeHasPassed() {
        assertEquals(ScreensaverIdle.Kind.START, ScreensaverIdle.decide(MINUTE, 0, 1, true, false).kind);
        assertEquals(ScreensaverIdle.Kind.START, ScreensaverIdle.decide(31 * MINUTE, 0, 30, true, false).kind);
        assertEquals(ScreensaverIdle.Kind.WAIT, ScreensaverIdle.decide(MINUTE - 1, 0, 1, true, false).kind);
    }

    @Test public void neverRunsWhenHomeIsNotInFrontOrIsInUse() {
        assertEquals(ScreensaverIdle.Kind.NONE, ScreensaverIdle.decide(60 * MINUTE, 0, 1, false, false).kind);
        assertEquals(ScreensaverIdle.Kind.NONE, ScreensaverIdle.decide(60 * MINUTE, 0, 1, true, true).kind);
        assertEquals(ScreensaverIdle.Kind.NONE, ScreensaverIdle.decide(60 * MINUTE, 0, 1, false, true).kind);
    }

    @Test public void everyAllowedTimeIsHonoured() {
        for (int minutes : ScreensaverSettings.IDLE_MINUTES) {
            if (minutes == ScreensaverSettings.ANDROID_DECIDES) continue;
            ScreensaverIdle.Decision d = ScreensaverIdle.decide(0, 0, minutes, true, false);
            assertEquals(ScreensaverIdle.Kind.WAIT, d.kind);
            assertEquals(minutes * MINUTE, d.delayMs);
        }
    }
}
