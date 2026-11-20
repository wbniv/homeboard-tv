package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.List;

/**
 * Android's enabled_accessibility_services setting as a list: colon-separated "package/class" entries, where a class
 * that starts with '.' is relative to its package. Pure string logic, so every edit is unit tested. Edits touch only
 * the named component: every other entry keeps its place and its spelling, so another app's service is never
 * rewritten. Empty entries and the literal "null" some firmware stores are dropped.
 */
final class AccessibilityList {
    private AccessibilityList() { }

    /** The entries, in order, without empty ones or "null". A null or empty setting is an empty list. */
    static List<String> entries(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) return out;
        for (String part : raw.split(":")) {
            String entry = part.trim();
            if (entry.isEmpty() || entry.equals("null")) continue;
            out.add(entry);
        }
        return out;
    }

    /** "pkg/.Cls" and "pkg/pkg.Cls" name the same service; this returns the second form. */
    static String normalize(String entry) {
        if (entry == null) return "";
        String value = entry.trim();
        int slash = value.indexOf('/');
        if (slash <= 0 || slash == value.length() - 1) return value;
        String pkg = value.substring(0, slash), cls = value.substring(slash + 1);
        return pkg + "/" + (cls.startsWith(".") ? pkg + cls : cls);
    }

    static boolean same(String a, String b) {
        return normalize(a).equals(normalize(b));
    }

    static boolean contains(String raw, String component) {
        for (String entry : entries(raw)) if (same(entry, component)) return true;
        return false;
    }

    /** The list with the component once: kept at its first place if present (duplicates of it dropped), else appended. */
    static String with(String raw, String component) {
        List<String> out = new ArrayList<>();
        boolean present = false;
        for (String entry : entries(raw)) {
            if (same(entry, component)) {
                if (present) continue;
                present = true;
            }
            out.add(entry);
        }
        if (!present) out.add(component);
        return String.join(":", out);
    }

    /** The list without the component (every spelling and duplicate of it); the empty string when nothing is left. */
    static String without(String raw, String component) {
        List<String> out = new ArrayList<>();
        for (String entry : entries(raw)) if (!same(entry, component)) out.add(entry);
        return String.join(":", out);
    }
}
