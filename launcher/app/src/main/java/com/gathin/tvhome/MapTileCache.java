package com.gathin.tvhome;

import java.util.HashSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** UI-thread cache/admission with an immutable visibility snapshot for workers. */
final class MapTileCache<T> {
    static final int RECENT_TILES = 40, MAX_PENDING = 32;
    private final LinkedHashMap<String,T> tiles = new LinkedHashMap<>(16, .75f, true);
    private final Set<String> pending = new HashSet<>();
    private volatile Set<String> visible = Collections.emptySet();

    void visible(Set<String> keys) { visible = Collections.unmodifiableSet(new HashSet<>(keys)); trim(); }
    boolean isVisible(String key) { return visible.contains(key); }
    T get(String key) { return tiles.get(key); }
    boolean beginLoad(String key) {
        if (!visible.contains(key) || pending.contains(key) || pending.size() >= MAX_PENDING) return false;
        pending.add(key); return true;
    }
    void publish(String key, T tile) {
        if (tile != null && visible.contains(key)) { tiles.put(key, tile); trim(); }
    }
    void completeLoad(String key, T tile) { pending.remove(key); publish(key, tile); }
    boolean loading() { return !pending.isEmpty(); }
    int size() { return tiles.size(); }
    int pendingCount() { return pending.size(); }
    void clear() { tiles.clear(); pending.clear(); visible = Collections.emptySet(); }
    private void trim() {
        int limit = Math.max(RECENT_TILES, visible.size());
        Iterator<Map.Entry<String,T>> entries = tiles.entrySet().iterator();
        while (tiles.size() > limit && entries.hasNext()) {
            if (!visible.contains(entries.next().getKey())) entries.remove();
        }
    }
}
