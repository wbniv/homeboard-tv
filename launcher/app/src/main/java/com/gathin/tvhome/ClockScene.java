package com.gathin.tvhome;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.Random;

/**
 * The Clock screensaver (layout dream_clock): the thin clock and date over a slow ambient glow, on plain black,
 * or with nothing behind it so a BackgroundScene's Home background shows through. Every minute the clock drifts
 * to a new spot and the glow shifts colour, so nothing burns in; a ClockRefresh keeps its TextClocks current
 * where Android would not. Options come from ScreensaverSettings.
 */
final class ClockScene implements ScreensaverScene {

    /** What is drawn behind the clock. CLEAR draws nothing: the caller has put something behind the layout. */
    enum Backdrop { GLOW, BLACK, CLEAR }

    private static final long MOVE_MS = 60_000L;
    /** The glow's colour change: 16 steps over 8 s. Animated per frame it redrew the full-screen gradient ~480 times. */
    private static final int GLOW_STEPS = 16;
    private static final long GLOW_STEP_MS = 500L;
    /** Deep, low-brightness glow colours; the glow is drawn at partial alpha on black. */
    private static final int[] PALETTE = {0xFF2B5CFF, 0xFF00A39A, 0xFF8A3FFC, 0xFFE0661B, 0xFFD6336C};

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final View content;
    private final ClockRefresh clockRefresh;
    private final GradientDrawable glow;
    private final boolean glowing;
    private int colorIndex;
    private int glowColor;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            moveClock();
            shiftGlow();
            handler.postDelayed(this, MOVE_MS);
        }
    };

    /** The registry's factory for the Clock entry: its Home background backdrop is the Home background scene with the clock on. */
    static ScreensaverScene create(Context context, FrameLayout container, HomeLayout layout) {
        ScreensaverSettings settings = layout.screensaver;
        if (ScreensaverSettings.BACKDROP_HOME.equals(settings.clockBackdrop))
            return new BackgroundScene(context, container, layout, settings.clockDimPercent, true);
        return new ClockScene(inflate(context, container),
                ScreensaverSettings.BACKDROP_BLACK.equals(settings.clockBackdrop) ? Backdrop.BLACK : Backdrop.GLOW);
    }

    /** Inflates dream_clock into container and returns its root. */
    static View inflate(Context context, FrameLayout container) {
        return LayoutInflater.from(context).inflate(R.layout.dream_clock, container, true);
    }

    /** root holds the inflated dream_clock layout. */
    ClockScene(View root, Backdrop backdrop) {
        content = root.findViewById(R.id.dreamContent);
        clockRefresh = new ClockRefresh(content);
        ImageView ambient = root.findViewById(R.id.dreamAmbient);
        View layoutRoot = (View) ambient.getParent();
        glowing = backdrop == Backdrop.GLOW;
        glow = new GradientDrawable();
        glow.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        if (backdrop == Backdrop.CLEAR) layoutRoot.setBackground(null);
        if (glowing) {
            ambient.setBackground(glow);
            ambient.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) ->
                    glow.setGradientRadius((r - l) * 0.55f));
            colorIndex = random.nextInt(PALETTE.length);
            setGlow(PALETTE[colorIndex]);
        } else {
            ambient.setBackground(null);   // BLACK: plain black for OLED panels; the clock still moves every minute
        }
    }

    @Override
    public void start() {
        content.setAlpha(0f);
        content.post(() -> {
            placeClock();
            content.animate().alpha(1f).setDuration(1200).start();
        });
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, MOVE_MS);
        clockRefresh.start();
    }

    @Override
    public void stop() {
        handler.removeCallbacksAndMessages(null);   // the tick and any glow steps still queued
        clockRefresh.stop();
        content.animate().cancel();
    }

    @Override
    public void release() { stop(); }

    private void moveClock() {
        content.animate().alpha(0f).setDuration(900).withLayer().withEndAction(() -> {
            placeClock();
            content.animate().alpha(1f).setDuration(1200).withLayer().start();
        }).start();
    }

    /** A random spot that keeps the whole clock on screen, with the glow centred behind it. */
    private void placeClock() {
        View parent = (View) content.getParent();
        int maxX = Math.max(0, parent.getWidth() - content.getWidth());
        int maxY = Math.max(0, parent.getHeight() - content.getHeight());
        float x = maxX * (0.1f + 0.8f * random.nextFloat());
        float y = maxY * (0.1f + 0.8f * random.nextFloat());
        content.setX(x);
        content.setY(y);
        if (glowing && parent.getWidth() > 0 && parent.getHeight() > 0) {
            glow.setGradientCenter((x + content.getWidth() / 2f) / parent.getWidth(),
                    (y + content.getHeight() / 2f) / parent.getHeight());
        }
    }

    private void shiftGlow() {
        if (!glowing) return;
        int from = glowColor;
        colorIndex = (colorIndex + 1) % PALETTE.length;
        int to = PALETTE[colorIndex];
        ArgbEvaluator mix = new ArgbEvaluator();
        for (int i = 1; i <= GLOW_STEPS; i++) {
            float f = (float) i / GLOW_STEPS;
            handler.postDelayed(() -> setGlow((int) mix.evaluate(f, from, to)), i * GLOW_STEP_MS);
        }
    }

    private void setGlow(int color) {
        glowColor = color;
        int c = color & 0x00FFFFFF;
        glow.setColors(new int[]{0x40000000 | c, 0x14000000 | c, 0x00000000 | c});
    }
}
