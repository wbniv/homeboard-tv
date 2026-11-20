package com.gathin.tvhome;

import android.content.Context;
import android.widget.FrameLayout;

/**
 * The Home background screensaver: Homeboard's own current background (HomeBackdrop), full screen and dimmed,
 * with the drifting clock optionally on top. Also serves the Clock entry's Home background backdrop.
 */
final class BackgroundScene implements ScreensaverScene {
    private final HomeBackdrop backdrop;
    private final ClockScene clock;

    /** The registry's factory for the Home background entry. */
    static ScreensaverScene create(Context context, FrameLayout container, HomeLayout layout) {
        ScreensaverSettings settings = layout.screensaver;
        return new BackgroundScene(context, container, layout, settings.backgroundDimPercent, settings.backgroundShowsClock);
    }

    BackgroundScene(Context context, FrameLayout container, HomeLayout layout, int dimPercent, boolean showClock) {
        clock = showClock ? new ClockScene(ClockScene.inflate(context, container), ClockScene.Backdrop.CLEAR) : null;
        backdrop = new HomeBackdrop(context, container, layout, dimPercent);
    }

    @Override
    public void start() {
        backdrop.start();
        if (clock != null) clock.start();
    }

    @Override
    public void stop() {
        if (clock != null) clock.stop();
        backdrop.stop();
    }

    @Override
    public void release() {
        if (clock != null) clock.release();
        backdrop.release();
    }
}
