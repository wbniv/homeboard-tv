package com.gathin.tvhome;

import android.content.Context;
import android.widget.FrameLayout;

/**
 * Homeboard's own background, for a screensaver: the saved provider (layout.wallpaper) played through the same
 * WallpaperSurface and ProjectivyWallpaperHost Home uses, muted, or Home's solid palette colour, darkened by the
 * screensaver's own dim. Home's settings are read, never written; Home's own host is disconnected while Home is
 * not visible, so only this one is bound.
 */
final class HomeBackdrop {
    private final WallpaperSurface surface;
    private final ProjectivyWallpaperHost host;

    /** Adds itself to container, behind anything already in it. */
    HomeBackdrop(Context context, FrameLayout container, HomeLayout layout, int dimPercent) {
        container.setBackgroundColor(MainActivity.paletteColorForId(layout.palette));
        surface = new WallpaperSurface(context);
        surface.options(dimPercent, layout.reducedMotion);
        container.addView(surface, 0, new FrameLayout.LayoutParams(-1, -1));
        if (layout.wallpaper.solid()) { host = null; return; }
        ProjectivyWallpaperHost[] self = new ProjectivyWallpaperHost[1];
        host = self[0] = new ProjectivyWallpaperHost(context, new ProjectivyWallpaperHost.Listener() {
            @Override public void wallpaper(tv.projectivy.plugin.wallpaperprovider.api.Wallpaper item) {
                surface.show(item, (ready, status) -> { if (!ready && self[0] != null) self[0].mediaFailed(status); });
            }
            @Override public void status(String message) { }
            @Override public void fallback() { surface.solid(); }
        });
        host.select(layout.wallpaper);
    }

    void start() { surface.visible(true); if (host != null) host.visible(true); }

    void stop() { if (host != null) host.visible(false); surface.visible(false); }

    void release() { if (host != null) host.close(); surface.close(); }
}
