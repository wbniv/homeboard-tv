package com.gathin.tvhome;

import java.util.List;
import org.junit.Test;
import static com.gathin.tvhome.HomeSettingsMenu.Entry.*;
import static org.junit.Assert.assertEquals;

/** Home settings' entry order (D2 of docs/plans/2026-11-26-top-row-start-screensaver.md), four entries a page. */
public class HomeSettingsMenuTest {
    private static final int PAGE = 4;

    @Test public void deviceSettingsIsOnPageOneInTheLeftSlot() {
        List<HomeSettingsMenu.Entry> entries = HomeSettingsMenu.entries(false);
        assertEquals(List.of(WIDGETS_AND_RESTORE, APPEARANCE, SCREENSAVER, DEVICE_SETTINGS), entries.subList(0, PAGE));
        assertEquals(3, entries.indexOf(DEVICE_SETTINGS));   // the fourth slot of a page is left of the hub
    }

    @Test public void homeBehaviorMovesToPageTwoAndAboutToPageThree() {
        List<HomeSettingsMenu.Entry> entries = HomeSettingsMenu.entries(false);
        assertEquals(List.of(HOME_BEHAVIOR, LANGUAGE, UNDO_HISTORY, HOME_BUTTON), entries.subList(PAGE, 2 * PAGE));
        assertEquals(List.of(ABOUT, PHONE, CLEANUP), entries.subList(2 * PAGE, entries.size()));
    }

    // A dashboard saved by a newer Homeboard: Android's Settings stays reachable without the top row's gear.
    @Test public void aNewerDashboardOpensOnlyDeviceSettingsAndAbout() {
        assertEquals(List.of(DEVICE_SETTINGS, ABOUT), HomeSettingsMenu.entries(true));
    }
}
