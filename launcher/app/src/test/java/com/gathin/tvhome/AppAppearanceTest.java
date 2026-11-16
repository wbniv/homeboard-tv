package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class AppAppearanceTest {
    @Test public void haloAndRingLeaveRoomOutsideEnlargedArtworkAtEveryDensity() {
        for (float density : new float[]{1f, 1.5f, 2f, 3f}) {
            float diameter = 48 * density;
            float artworkRadius = diameter * AppAppearance.FOCUS_SCALE / 2;
            float ringInner = AppAppearance.ringOuterRadius(diameter) - AppAppearance.ringWidth(diameter);
            assertTrue("Ring must not cover the enlarged artwork", ringInner > artworkRadius);
            assertTrue("Halo must not be clipped to the rim", AppAppearance.focusExtent(diameter) / 2f
                    > AppAppearance.ringOuterRadius(diameter));
        }
    }

    @Test public void denseGridAndLargeTextKeepTheFocusedArtworkAndLabelInsideTheCell() {
        for (int width : new int[]{70, 96, 160}) {
            for (int height : new int[]{64, 96, 180}) {
                for (int labelHeight : new int[]{20, 38, 48}) {
                    int diameter = AppAppearance.fitIcon(96, width, height, labelHeight, 8);
                    assertTrue(diameter >= 0 && diameter <= 96);
                    assertTrue(diameter * AppAppearance.FOCUS_SCALE <= width);
                    if (height >= labelHeight + 8)
                        assertTrue(diameter * AppAppearance.FOCUS_SCALE + labelHeight + 8 <= height);
                    else assertEquals(0, diameter);
                }
            }
        }
    }

    @Test public void unmeasuredCellCannotAllocateAVisibleIcon() {
        assertEquals(0, AppAppearance.fitIcon(96, 0, 0, 20, 8));
        assertEquals(0, AppAppearance.fitIcon(96, 100, 18, 20, 8));
    }
}
