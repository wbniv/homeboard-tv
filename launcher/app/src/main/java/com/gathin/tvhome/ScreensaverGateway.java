package com.gathin.tvhome;

import java.util.List;

/**
 * Everything Homeboard asks of Android about this TV's own screensaver, as one small interface so the rules
 * (ScreensaverRules) and the change logic (EverywhereScreensaver) are unit tested with a fake. SystemScreensaver
 * is the only real implementation and holds every framework call. No method throws: a read reports whether it
 * could read, a write reports whether Android took it, and the caller reads back to verify.
 */
interface ScreensaverGateway {
    /** Settings.Secure keys. The component list is raw: Android allows a comma-separated list, usually one. */
    String COMPONENTS = "screensaver_components", ENABLED = "screensaver_enabled",
            ACTIVATE_ON_SLEEP = "screensaver_activate_on_sleep";

    /** One read. readable false: Android refused or failed the read. value null with readable true: not set. */
    final class Reading {
        static final Reading UNREADABLE = new Reading(false, null);
        final boolean readable;
        final String value;
        private Reading(boolean readable, String value) { this.readable = readable; this.value = value; }
        static Reading of(String value) { return new Reading(true, value); }
    }

    /** What a single write call reported. The caller still reads back: OK is Android's word, not proof. */
    enum Write { OK, NOT_PERMITTED, REFUSED }

    /** An installed Android screensaver service, from PackageManager. */
    final class InstalledDream {
        /** ComponentName.flattenToString(). */
        final String component;
        final String label;
        /** The settings activity it declares in its android.service.dream metadata, flattened; null when none. */
        final String settings;
        InstalledDream(String component, String label, String settings) {
            this.component = component; this.label = label; this.settings = settings;
        }
    }

    Reading readSecure(String key);
    /** Settings.System screen_off_timeout in milliseconds, as text. */
    Reading readTimeout();
    /** Homeboard holds WRITE_SECURE_SETTINGS (granted once with pm grant). Changes nothing. */
    boolean mayWrite();
    /** value null clears the setting, so an original that was never set can be put back as it was. */
    Write writeSecure(String key, String value);
    /** Android's own "start the screensaver now" shortcut; false when it is missing or would not start. */
    boolean startNow();
    List<InstalledDream> installedDreams();
    /** This install's ScreensaverDream, flattened: the package differs between debug and release builds. */
    String ownComponent();
}
