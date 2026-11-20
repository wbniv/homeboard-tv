package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class AppFocusHighlightTest {
    @Test public void roundAndBannerHighlightsSitBehindTheArtworkAndPlainIconsGetGlassOnTop() {
        assertTrue(AppFocusHighlight.behindArtwork(false, true));
        assertTrue(AppFocusHighlight.behindArtwork(true, false));
        assertTrue(AppFocusHighlight.behindArtwork(true, true));
        assertFalse(AppFocusHighlight.behindArtwork(false, false));
    }

    @Test public void plainIconScaleIsTheOneAllAppsAlwaysUsed() {
        assertEquals(1.08f, AppFocusHighlight.SCALE, 0f);
    }
}
