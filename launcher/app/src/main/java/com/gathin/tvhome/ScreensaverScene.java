package com.gathin.tvhome;

/**
 * One screensaver scene, played by ScreensaverActivity (Home idle, Preview, Show now) and by ClockDream (when
 * Android starts it). Built by a ScreensaverRegistry entry; start and stop follow the host's visibility and may
 * repeat, release frees what the scene holds and is the last call.
 */
interface ScreensaverScene {
    void start();
    void stop();
    void release();
}
