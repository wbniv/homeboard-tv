package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class AppGridTest {
    @Test public void typicalTvFitsThirtySixIconsInsteadOfEightBanners() {
        assertEquals(36, AppGrid.columns(960, false, true) * AppGrid.rows(540, false));
        assertEquals(8, AppGrid.columns(960, true, true) * AppGrid.rows(540, true));
    }
    @Test public void smallViewportRetainsPositiveCellsAndRoomForLabels() {
        assertEquals(6, AppGrid.columns(720, false, true));
        assertEquals(3, AppGrid.rows(480, false));
        assertEquals(1, AppGrid.columns(80, false, false));
        assertEquals(1, AppGrid.rows(80, false));
    }
    @Test public void defaultsPreferDenseIconsAndPreserveExplicitBanners() throws Exception {
        assertFalse(HomeLayout.firstRun().appBanners);
        HomeLayout saved = HomeLayout.firstRun(); saved.appBanners = true;
        assertTrue(HomeLayout.parse(saved.encode()).appBanners);
    }
}
