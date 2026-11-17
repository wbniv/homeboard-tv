package com.gathin.tvhome;

/** Every picker entry requires a compact and expanded presentation. */
enum WidgetKind {
    AIR(HomeLayout.AIR), CALENDAR(HomeLayout.CALENDAR), RAIN(HomeLayout.RAIN), FAVORITES(HomeLayout.FAVORITES);
    final String key;
    WidgetKind(String key) { this.key = key; }
    static WidgetKind of(String key) {
        for (WidgetKind kind : values())
            if (kind.key.equals(key) || (kind == AIR || kind == RAIN) && key != null && key.startsWith(kind.key + ":")) return kind;
        throw new IllegalArgumentException("Unknown widget: " + key);
    }
}
