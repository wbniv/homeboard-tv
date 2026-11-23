package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ResolutionModeChooserTest {
    @Test public void choosesGreatestPixelAreaRatherThanGreatestRefresh() {
        assertEquals(1, ResolutionModeChooser.bestIndex(
                new int[]{1920, 3840, 3840}, new int[]{1080, 2160, 2160},
                new float[]{60, 60, 24}, 0));
    }
    @Test public void keepsCurrentModeWhenResolutionAndRefreshMatch() {
        assertEquals(1, ResolutionModeChooser.bestIndex(
                new int[]{3840, 3840, 1920}, new int[]{2160, 2160, 1080},
                new float[]{60, 60, 120}, 1));
    }
    @Test public void handlesMissingAndInvalidModes() {
        assertEquals(-1, ResolutionModeChooser.bestIndex(new int[]{0}, new int[]{0}, new float[]{60}, -1));
        assertEquals(-1, ResolutionModeChooser.bestIndex(new int[]{}, new int[]{}, new float[]{}, -1));
    }
    @Test public void rotatesNaturalPortraitModeForLandscape() {
        assertEquals(2992, ResolutionModeChooser.orientedWidth(1344, 2992, 1));
        assertEquals(1344, ResolutionModeChooser.orientedHeight(1344, 2992, 1));
        assertEquals(2992, ResolutionModeChooser.orientedWidth(1344, 2992, 3));
        assertEquals(1344, ResolutionModeChooser.orientedWidth(1344, 2992, 0));
    }
}
