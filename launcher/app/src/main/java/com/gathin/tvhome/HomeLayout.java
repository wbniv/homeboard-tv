package com.gathin.tvhome;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Persisted product state; page/focus and uncommitted move previews live in the activity. */
final class HomeLayout {
    static final String AIR = "widget:air";
    // Independent worktrees share an installed app. Preserve fields written by newer feature
    // branches (currently homeAppSize) when this branch saves known layout settings.
    private JSONObject additionalFields = new JSONObject();
    final List<String> widgets = new ArrayList<>();
    final List<String> order = new ArrayList<>();
    final Set<String> hidden = new HashSet<>();
    static final int PANEL_SMALL = 1, PANEL_MEDIUM = 2, PANEL_LARGE = 3;
    int defaultPanelSize = PANEL_MEDIUM;
    final Map<String, Integer> panelSizes = new LinkedHashMap<>();
    final Map<String, PanelSpan> panelSpans = new LinkedHashMap<>();
    final Map<String, HomePosition> homePositions = new LinkedHashMap<>();
    int homeColumns;
    int homeRows;
    boolean reducedMotion;
    boolean appBanners;
    boolean circularIcons = true;
    String customWallpaper = "";
    WallpaperSettings wallpaper = new WallpaperSettings();
    int uiOpacityPercent = 75;
    String palette = "midnight";

    static HomeLayout firstRun() {
        HomeLayout s = new HomeLayout();
        s.addPanel(AIR);
        return s;
    }

    static HomeLayout parse(String encoded) throws JSONException {
        JSONObject j = new JSONObject(encoded);
        HomeLayout s = new HomeLayout();
        s.additionalFields = new JSONObject(j.toString());
        for (String key : new String[]{"widgets", "order", "hidden", "defaultPanelSize", "panelSizes",
                "airWidth", "panelSpans", "homePositions", "homeColumns", "homeRows", "appBanners",
                "circularIcons", "customWallpaper", "reducedMotion", "palette", "wallpaper", "uiOpacityPercent"})
            s.additionalFields.remove(key);
        read(j.getJSONArray("widgets"), s.widgets);
        read(j.getJSONArray("order"), s.order);
        List<String> hidden = new ArrayList<>();
        read(j.getJSONArray("hidden"), hidden);
        s.hidden.addAll(hidden);
        s.defaultPanelSize = validPanelSize(j.optInt("defaultPanelSize", PANEL_MEDIUM));
        JSONObject sizes = j.optJSONObject("panelSizes");
        JSONArray sizeKeys = sizes == null ? null : sizes.names();
        if (sizeKeys != null) for (int i = 0; i < sizeKeys.length(); i++) {
            String key = sizeKeys.getString(i);
            if (AIR.equals(key) || key.startsWith("widget:"))
                s.panelSizes.put(key, validPanelSize(sizes.optInt(key, s.defaultPanelSize)));
        }
        // Migrate the original PM2.5 compact/wide setting to a per-panel size.
        if (!s.panelSizes.containsKey(AIR) && j.has("airWidth"))
            s.panelSizes.put(AIR, j.optInt("airWidth", 2) == 1 ? PANEL_SMALL : PANEL_MEDIUM);
        for (String key : s.widgets) if (key.startsWith("widget:"))
            s.panelSizes.putIfAbsent(key, s.defaultPanelSize);
        JSONObject spans = j.optJSONObject("panelSpans");
        JSONArray spanKeys = spans == null ? null : spans.names();
        if (spanKeys != null) for (int i = 0; i < spanKeys.length(); i++) {
            String key = spanKeys.optString(i);
            JSONObject span = spans.optJSONObject(key);
            if ((AIR.equals(key) || key.startsWith("widget:")) && span != null)
                s.setPanelSpan(key, span.optInt("width", 1), span.optInt("height", 1));
        }
        JSONObject positions = j.optJSONObject("homePositions");
        JSONArray positionKeys = positions == null ? null : positions.names();
        if (positionKeys != null) for (int i = 0; i < positionKeys.length(); i++) {
            String key = positionKeys.optString(i);
            JSONObject p = positions.optJSONObject(key);
            if (p != null && (key.startsWith("widget:") || key.startsWith("app:")))
                s.homePositions.put(key, new HomePosition(Math.max(0, p.optInt("page", 0)),
                        Math.max(0, p.optInt("row", 0)), Math.max(0, p.optInt("column", 0))));
        }
        int homeColumns = j.optInt("homeColumns", 0);
        s.homeColumns = homeColumns >= 4 && homeColumns <= 12 ? homeColumns : 0;
        int homeRows = j.optInt("homeRows", 0);
        s.homeRows = homeRows == 0 ? 0 : Math.max(2, homeRows);
        s.appBanners = j.optBoolean("appBanners", false);
        s.circularIcons = j.optBoolean("circularIcons", true);
        s.uiOpacityPercent = WallpaperSettings.percent(j.optInt("uiOpacityPercent", 75));
        s.wallpaper = WallpaperSettings.parse(j.optJSONObject("wallpaper"));
        s.customWallpaper = j.optString("customWallpaper", "");
        s.reducedMotion = j.optBoolean("reducedMotion", false);
        s.palette = j.optString("palette", "midnight");
        return s;
    }

    private static void read(JSONArray a, List<String> out) throws JSONException {
        for (int i = 0; i < a.length(); i++) {
            String key = a.getString(i);
            if ((key.startsWith("widget:") || key.startsWith("app:")) && !out.contains(key)) out.add(key);
        }
    }

    String encode() {
        try {
            return new JSONObject(additionalFields.toString()).put("widgets", new JSONArray(widgets)).put("order", new JSONArray(order))
                    .put("hidden", new JSONArray(new java.util.TreeSet<>(hidden)))
                    .put("defaultPanelSize", defaultPanelSize).put("panelSizes", new JSONObject(panelSizes))
                    .put("panelSpans", encodedSpans())
                    .put("homePositions", encodedHomePositions())
                    .put("homeColumns", homeColumns).put("homeRows", homeRows)
                    .put("appBanners", appBanners).put("circularIcons", circularIcons)
                    .put("customWallpaper", customWallpaper)
                    .put("wallpaper", wallpaper.json()).put("uiOpacityPercent", uiOpacityPercent)
                    .put("reducedMotion", reducedMotion).put("palette", palette).toString();
        } catch (JSONException e) { throw new IllegalStateException(e); }
    }

    void favorite(String key) { if (!widgets.contains(key)) widgets.add(key); }
    void addPanel(String key) {
        if (!key.startsWith("widget:")) throw new IllegalArgumentException("Panel keys must start with widget:");
        favorite(key);
        panelSizes.putIfAbsent(key, defaultPanelSize);
    }
    int panelSize(String key) { return validPanelSize(panelSizes.containsKey(key) ? panelSizes.get(key) : defaultPanelSize); }
    void setPanelSize(String key, int size) { panelSizes.put(key, validPanelSize(size)); }
    PanelSpan panelSpan(String key) { return panelSpans.get(key); }
    void setPanelSpan(String key, int width, int height) {
        panelSpans.put(key, new PanelSpan(Math.max(1, Math.min(12, width)), Math.max(1, Math.min(12, height))));
    }
    private JSONObject encodedSpans() throws JSONException {
        JSONObject result = new JSONObject();
        for (Map.Entry<String, PanelSpan> entry : panelSpans.entrySet())
            result.put(entry.getKey(), new JSONObject().put("width", entry.getValue().width).put("height", entry.getValue().height));
        return result;
    }
    private JSONObject encodedHomePositions() throws JSONException {
        JSONObject result = new JSONObject();
        for (Map.Entry<String, HomePosition> entry : homePositions.entrySet()) {
            HomePosition p = entry.getValue();
            result.put(entry.getKey(), new JSONObject().put("page", p.page).put("row", p.row).put("column", p.column));
        }
        return result;
    }
    private static int validPanelSize(int size) { return size < PANEL_SMALL || size > PANEL_LARGE ? PANEL_MEDIUM : size; }
    void delete(String key, boolean dashboard) {
        if (dashboard) { widgets.remove(key); panelSpans.remove(key); homePositions.remove(key); }
        else hidden.add(key);
    }
    List<String> visibleApps(List<String> installed) {
        List<String> result = new ArrayList<>();
        for (String key : order) if (installed.contains(key) && !hidden.contains(key)) result.add(key);
        for (String key : installed) if (!hidden.contains(key) && !result.contains(key)) result.add(key);
        return result;
    }
    /** Collapse multiple launch activities from one installed app into its chosen single tile. */
    boolean canonicalizeAppPackage(String packageName, String chosenKey) {
        String prefix = "app:" + packageName + "/";
        boolean changed = canonicalizeKeys(widgets, prefix, chosenKey);
        changed |= canonicalizeKeys(order, prefix, chosenKey);
        int hiddenPackageCount = 0;
        for (String key : hidden) if (key.startsWith(prefix)) hiddenPackageCount++;
        if (hiddenPackageCount > 0 && (hiddenPackageCount > 1 || !hidden.contains(chosenKey))) {
            hidden.removeIf(key -> key.startsWith(prefix));
            hidden.add(chosenKey);
            changed = true;
        }
        return changed;
    }
    private static boolean canonicalizeKeys(List<String> keys, String prefix, String chosenKey) {
        boolean found = false, changed = false;
        List<String> canonical = new ArrayList<>();
        for (String key : keys) {
            if (key.startsWith(prefix)) {
                if (found) { changed = true; continue; }
                if (!key.equals(chosenKey)) changed = true;
                canonical.add(chosenKey);
                found = true;
            } else canonical.add(key);
        }
        if (changed) { keys.clear(); keys.addAll(canonical); }
        return changed;
    }
    // Only a confirmed package removal invokes this. A missing launcher activity alone is not uninstall.
    void packageRemoved(String packageName) {
        String prefix = "app:" + packageName + "/";
        widgets.removeIf(k -> k.startsWith(prefix));
        order.removeIf(k -> k.startsWith(prefix));
        hidden.removeIf(k -> k.startsWith(prefix));
        panelSpans.keySet().removeIf(k -> k.startsWith(prefix));
        homePositions.keySet().removeIf(k -> k.startsWith(prefix));
    }
    static void move(List<String> items, int from, int to) {
        if (from < 0 || from >= items.size() || to < 0 || to >= items.size()) return;
        items.add(to, items.remove(from));
    }

    static final class Cell {
        final String key;
        final int page, row, column, width, height;
        Cell(String key, int page, int row, int column, int width, int height) {
            this.key = key; this.page = page; this.row = row; this.column = column;
            this.width = width; this.height = height;
        }
    }
    static final class PanelSpan {
        final int width, height;
        PanelSpan(int width, int height) { this.width = width; this.height = height; }
    }
    static final class HomePosition {
        final int page, row, column;
        HomePosition(int page, int row, int column) { this.page = page; this.row = row; this.column = column; }
    }
    List<Cell> cells(List<String> items, int columns, int rows) {
        return cells(items, columns, rows, homePositions);
    }
    List<Cell> cells(List<String> items, int columns, int rows, Map<String, HomePosition> positions) {
        List<Cell> result = new ArrayList<>();
        if (columns < 1 || rows < 1) return result;
        if (positions != null && !positions.isEmpty()) return positionedCells(items, columns, rows, positions);
        int page = 0, cursor = 0;
        boolean[][] occupied = new boolean[rows][columns];
        for (String key : items) {
            int smallSpan = Math.max(2, columns / 3);
            int mediumSpan = Math.max(2, (columns + 1) / 2);
            int largeSpan = Math.max(mediumSpan + 1, (columns * 3 + 3) / 4);
            int size = panelSize(key);
            int width = key.startsWith("widget:")
                    ? size == PANEL_SMALL ? smallSpan : size == PANEL_MEDIUM ? mediumSpan : largeSpan : 1;
            int height = key.startsWith("widget:") ? (size == PANEL_LARGE ? Math.min(3, rows) : Math.min(2, rows)) : 1;
            PanelSpan exact = panelSpans.get(key);
            if (key.startsWith("widget:") && exact != null) { width = exact.width; height = exact.height; }
            width = Math.min(width, columns);
            height = Math.min(height, rows);
            int position = -1;
            while (position < 0) {
                for (int slot = cursor; slot < rows * columns; slot++) {
                    int row = slot / columns, column = slot % columns;
                    if (column + width > columns || row + height > rows) continue;
                    boolean free = true;
                    for (int y = row; y < row + height && free; y++)
                        for (int x = column; x < column + width; x++) if (occupied[y][x]) { free = false; break; }
                    if (free) { position = slot; break; }
                }
                if (position < 0) {
                    page++; cursor = 0; occupied = new boolean[rows][columns];
                }
            }
            int row = position / columns, column = position % columns;
            for (int y = row; y < row + height; y++)
                for (int x = column; x < column + width; x++) occupied[y][x] = true;
            result.add(new Cell(key, page, row, column, width, height));
            cursor = position + 1;
        }
        return result;
    }
    private List<Cell> positionedCells(List<String> items, int columns, int rows,
                                       Map<String, HomePosition> positions) {
        List<Cell> result = new ArrayList<>();
        Map<String, Cell> placed = new LinkedHashMap<>();
        List<boolean[][]> occupied = new ArrayList<>();
        int slots = columns * rows;
        int cursor = 0;
        // Explicit positions are considered in map order. The moving tile is inserted first
        // in the preview map, so it claims its requested target and only overlapping tiles shift.
        for (Map.Entry<String, HomePosition> entry : positions.entrySet()) {
            String key = entry.getKey();
            if (!items.contains(key) || placed.containsKey(key)) continue;
            int[] span = span(key, columns, rows);
            HomePosition requested = entry.getValue();
            int page = requested.page, row = requested.row, column = requested.column;
            if (!fits(occupied, page, row, column, span[0], span[1], columns, rows)) {
                int requestedSlot = Math.max(0, page * slots + row * columns + column);
                int[] free = firstFree(occupied, requestedSlot, span[0], span[1], columns, rows);
                page = free[0]; row = free[1]; column = free[2];
            }
            occupy(occupied, page, row, column, span[0], span[1], columns, rows);
            placed.put(key, new Cell(key, page, row, column, span[0], span[1]));
            cursor = Math.max(cursor, page * slots + row * columns + column + 1);
        }
        cursor = 0;
        for (String key : items) {
            if (placed.containsKey(key)) continue;
            int[] span = span(key, columns, rows);
            int[] free = firstFree(occupied, cursor, span[0], span[1], columns, rows);
            occupy(occupied, free[0], free[1], free[2], span[0], span[1], columns, rows);
            placed.put(key, new Cell(key, free[0], free[1], free[2], span[0], span[1]));
            cursor = free[0] * slots + free[1] * columns + free[2] + 1;
        }
        for (String key : items) if (placed.containsKey(key)) result.add(placed.get(key));
        return result;
    }
    private int[] span(String key, int columns, int rows) {
        int size = panelSize(key);
        int width = key.startsWith("widget:")
                ? size == PANEL_SMALL ? Math.max(2, columns / 3)
                : size == PANEL_MEDIUM ? Math.max(2, (columns + 1) / 2)
                : Math.max(Math.max(2, (columns + 1) / 2) + 1, (columns * 3 + 3) / 4) : 1;
        int height = key.startsWith("widget:") ? (size == PANEL_LARGE ? Math.min(3, rows) : Math.min(2, rows)) : 1;
        PanelSpan exact = panelSpans.get(key);
        if (key.startsWith("widget:") && exact != null) { width = exact.width; height = exact.height; }
        return new int[]{Math.min(width, columns), Math.min(height, rows)};
    }
    private static boolean fits(List<boolean[][]> occupied, int page, int row, int column,
                                int width, int height, int columns, int rows) {
        if (page < 0 || row < 0 || column < 0 || row + height > rows || column + width > columns) return false;
        if (page >= occupied.size()) return true;
        boolean[][] pageGrid = occupied.get(page);
        for (int y = row; y < row + height; y++) for (int x = column; x < column + width; x++)
            if (pageGrid[y][x]) return false;
        return true;
    }
    private static void occupy(List<boolean[][]> occupied, int page, int row, int column,
                               int width, int height, int columns, int rows) {
        while (occupied.size() <= page) occupied.add(new boolean[rows][columns]);
        for (int y = row; y < row + height; y++) for (int x = column; x < column + width; x++)
            occupied.get(page)[y][x] = true;
    }
    private static int[] firstFree(List<boolean[][]> occupied, int startSlot, int width, int height,
                                   int columns, int rows) {
        int slots = columns * rows;
        int maxPages = Math.max(occupied.size() + 4, startSlot / slots + 2);
        int maxSlots = maxPages * slots;
        for (int slot = startSlot; slot < maxSlots; slot++) {
            int page = slot / slots, inPage = slot % slots, row = inPage / columns, column = inPage % columns;
            if (fits(occupied, page, row, column, width, height, columns, rows)) return new int[]{page, row, column};
        }
        int page = Math.max(0, maxSlots / slots);
        return new int[]{page, 0, 0};
    }
    int pageOf(String key, List<String> items, int columns, int rows) {
        for (Cell c : cells(items, columns, rows)) if (c.key.equals(key)) return c.page;
        return 0;
    }
    int pageCount(List<String> items, int columns, int rows) {
        List<Cell> cells = cells(items, columns, rows);
        int pages = 0;
        for (Cell cell : cells) pages = Math.max(pages, cell.page + 1);
        return Math.max(1, pages);
    }
}
