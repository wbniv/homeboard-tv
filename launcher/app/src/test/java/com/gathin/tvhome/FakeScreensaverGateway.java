package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** An in-memory Android for EverywhereScreensaver and ScreensaverRules tests. Nothing here touches a device. */
final class FakeScreensaverGateway implements ScreensaverGateway {
    static final String OWN = "net.biohack.homeboard.debug/com.gathin.tvhome.ScreensaverDream";
    static final String AMBIENT = "com.google.android.apps.tv.dreamx/com.google.android.apps.tv.dreamx.service.Backdrop";
    static final String AERIAL = "com.neilturner.aerialviews/com.neilturner.aerialviews.ui.screensaver.DreamActivity";

    /** The secure settings as Android holds them; a missing key reads as not set. */
    final Map<String, String> secure = new HashMap<>();
    final Set<String> unreadable = new HashSet<>();
    /** Keys whose write Android refuses (putString returns false). */
    final Set<String> refused = new HashSet<>();
    /** Keys whose next write reports OK but stores something else, so the read-back differs. One-shot. */
    final Map<String, String> storesInstead = new HashMap<>();
    /** Keys whose every write reports OK but stores something else: a revert cannot succeed either. */
    final Map<String, String> alwaysStoresInstead = new HashMap<>();
    boolean permitted = true, startNowWorks = true;
    String timeout = "600000";
    boolean timeoutReadable = true;
    final List<InstalledDream> dreams = new ArrayList<>();
    /** Every write attempted, in order, as "key=value". */
    final List<String> writes = new ArrayList<>();
    int startNowCalls;

    FakeScreensaverGateway() {
        secure.put(COMPONENTS, AMBIENT);
        secure.put(ENABLED, "1");
        secure.put(ACTIVATE_ON_SLEEP, "1");
    }

    @Override public Reading readSecure(String key) {
        return unreadable.contains(key) ? Reading.UNREADABLE : Reading.of(secure.get(key));
    }

    @Override public Reading readTimeout() { return timeoutReadable ? Reading.of(timeout) : Reading.UNREADABLE; }

    @Override public boolean mayWrite() { return permitted; }

    @Override public Write writeSecure(String key, String value) {
        writes.add(key + "=" + value);
        if (!permitted) return Write.NOT_PERMITTED;
        if (refused.contains(key)) return Write.REFUSED;
        String stored = alwaysStoresInstead.containsKey(key) ? alwaysStoresInstead.get(key)
                : storesInstead.containsKey(key) ? storesInstead.remove(key) : value;
        if (stored == null) secure.remove(key); else secure.put(key, stored);
        return Write.OK;
    }

    @Override public boolean startNow() { startNowCalls++; return startNowWorks; }

    @Override public List<InstalledDream> installedDreams() { return dreams; }

    @Override public String ownComponent() { return OWN; }

    /** A store whose put can be made to fail. */
    static final class MemoryBacking implements ScreensaverDeviceStore.Backing {
        final Map<String, String> values = new HashMap<>();
        /** How many puts succeed before every later one fails. */
        int allowedPuts = Integer.MAX_VALUE;
        int puts;
        @Override public String get(String key) { return values.get(key); }
        @Override public boolean put(Map<String, String> changes) {
            if (puts++ >= allowedPuts) return false;
            for (Map.Entry<String, String> change : changes.entrySet()) {
                if (change.getValue() == null) values.remove(change.getKey()); else values.put(change.getKey(), change.getValue());
            }
            return true;
        }
    }
}
