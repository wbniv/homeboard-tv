package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class ScreensaverAlarmTest {
    @Test public void theScreensaverAndTheAlarmNotificationOpenTheSameTimerScreen() {
        // Regression guard: the screensaver, the notification and MainActivity share one action constant, so an
        // alarm that ends a screensaver can never lead somewhere other than the screen the notification opens.
        assertEquals("homeboard.timers", ScreensaverActivity.ACTION_OPEN_TIMERS);
    }
}
