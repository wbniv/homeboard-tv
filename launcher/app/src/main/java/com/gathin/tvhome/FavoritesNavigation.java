package com.gathin.tvhome;

/** Pure list navigation: no row wrapping; -1 hands focus back to the outer Home grid. */
final class FavoritesNavigation {
    static int next(int index, int count, int columns, boolean horizontal, int dx, int dy) {
        if (index < 0 || index >= count) return -1;
        int next;
        if (horizontal) { if (dy != 0) return -1; next=index+dx; }
        else {
            if (dx < 0 && index % columns == 0 || dx > 0 && index % columns == columns-1) return -1;
            next=index+dx+dy*columns;
            if (dy > 0 && next >= count && index/columns < (count-1)/columns) next=count-1;
        }
        return next >= 0 && next < count ? next : -1;
    }
}
