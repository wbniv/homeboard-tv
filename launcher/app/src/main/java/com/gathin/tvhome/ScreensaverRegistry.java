package com.gathin.tvhome;

import android.content.Context;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The screensavers the Screensaver screen lists: the built-in entries, then the Android screensaver apps installed
 * on this TV (Stage 3). The first built-in row is the default slot, so a better bundled screensaver later is one
 * more row placed first, with no change to the Screensaver screen, the settings or the scenes. An Android app is
 * listed by its own label under its flattened component; Android runs it, so it has no scene. An app that declares its
 * own preview screen (Stage 5, docs/plans/2026-11-26-screensaver-companion-contract.md) also previews and plays on
 * Home through that screen; one that does not has no Preview and is used only through Use everywhere.
 */
final class ScreensaverRegistry {
    static final String CLOCK = "clock", HOME_BACKGROUND = "background";
    /** The Dream service meta-data that names its preview screen (the companion contract's "What each app declares"). */
    static final String PREVIEW_ACTIVITY = "org.worldfoundry.screensaver.PREVIEW_ACTIVITY",
            PREVIEW_EXTRA = "org.worldfoundry.screensaver.PREVIEW_EXTRA", PREVIEW_VALUE = "org.worldfoundry.screensaver.PREVIEW_VALUE";

    /** The options an entry's Options screen offers. "Start on Home after" applies to every entry. */
    enum Option { BACKDROP, BACKDROP_DIM, SHOW_CLOCK, DIM }

    interface SceneFactory {
        /** Builds the scene inside container (empty, full screen) from layout's screensaver options and background. */
        ScreensaverScene create(Context context, FrameLayout container, HomeLayout layout);
    }

    enum Kind { BUILT_IN, ANDROID_APP, MISSING_APP }

    static final class Entry {
        final String id;
        final Kind kind;
        /** label is a string resource for a built-in entry and 0 for an app, which carries its own name instead. */
        final int label, summary, description;
        final String name;
        /** A one-character glyph for the list card, like the menu icons. */
        final String glyph;
        final Set<Option> options;
        /** null for an Android screensaver app: Android runs it, Homeboard cannot. */
        final SceneFactory scene;
        /** An app's declared settings activity, flattened; null when it declares none. Always null when built in. */
        final String settings;
        /** An app's own preview screen, which Preview, Home idle and the remote start; null when none or built in. */
        final ScreensaverGateway.AppScreen screen;
        Entry(String id, int label, int summary, int description, String glyph, Set<Option> options, SceneFactory scene) {
            this(id, Kind.BUILT_IN, label, null, summary, description, glyph, options, scene, null, null);
        }
        private Entry(String id, Kind kind, int label, String name, int summary, int description, String glyph,
                      Set<Option> options, SceneFactory scene, String settings, ScreensaverGateway.AppScreen screen) {
            this.id = id; this.kind = kind; this.label = label; this.name = name; this.summary = summary;
            this.description = description; this.glyph = glyph; this.options = Collections.unmodifiableSet(options);
            this.scene = scene; this.settings = settings; this.screen = screen;
        }
        /** An Android screensaver app, installed or (missing) not. */
        boolean app() { return kind == Kind.ANDROID_APP || kind == Kind.MISSING_APP; }
        /** An app Homeboard can play itself, through the app's own preview screen. */
        boolean playsOnHome() { return app() && screen != null; }
        /** The saved choice's app, no longer installed: listed so it can be seen, with no actions of its own. */
        boolean missing() { return kind == Kind.MISSING_APP; }
        String title(Context context) { return app() ? name : context.getString(label); }
    }

    static final List<Entry> BUILT_IN = Collections.unmodifiableList(Arrays.asList(
            new Entry(CLOCK, R.string.hb_ss_clock, R.string.hb_ss_clock_summary, R.string.hb_ss_clock_detail, "◷",
                    EnumSet.of(Option.BACKDROP, Option.BACKDROP_DIM), ClockScene::create),
            new Entry(HOME_BACKGROUND, R.string.hb_ss_background, R.string.hb_ss_background_summary,
                    R.string.hb_ss_background_detail, "▦", EnumSet.of(Option.SHOW_CLOCK, Option.DIM),
                    BackgroundScene::create)));

    private ScreensaverRegistry() { }

    /** The default slot: whatever the first entry is. */
    static Entry fallback() { return BUILT_IN.get(0); }

    static Entry find(String id) {
        for (Entry entry : BUILT_IN) if (entry.id.equals(id)) return entry;
        return null;
    }

    /** id when a built-in entry has it, otherwise the default slot's id: what Homeboard itself can play. */
    static String known(String id) { return find(id) == null ? fallback().id : id; }

    /**
     * What the saved choice keeps: a built-in id, or an Android app's flattened component, whose installation is
     * checked on read (resolve) because an app can be uninstalled at any time. Anything else is the default slot.
     */
    static String stored(String id) { return ScreensaverRules.isApp(id) ? id : known(id); }

    /** An Android screensaver app as a list entry; one with a preview screen says it also plays on Home. */
    static Entry app(ScreensaverGateway.InstalledDream dream) {
        boolean screen = dream.screen != null;
        return new Entry(dream.component, Kind.ANDROID_APP, 0, dream.label,
                screen ? R.string.hb_ss_app_screen_summary : R.string.hb_ss_app_summary,
                screen ? R.string.hb_ss_app_screen_detail : R.string.hb_ss_app_detail,
                "☾", EnumSet.noneOf(Option.class), null, dream.settings, dream.screen);
    }

    /**
     * The whole list: built-ins in order, then installed screensaver apps by label, without Homeboard's own
     * ScreensaverDream (its choices are the built-ins) and without duplicates.
     */
    static List<Entry> entries(List<ScreensaverGateway.InstalledDream> dreams, String ownComponent) {
        List<Entry> apps = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ScreensaverGateway.InstalledDream dream : dreams) {
            if (dream == null || !ScreensaverRules.isApp(dream.component) || dream.component.equals(ownComponent)) continue;
            if (seen.add(dream.component)) apps.add(app(dream));
        }
        apps.sort((a, b) -> {
            int byName = a.name.toLowerCase(Locale.ROOT).compareTo(b.name.toLowerCase(Locale.ROOT));
            return byName != 0 ? byName : a.id.compareTo(b.id);
        });
        List<Entry> all = new ArrayList<>(BUILT_IN);
        all.addAll(apps);
        return Collections.unmodifiableList(all);
    }

    /**
     * entries plus, when the saved choice is an app that is no longer installed, an unavailable row for it under its
     * saved name (or package), among the apps in name order. Home still plays the default slot for it: the row has no
     * scene and no preview screen. Unchanged when the choice is a built-in or installed.
     */
    static List<Entry> withSaved(List<Entry> entries, String choice, String choiceName) {
        if (!ScreensaverRules.isApp(choice)) return entries;
        for (Entry entry : entries) if (entry.id.equals(choice)) return entries;
        int slash = choice.indexOf('/');
        String name = choiceName != null && !choiceName.isEmpty() ? choiceName : choice.substring(0, slash);
        Entry gone = new Entry(choice, Kind.MISSING_APP, 0, name, R.string.hb_ss_not_installed, R.string.hb_ss_missing_detail,
                "☾", EnumSet.noneOf(Option.class), null, null, null);
        List<Entry> all = new ArrayList<>(entries);
        int at = all.size();
        for (int i = 0; i < all.size(); i++) {
            Entry e = all.get(i);
            if (e.app() && e.name.toLowerCase(Locale.ROOT).compareTo(name.toLowerCase(Locale.ROOT)) > 0) { at = i; break; }
        }
        all.add(at, gone);
        return Collections.unmodifiableList(all);
    }

    /** The entry for id in entries; the default slot when it is unknown or its app is no longer installed. */
    static Entry resolve(String id, List<Entry> entries) {
        for (Entry entry : entries) if (entry.id.equals(id)) return entry;
        return fallback();
    }

    static int indexOf(String id, List<Entry> entries) {
        for (int i = 0; i < entries.size(); i++) if (entries.get(i).id.equals(id)) return i;
        return 0;
    }

    /** The flattened components of the installed apps in entries. */
    static Set<String> installed(List<Entry> entries) {
        Set<String> found = new HashSet<>();
        for (Entry entry : entries) if (entry.kind == Kind.ANDROID_APP) found.add(entry.id);
        return found;
    }

    /**
     * The settings activity a screensaver declares in its android.service.dream metadata (settingsActivity), as a
     * flattened component: a "package/class" value is taken whole, a bare class belongs to the service's own
     * package, and a class starting with '.' is relative to it, as Android's own screensaver settings read it.
     */
    static String settingsComponent(String packageName, String declared) {
        if (declared == null || declared.trim().isEmpty() || packageName == null) return null;
        String value = declared.trim();
        if (value.indexOf('/') > 0) return value.endsWith("/") ? null : value;
        return packageName + "/" + (value.startsWith(".") ? packageName + value : value);
    }

    /**
     * The preview screen a Dream service declares in its meta-data (PREVIEW_ACTIVITY, PREVIEW_EXTRA, PREVIEW_VALUE),
     * or null when it declares none or an unusable one. The activity is always built in the Dream's own package, so
     * the metadata can never point Homeboard at another app: a value with a '/' or a leading '.' is refused, as is
     * one that is blank or not a dotted class name. The extra is sent only when both its name and value are given.
     */
    static ScreensaverGateway.AppScreen appScreen(String packageName, String activity, String extra, String value) {
        if (packageName == null || packageName.isEmpty() || packageName.indexOf('/') >= 0 || activity == null) return null;
        String name = activity.trim();
        if (name.isEmpty() || name.startsWith(".") || name.endsWith(".") || name.indexOf('/') >= 0) return null;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c != '.' && c != '$' && c != '_' && !Character.isLetterOrDigit(c)) return null;
        }
        boolean withExtra = extra != null && !extra.trim().isEmpty() && value != null && !value.isEmpty();
        return new ScreensaverGateway.AppScreen(packageName + "/" + name, withExtra ? extra.trim() : null, withExtra ? value : null);
    }

    /** The label for a component in entries; null when it is not an installed app's. */
    static String appLabel(String component, List<Entry> entries) {
        for (Entry entry : entries) if (entry.kind == Kind.ANDROID_APP && entry.id.equals(component)) return entry.name;
        return null;
    }

    static Entry entry(String id) { Entry found = find(id); return found == null ? fallback() : found; }

    static int indexOf(String id) {
        for (int i = 0; i < BUILT_IN.size(); i++) if (BUILT_IN.get(i).id.equals(id)) return i;
        return 0;
    }
}
