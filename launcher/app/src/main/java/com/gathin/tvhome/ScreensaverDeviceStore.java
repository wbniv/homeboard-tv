package com.gathin.tvhome;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Two device facts, kept in their own small preferences file and never in the Home layout, which Undo replaces:
 * whether Homeboard manages this TV's Android screensaver, and the original, the values Homeboard found before
 * its first write. The original is recorded once and never overwritten; Restore puts it back.
 */
final class ScreensaverDeviceStore {
    static final String FILE = "screensaver-device";
    static final String MANAGED = "managed", ORIGINAL = "original";

    /** The storage underneath: SharedPreferences on the TV, a map in tests. put commits synchronously; a null value removes. */
    interface Backing {
        String get(String key);
        boolean put(Map<String, String> changes);
    }

    private final Backing backing;

    ScreensaverDeviceStore(Backing backing) { this.backing = backing; }

    boolean managed() { return "true".equals(backing.get(MANAGED)); }

    boolean setManaged(boolean managed) {
        return managed() == managed || backing.put(Collections.singletonMap(MANAGED, managed ? "true" : null));
    }

    /** The kept original, or null before Homeboard's first change. */
    ScreensaverValues original() { return ScreensaverValues.parse(backing.get(ORIGINAL)); }

    /**
     * Records found as the original unless one is already recorded. Returns false only when a record was needed
     * and could not be stored, in which case nothing must be written to Android.
     */
    boolean recordOriginal(ScreensaverValues found) {
        if (original() != null) return true;
        Map<String, String> change = new HashMap<>();
        change.put(ORIGINAL, found.json().toString());
        return backing.put(change) && found.equals(original());
    }
}
