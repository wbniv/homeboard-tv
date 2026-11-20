package com.gathin.tvhome;

import android.content.Context;
import android.widget.FrameLayout;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The screensavers Homeboard can play. Stage 1 holds the built-in entries only; the first row is the default
 * slot, so a better bundled screensaver later is one more row placed first, with no change to the Screensaver
 * screen, the settings or the scenes.
 */
final class ScreensaverRegistry {
    static final String CLOCK = "clock", HOME_BACKGROUND = "background";

    /** The options an entry's Options screen offers. "Start on Home after" applies to every entry. */
    enum Option { BACKDROP, BACKDROP_DIM, SHOW_CLOCK, DIM }

    interface SceneFactory {
        /** Builds the scene inside container (empty, full screen) from layout's screensaver options and background. */
        ScreensaverScene create(Context context, FrameLayout container, HomeLayout layout);
    }

    static final class Entry {
        final String id;
        final int label, summary, description;
        /** A one-character glyph for the list card, like the menu icons. */
        final String glyph;
        final Set<Option> options;
        final SceneFactory scene;
        Entry(String id, int label, int summary, int description, String glyph, Set<Option> options, SceneFactory scene) {
            this.id = id; this.label = label; this.summary = summary; this.description = description;
            this.glyph = glyph; this.options = Collections.unmodifiableSet(options); this.scene = scene;
        }
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

    /** id when an entry has it, otherwise the default slot's id. */
    static String known(String id) { return find(id) == null ? fallback().id : id; }

    static Entry entry(String id) { Entry found = find(id); return found == null ? fallback() : found; }

    static int indexOf(String id) {
        for (int i = 0; i < BUILT_IN.size(); i++) if (BUILT_IN.get(i).id.equals(id)) return i;
        return 0;
    }
}
