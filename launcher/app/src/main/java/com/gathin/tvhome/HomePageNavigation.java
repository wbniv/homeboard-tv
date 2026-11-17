package com.gathin.tvhome;

/** A blank page has the same place in the page sequence as a populated one. */
final class HomePageNavigation {
    static final int ALL_APPS = -1;
    static int next(int page, int pageCount, int direction) {
        int count = Math.max(1, pageCount);
        int current = Math.max(0, Math.min(page, count - 1));
        if (direction > 0) return current + 1 < count ? current + 1 : ALL_APPS;
        if (direction < 0) return Math.max(0, current - 1);
        return current;
    }
}
