package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Screensaver chooser's browsing state and decisions, free of Android so they are unit tested
 * (docs/plans/2026-11-26-screensaver-browsing.md, "Navigation and state" and "Supported actions and edge states").
 * MainActivity draws one continuous list from these answers: which row is browsed, where the list was scrolled,
 * which action to focus on return, which actions an entry shows, and which scope badge each row carries.
 */
final class ScreensaverPicker {
    private ScreensaverPicker() { }

    /**
     * What the person was doing in the chooser, kept across Preview, Options, an app's settings screen, Use
     * everywhere and Activity recreation. Fresh openings start at the saved choice (reset).
     */
    static final class State {
        /** The browsed entry's id (the focus ring), not necessarily the saved choice. */
        String browsed;
        /** Its last known position, for the nearest entry when it disappears. */
        int browsedIndex;
        /** The first visible row's id and its top offset in pixels, so a return restores the scroll exactly. */
        String anchor;
        int anchorOffset;
        /** The tag of the last action used ("act:preview", "act:options", …), focused again on return. */
        String action;
        /** A return from an action is pending: the next draw focuses action instead of the row. */
        boolean returning;
        /** Opened from Home settings (Back goes there) rather than by holding OK on Start screensaver. */
        boolean fromHomeSettings;

        void reset(String chosen, int chosenIndex, boolean fromHomeSettings) {
            browsed = chosen; browsedIndex = chosenIndex; anchor = null; anchorOffset = 0; action = null;
            returning = false; this.fromHomeSettings = fromHomeSettings;
        }

        /** An action is about to leave or redraw the list: remember it, so the return focuses it again. */
        void used(String actionTag) { action = actionTag; returning = true; }

        /** The tag the next draw focuses: the pending action on a return, otherwise null (the browsed row). */
        String takeReturnFocus() {
            String tag = returning ? action : null;
            returning = false;
            return tag;
        }

        State copy() {
            State s = new State();
            s.browsed = browsed; s.browsedIndex = browsedIndex; s.anchor = anchor; s.anchorOffset = anchorOffset;
            s.action = action; s.returning = returning; s.fromHomeSettings = fromHomeSettings;
            return s;
        }
    }

    static int indexOf(String id, List<ScreensaverRegistry.Entry> entries) {
        if (id != null) for (int i = 0; i < entries.size(); i++) if (entries.get(i).id.equals(id)) return i;
        return -1;
    }

    /**
     * The row to browse in entries: id's row, or, when that entry has gone (an app uninstalled while the chooser
     * was open), the nearest remaining row to where it was. Never snaps to the first row for no reason.
     */
    static int browsedRow(String id, int lastIndex, List<ScreensaverRegistry.Entry> entries) {
        if (entries.isEmpty()) return -1;
        int found = indexOf(id, entries);
        return found >= 0 ? found : Math.max(0, Math.min(entries.size() - 1, lastIndex));
    }

    /** Where Up or Down from a row goes. */
    enum Move { ROW, HEADER, STAY }

    /** Up on the first row goes to the header's Back; Down on the last row stays; nothing wraps. */
    static Move move(int index, int size, int delta) {
        if (delta < 0 && index <= 0) return Move.HEADER;
        if (delta > 0 && index >= size - 1) return Move.STAY;
        return Move.ROW;
    }

    /** The row a ROW move lands on. */
    static int target(int index, int size, int delta) {
        return Math.max(0, Math.min(size - 1, index + delta));
    }

    /** The actions an entry offers, in order; unsupported ones are absent, never shown as dead ends. */
    enum Action {
        /** Use this screensaver (on Home, or everywhere once managed). */
        USE("act:use"),
        /** Use everywhere…: an app only Android can run, before Homeboard manages the TV. */
        EVERYWHERE("act:use"),
        PREVIEW("act:preview"),
        /** A built-in entry's look options. */
        OPTIONS("act:options"),
        /** Open ‹app› settings: the app's own declared settings activity. */
        SETTINGS("act:settings");
        final String tag;
        Action(String tag) { this.tag = tag; }
    }

    /**
     * inUseOnHome: the entry is Home's saved choice, which needs no Use. A built-in keeps Preview and Options; an
     * app with its own preview screen keeps Preview and its settings link; an Android-only app offers Use everywhere
     * and its settings link when it has one.
     */
    static List<Action> actions(ScreensaverRegistry.Entry entry, boolean managed, boolean inUseOnHome) {
        List<Action> out = new ArrayList<>();
        if (entry.missing()) return Collections.unmodifiableList(out);   // not installed: nothing to preview or use
        if (!inUseOnHome) out.add(ScreensaverRules.use(managed, entry.id, entry.playsOnHome()) == ScreensaverRules.Use.EVERYWHERE_SCREEN
                ? Action.EVERYWHERE : Action.USE);
        if (entry.app()) {
            if (entry.playsOnHome()) out.add(Action.PREVIEW);
            if (entry.settings != null) out.add(Action.SETTINGS);
        } else {
            out.add(Action.PREVIEW);
            out.add(Action.OPTIONS);
        }
        return Collections.unmodifiableList(out);
    }

    /** The saved-choice badge on a row; the focus ring is separate and means only "browsing this". */
    enum Badge { IN_USE, ON_HOME, IN_OTHER_APPS }

    /**
     * The entry Android starts in other apps, as a list id: Homeboard's own screensaver plays Home's built-in
     * choice (or the default slot when Home uses an app); an installed app is its own entry. null when Android's
     * setting cannot be read, is unset, or names something not in the list.
     */
    static String otherAppsEntry(ScreensaverGateway.Reading component, String ownComponent, String homeChoice,
                                 List<ScreensaverRegistry.Entry> entries) {
        if (component == null || !component.readable) return null;
        String first = ScreensaverRules.first(component.value);
        if (first == null) return null;
        if (first.equals(ownComponent)) return ScreensaverRegistry.known(homeChoice);
        return indexOf(first, entries) >= 0 ? first : null;
    }

    /**
     * One "In use" badge when Home and other apps use the same entry; otherwise "On Home" and "In other apps" on
     * their own rows, so two ambiguous "In use" badges never appear.
     */
    static Map<String, Badge> badges(String homeId, String otherId) {
        Map<String, Badge> out = new HashMap<>();
        if (homeId != null && homeId.equals(otherId)) { out.put(homeId, Badge.IN_USE); return out; }
        if (homeId != null) out.put(homeId, otherId == null ? Badge.IN_USE : Badge.ON_HOME);
        if (otherId != null) out.put(otherId, Badge.IN_OTHER_APPS);
        return out;
    }

    /** What Change under When it starts → On Home opens. */
    enum OptionsView {
        /** A built-in's Options: its look rows, Preview, then When it starts. */
        ENTRY_OPTIONS,
        /** When it starts alone: Home's choice is an app, which has no Homeboard look options. */
        WHEN_ONLY
    }

    /** The groups of rows an options screen shows. */
    enum Group { LOOK, PREVIEW, WHEN }

    /** chosen: Home's saved choice as the list resolves it. Folded into the list rework by Will, 26 November. */
    static OptionsView changeOpens(ScreensaverRegistry.Entry chosen) {
        return chosen != null && chosen.app() ? OptionsView.WHEN_ONLY : OptionsView.ENTRY_OPTIONS;
    }

    static Set<Group> groups(OptionsView view) {
        return view == OptionsView.WHEN_ONLY ? EnumSet.of(Group.WHEN) : EnumSet.allOf(Group.class);
    }

    /**
     * The non-focusable scroll indicator's thumb, as {top, height} in track pixels, from RecyclerView's
     * computeVerticalScroll* values; null when everything fits (two built-ins alone need no scrollbar).
     */
    static int[] thumb(int offset, int extent, int range, int track, int minThumb) {
        if (range <= extent || extent <= 0 || track <= 0) return null;
        int height = Math.max(Math.min(minThumb, track), Math.round((float) track * extent / range));
        int travel = track - height;
        int top = Math.round(travel * Math.max(0f, Math.min(1f, (float) offset / (range - extent))));
        return new int[]{top, height};
    }

    /** A stable RecyclerView id for an entry id (a component or built-in id): 64-bit FNV-1a. */
    static long stableId(String id) {
        long hash = 0xcbf29ce484222325L;
        for (int i = 0; i < id.length(); i++) { hash ^= id.charAt(i); hash *= 0x100000001b3L; }
        return hash;
    }
}
