package com.gathin.tvhome;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The entries of Home settings, the radial menu, in page order (four a page). Device settings, Android's own
 * Settings, sits on page 1 in the left slot, since Home's top row no longer has a gear (D2 of
 * docs/plans/2026-11-26-top-row-start-screensaver.md). A dashboard saved by a newer Homeboard cannot be changed, so
 * its menu keeps only what never touches the layout: Device settings and About Homeboard TV.
 */
final class HomeSettingsMenu {
    private HomeSettingsMenu() { }

    enum Entry {
        WIDGETS_AND_RESTORE, APPEARANCE, SCREENSAVER, DEVICE_SETTINGS,
        HOME_BEHAVIOR, LANGUAGE, UNDO_HISTORY, HOME_BUTTON,
        ABOUT, PHONE, CLEANUP
    }

    private static final List<Entry> FULL = Collections.unmodifiableList(Arrays.asList(Entry.values()));
    private static final List<Entry> READ_ONLY = Collections.unmodifiableList(Arrays.asList(Entry.DEVICE_SETTINGS, Entry.ABOUT));

    /** The entries shown, in order; readOnly when the saved dashboard came from a newer Homeboard. */
    static List<Entry> entries(boolean readOnly) { return readOnly ? READ_ONLY : FULL; }
}
