package com.gathin.tvhome;

import android.app.ActivityManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.airbnb.lottie.LottieAnimationView;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import org.json.JSONArray;
import org.json.JSONObject;
import tv.projectivy.plugin.wallpaperprovider.api.Wallpaper;

/**
 * Debug-only 4K wallpaper oracle (docs/plans/2026-11-28-4k-wallpapers-and-video.md). Runs on the real native
 * renderer when the TV qualifies, shows the debug fixture's 3840 x 2160 media through the real WallpaperSurface,
 * and reads the pixels back: a one-pixel stripe pattern survives only a full-resolution path. Holds each stage
 * for a fixed window so coordinator PSS samples can be lined up with it. Never touches the household layout.
 * The .AnimatedWallpaperProbe alias (meta-data mode=animated) runs the animated-source matrix instead
 * (docs/plans/2026-11-28-animated-wallpapers-from-large-sources.md).
 */
@android.annotation.TargetApi(30)
public final class WallpaperResolutionProbeActivity extends NativeDashboardActivity {
    private static final String TAG = "HomeboardWallpaperProbe";
    private static final long HOLD_MS = 20_000, STAGE_DEADLINE_MS = 30_000;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ArrayDeque<Runnable> steps = new ArrayDeque<>();
    private final JSONObject summary = new JSONObject();
    private final JSONArray failures = new JSONArray();
    private FrameLayout root;
    private TextView report;
    private WallpaperSurface surface;
    private String lines = "4K WALLPAPER PROBE\n";
    private boolean finished;
    private boolean nativeRenderer;
    private Runnable deadline;
    /** "resolution" (the 4K plan's stages) or "animated" (the animated-source matrix), from the alias's meta-data. */
    private String mode = "resolution";

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            Bundle meta = getPackageManager().getActivityInfo(getComponentName(), android.content.pm.PackageManager.GET_META_DATA).metaData;
            if (meta != null) mode = meta.getString("mode", mode);
        } catch (Exception ignored) { }
        if ("animated".equals(mode)) lines = "ANIMATED WALLPAPER PROBE\n";
        root = new FrameLayout(this); root.setBackgroundColor(Color.rgb(16, 24, 40));
        report = new TextView(this); report.setTextColor(Color.WHITE); report.setTextSize(13);
        report.setBackgroundColor(0xCC000000); report.setPadding(16, 12, 16, 12);
        root.addView(report, new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START));
        setContentView(root);
        nativeRenderer = nativeDashboardRendering();
        queue();
        whenLaidOut(this::next);
        main.postDelayed(() -> fail("overall", "probe did not finish within 10 minutes"), 600_000);
    }

    // ---- stages ---------------------------------------------------------------------------------------------

    /** Animated fixtures: the same 12 frames at 10 frames/s, every frame repainting the whole canvas. */
    private static final String[] ANIMATED_FIXTURES = {"uhd_animated", "animated_1440", "animated_1080", "animated_720"};
    private static final int[][] ANIMATED_SOURCES = {{3840, 2160}, {2560, 1440}, {1920, 1080}, {1280, 720}};
    private static final int[][] ANIMATED_TARGETS = {{1920, 1080}, {1280, 720}, {960, 540}};

    private void queue() {
        if ("animated".equals(mode)) { queueAnimated(); return; }
        steps.add(this::environment);
        steps.add(() -> measure("baseline"));
        steps.add(() -> image("hd-control", false, "uhd_stripes"));
        steps.add(() -> image("uhd-image", true, "uhd_stripes"));
        steps.add(this::rotation);
        steps.add(() -> animatedDirect("animated-hd", "uhd_animated", WallpaperTarget.HD_WIDTH, WallpaperTarget.HD_HEIGHT, true));
        steps.add(() -> animatedDirect("animated-uhd", "uhd_animated", WallpaperTarget.UHD_WIDTH, WallpaperTarget.UHD_HEIGHT, true));
        steps.add(() -> animatedProduction("animated-production", "uhd_animated", 3840, 2160));
        steps.add(this::lottie);
        steps.add(() -> video("video-1080", "light", false));
        steps.add(() -> video("uhd-video", "uhd_video", true));
        steps.add(this::fallback);
        steps.add(() -> { replaceSurface(null); measure("baseline-after"); });
        steps.add(this::complete);
    }

    /**
     * docs/plans/2026-11-28-animated-wallpapers-from-large-sources.md: every animated source at its own size and at
     * each smaller target (pace over three loops, PSS while running, no hold), then each through the production
     * WallpaperSurface against the current WallpaperTarget policy.
     */
    private void queueAnimated() {
        steps.add(this::environment);
        steps.add(() -> measure("baseline"));
        for (int i = 0; i < ANIMATED_FIXTURES.length; i++) {
            String raw = ANIMATED_FIXTURES[i]; int[] source = ANIMATED_SOURCES[i];
            java.util.List<int[]> targets = new java.util.ArrayList<>();
            targets.add(source);
            for (int[] t : ANIMATED_TARGETS) if (t[0] < source[0] && t[1] < source[1]) targets.add(t);
            for (int[] t : targets) {
                String stage = "matrix-" + source[1] + "p-" + t[0] + "x" + t[1];
                steps.add(() -> animatedDirect(stage, raw, t[0], t[1], false));
            }
        }
        steps.add(() -> measure("baseline-mid"));
        for (int i = 0; i < ANIMATED_FIXTURES.length; i++) {
            String raw = ANIMATED_FIXTURES[i]; int[] source = ANIMATED_SOURCES[i];
            steps.add(() -> animatedProduction("production-" + source[1] + "p", raw, source[0], source[1]));
        }
        steps.add(() -> { replaceSurface(null); measure("baseline-after"); });
        steps.add(this::complete);
    }

    private void environment() {
        try {
            JSONObject env = new JSONObject();
            int[] rendering = dashboardRenderingSurface();
            Display.Mode mode = getWindowManager().getDefaultDisplay().getMode();
            env.put("renderer", nativeRenderer ? "native" : "ordinary");
            env.put("surface", rendering == null ? "none" : rendering[0] + "x" + rendering[1]);
            env.put("root", root.getWidth() + "x" + root.getHeight());
            env.put("output", mode.getPhysicalWidth() + "x" + mode.getPhysicalHeight() + "@" + Math.round(mode.getRefreshRate()));
            env.put("homeboardVersion", getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
            ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memory = new ActivityManager.MemoryInfo(); manager.getMemoryInfo(memory);
            env.put("memoryClassMiB", manager.getMemoryClass()).put("largeMemoryClassMiB", manager.getLargeMemoryClass())
                    .put("lowRam", manager.isLowRamDevice()).put("totalMiB", memory.totalMem >> 20)
                    .put("availMiB", memory.availMem >> 20).put("thresholdMiB", memory.threshold >> 20)
                    .put("uhdAffordable", WallpaperTarget.uhdAffordable(manager.isLowRamDevice(), memory.availMem, memory.threshold));
            JSONArray hevc = new JSONArray();
            for (MediaCodecInfo info : new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos()) {
                if (info.isEncoder()) continue;
                for (String type : info.getSupportedTypes()) {
                    if (!type.equalsIgnoreCase("video/hevc")) continue;
                    MediaCodecInfo.VideoCapabilities video = info.getCapabilitiesForType(type).getVideoCapabilities();
                    hevc.put(new JSONObject().put("name", info.getName()).put("hardware", info.isHardwareAccelerated())
                            .put("uhd30", video.areSizeAndRateSupported(3840, 2160, 30))
                            .put("maxWidth", video.getSupportedWidths().getUpper()).put("maxHeight", video.getSupportedHeights().getUpper()));
                }
            }
            env.put("hevcDecoders", hevc);
            if (nativeRenderer && (rendering == null || rendering[0] != 3840 || rendering[1] != 2160))
                failures.put("native renderer without a 3840x2160 surface");
            record("environment", env);
        } catch (Exception e) { fail("environment", e.toString()); return; }
        next();
    }

    /** Still picture through a WallpaperSurface: decoded size, hardware, matrix, stripes, then memory. */
    private void image(String stage, boolean production, String raw) {
        WallpaperSurface next = production ? new WallpaperSurface(this) : new WallpaperSurface(this, false);
        replaceSurface(next);
        show(stage, wallpaper(raw, 0), () -> frames(5, () -> {
            try {
                JSONObject result = inspectImage(stage);
                boolean expectUhd = production && nativeRenderer;
                String decoded = result.optString("decoded");
                double h = result.optDouble("stripesH", 0), v = result.optDouble("stripesV", 0);
                if (expectUhd) {
                    if (!"3840x2160".equals(decoded)) failures.put(stage + ": decoded " + decoded);
                    if (!result.optBoolean("hardware")) failures.put(stage + ": not a hardware bitmap");
                    if (!result.optBoolean("identityMatrix")) failures.put(stage + ": image matrix is not identity");
                    if (!"3840x2160".equals(result.optString("view"))) failures.put(stage + ": view " + result.optString("view"));
                    if (h < 0.95 || v < 0.95) failures.put(stage + ": stripes not crisp " + h + "/" + v);
                } else {
                    if (!"1920x1080".equals(decoded)) failures.put(stage + ": expected 1920x1080, decoded " + decoded);
                    if (h > 0.1 || v > 0.1) failures.put(stage + ": 1080p decode unexpectedly kept one-pixel stripes");
                }
                holdThenRecord(stage, result);
            } catch (Exception e) { fail(stage, e.toString()); }
        }));
    }

    private void rotation() {
        String stage = "uhd-rotation";
        String[] order = {"uhd_stripes_b", "uhd_stripes", "uhd_stripes_b", "uhd_stripes", "uhd_stripes_b", "uhd_stripes"};
        int[] index = {0};
        Runnable[] swap = new Runnable[1];
        swap[0] = () -> {
            if (index[0] == order.length) {
                try { holdThenRecord(stage, inspectImage(stage).put("swaps", order.length)); }
                catch (Exception e) { fail(stage, e.toString()); }
                return;
            }
            show(stage + "-" + index[0], wallpaper(order[index[0]++], 0), () -> frames(5, swap[0]));
        };
        swap[0].run();
    }

    /**
     * An animated fixture decoded straight through ImageDecoder at a chosen size: pace over three loops and memory,
     * the memory either after the 20 s hold (resolution mode) or straight after the loops (the animated matrix).
     */
    private void animatedDirect(String stage, String raw, int width, int height, boolean holdForMemory) {
        replaceSurface(null);
        try {
            // Bytes, as WallpaperSurface decodes them: a Resources source would also apply density scaling.
            byte[] bytes;
            try (java.io.InputStream in = getResources().openRawResource(resource(raw));
                 java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
                byte[] buffer = new byte[65536]; int count;
                while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
                bytes = out.toByteArray();
            }
            ImageDecoder.Source source = ImageDecoder.createSource(java.nio.ByteBuffer.wrap(bytes));
            Drawable drawable = ImageDecoder.decodeDrawable(source, (d, info, s) -> d.setTargetSize(width, height));
            if (!(drawable instanceof AnimatedImageDrawable)) { fail(stage, "fixture did not decode as animated"); return; }
            AnimatedImageDrawable animated = (AnimatedImageDrawable) drawable;
            ImageView view = new ImageView(this); view.setScaleType(ImageView.ScaleType.CENTER_CROP);
            view.setImageDrawable(animated);
            root.addView(view, 0, new FrameLayout.LayoutParams(-1, -1));
            animated.setRepeatCount(2);
            long[] started = {0};
            animated.registerAnimationCallback(new Animatable2.AnimationCallback() {
                @Override public void onAnimationStart(Drawable d) { started[0] = SystemClock.elapsedRealtime(); }
                @Override public void onAnimationEnd(Drawable d) {
                    animated.unregisterAnimationCallback(this);   // stop() and later loops must not re-enter
                    disarm();
                    long elapsed = SystemClock.elapsedRealtime() - started[0];
                    try {
                        JSONObject result = new JSONObject().put("decoded", animated.getIntrinsicWidth() + "x" + animated.getIntrinsicHeight())
                                .put("loopsMs", elapsed).put("nominalMs", 3 * 12 * 100).put("paceRatio", Math.round(elapsed / 36.0) / 100.0)
                                .put("stripesH", animatedStripes(animated));
                        animated.setRepeatCount(AnimatedImageDrawable.REPEAT_INFINITE); animated.start();
                        Runnable finish = () -> {
                            try { record(stage, result.put("memory", memory())); } catch (Exception e) { fail(stage, e.toString()); return; }
                            animated.stop(); root.removeView(view); next();
                        };
                        if (holdForMemory) hold(stage, finish); else finish.run();
                    } catch (Exception e) { fail(stage, e.toString()); }
                }
            });
            // Cast1 took about 24 s for three loops of the 4K source at 1080p; smaller targets may be slower still.
            arm(stage, holdForMemory ? STAGE_DEADLINE_MS : 90_000);
            animated.start();
        } catch (Exception e) { fail(stage, e.toString()); }
    }

    /**
     * An animated fixture through the production WallpaperSurface: it must animate at WallpaperTarget.animatedSize
     * when the source is within the animated budget, or show its first frame at the still bound when it is not (4K
     * hardware on the native renderer when admitted). An animation's own pace is timed over three loops.
     */
    private void animatedProduction(String stage, String raw, int sourceWidth, int sourceHeight) {
        replaceSurface(new WallpaperSurface(this));
        show(stage, wallpaper(raw, 0), () -> frames(5, () -> {
            try {
                View view = media(ImageView.class);
                Drawable drawable = view == null ? null : ((ImageView) view).getDrawable();
                String decoded = drawable == null ? "none" : drawable.getIntrinsicWidth() + "x" + drawable.getIntrinsicHeight();
                int[] hd = WallpaperTarget.bound(false, getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels);
                boolean animates = WallpaperTarget.animates(sourceWidth, sourceHeight);
                ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                ActivityManager.MemoryInfo memory = new ActivityManager.MemoryInfo(); manager.getMemoryInfo(memory);
                boolean uhdStill = nativeRenderer && WallpaperTarget.uhdAffordable(manager.isLowRamDevice(), memory.availMem, memory.threshold);
                int[] size = animates ? WallpaperTarget.animatedSize(sourceWidth, sourceHeight, hd)
                        : uhdStill ? WallpaperTarget.size(sourceWidth, sourceHeight, WallpaperTarget.UHD_WIDTH, WallpaperTarget.UHD_HEIGHT)
                        : WallpaperTarget.size(sourceWidth, sourceHeight, hd[0], hd[1]);
                String expected = size[0] + "x" + size[1];
                boolean isAnimated = drawable instanceof AnimatedImageDrawable;
                JSONObject result = new JSONObject().put("source", sourceWidth + "x" + sourceHeight).put("decoded", decoded)
                        .put("expected", expected).put("animated", isAnimated).put("policyAnimates", animates)
                        .put("maxSourcePixels", WallpaperTarget.ANIMATED_MAX_SOURCE_PIXELS)
                        .put("animatedCap", WallpaperTarget.ANIMATED_WIDTH + "x" + WallpaperTarget.ANIMATED_HEIGHT);
                if (!expected.equals(decoded)) failures.put(stage + ": decoded " + decoded + ", policy expects " + expected);
                if (isAnimated != animates) failures.put(stage + ": animated " + isAnimated + ", policy expects " + animates);
                if (!animates && drawable instanceof BitmapDrawable) {
                    Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                    result.put("hardware", bitmap.getConfig() == Bitmap.Config.HARDWARE);
                    if (uhdStill && bitmap.getConfig() != Bitmap.Config.HARDWARE) failures.put(stage + ": first frame is not a hardware bitmap");
                }
                if (!isAnimated) { holdThenRecord(stage, result); return; }
                AnimatedImageDrawable animated = (AnimatedImageDrawable) drawable;
                animated.stop(); animated.setRepeatCount(2);
                long[] started = {0};
                animated.registerAnimationCallback(new Animatable2.AnimationCallback() {
                    @Override public void onAnimationStart(Drawable d) { started[0] = SystemClock.elapsedRealtime(); }
                    @Override public void onAnimationEnd(Drawable d) {
                        animated.unregisterAnimationCallback(this);
                        disarm();
                        long elapsed = SystemClock.elapsedRealtime() - started[0];
                        try {
                            result.put("loopsMs", elapsed).put("nominalMs", 3 * 12 * 100).put("paceRatio", Math.round(elapsed / 36.0) / 100.0);
                            animated.setRepeatCount(AnimatedImageDrawable.REPEAT_INFINITE); animated.start();
                            holdThenRecord(stage, result);
                        } catch (Exception e) { fail(stage, e.toString()); }
                    }
                });
                arm(stage, 90_000);
                animated.start();
            } catch (Exception e) { fail(stage, e.toString()); }
        }));
    }

    private void lottie() {
        String stage = "lottie";
        replaceSurface(new WallpaperSurface(this));
        show(stage, wallpaper("gradient", 3), () -> frames(5, () -> {
            try {
                View view = media(LottieAnimationView.class);
                if (view == null) { fail(stage, "no Lottie view"); return; }
                String size = view.getWidth() + "x" + view.getHeight();
                if (!size.equals(root.getWidth() + "x" + root.getHeight())) failures.put(stage + ": view " + size);
                holdThenRecord(stage, new JSONObject().put("view", size).put("hardwareAccelerated", view.isHardwareAccelerated())
                        .put("renderMode", ((LottieAnimationView) view).getRenderMode().name()));
            } catch (Exception e) { fail(stage, e.toString()); }
        }));
    }

    private void video(String stage, String raw, boolean uhd) {
        replaceSurface(new WallpaperSurface(this));
        surface.visible(true);
        show(stage, wallpaper(raw, 4), () -> frames(30, () -> {
            try {
                TextureView texture = (TextureView) media(TextureView.class);
                if (texture == null) { fail(stage, "no TextureView"); return; }
                JSONObject result = new JSONObject().put("view", texture.getWidth() + "x" + texture.getHeight())
                        .put("identityTransform", texture.getTransform(null).isIdentity());
                if (uhd) {
                    Bitmap frame = texture.getBitmap();
                    if (frame == null) { failures.put(stage + ": no readback"); }
                    else {
                        result.put("readback", frame.getWidth() + "x" + frame.getHeight())
                                .put("stripesH", stripesH(frame)).put("stripesV", stripesV(frame));
                        frame.recycle();
                        if (nativeRenderer && (result.optDouble("stripesH") < 0.9 || result.optDouble("stripesV") < 0.9))
                            failures.put(stage + ": video readback not crisp " + result.optDouble("stripesH") + "/" + result.optDouble("stripesV"));
                    }
                    if (nativeRenderer && !"3840x2160".equals(result.optString("view"))) failures.put(stage + ": view " + result.optString("view"));
                }
                holdThenRecord(stage, result);
            } catch (Exception e) { fail(stage, e.toString()); }
        }), uhd && !nativeRenderer);
    }

    private void fallback() {
        String stage = "fallback";
        replaceSurface(new WallpaperSurface(this));
        WallpaperSurface.failNextUhdDecode = nativeRenderer;
        show(stage, wallpaper("uhd_stripes_b", 0), () -> frames(5, () -> {
            try {
                JSONObject result = inspectImage(stage).put("injected", nativeRenderer).put("flagConsumed", !WallpaperSurface.failNextUhdDecode);
                if (!"1920x1080".equals(result.optString("decoded"))) failures.put(stage + ": decoded " + result.optString("decoded"));
                if (WallpaperSurface.failNextUhdDecode) failures.put(stage + ": injected failure was never reached");
                WallpaperSurface.failNextUhdDecode = false;
                holdThenRecord(stage, result);
            } catch (Exception e) { fail(stage, e.toString()); }
        }));
    }

    private void complete() {
        if (finished) return;
        finished = true;
        try {
            summary.put("status", failures.length() == 0 ? "pass" : "fail").put("failures", failures)
                    .put("renderer", nativeRenderer ? "native" : "ordinary");
            emit(summary);
        } catch (Exception e) { Log.e(TAG, "summary", e); }
    }

    // ---- inspection -------------------------------------------------------------------------------------------

    private JSONObject inspectImage(String stage) throws Exception {
        ImageView view = (ImageView) media(ImageView.class);
        if (view == null) throw new IllegalStateException(stage + ": no image view");
        Drawable drawable = view.getDrawable();
        JSONObject result = new JSONObject().put("view", view.getWidth() + "x" + view.getHeight())
                .put("identityMatrix", view.getImageMatrix().isIdentity());
        if (!(drawable instanceof BitmapDrawable)) return result.put("decoded", "not a bitmap");
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        result.put("decoded", bitmap.getWidth() + "x" + bitmap.getHeight()).put("config", String.valueOf(bitmap.getConfig()))
                .put("hardware", bitmap.getConfig() == Bitmap.Config.HARDWARE).put("bytes", bitmap.getAllocationByteCount());
        Bitmap pixels = bitmap.getConfig() == Bitmap.Config.HARDWARE ? bitmap.copy(Bitmap.Config.ARGB_8888, false) : bitmap;
        result.put("stripesH", stripesH(pixels)).put("stripesV", stripesV(pixels));
        if (pixels != bitmap) pixels.recycle();
        return result;
    }

    /** Fraction of neighbouring pixels along the vertical-stripe band (4K row 1080) that differ by 128 or more. */
    private static double stripesH(Bitmap b) {
        int y = 1080 * b.getHeight() / 2160, w = b.getWidth();
        int[] row = new int[w]; b.getPixels(row, 0, w, 0, y, w, 1);
        return crisp(row);
    }
    /** The same down the horizontal-stripe block (4K column 1900, rows 1320-1559). */
    private static double stripesV(Bitmap b) {
        int x = 1900 * b.getWidth() / 3840, y0 = 1320 * b.getHeight() / 2160, y1 = 1560 * b.getHeight() / 2160;
        int[] column = new int[y1 - y0]; b.getPixels(column, 0, 1, x, y0, 1, y1 - y0);
        return crisp(column);
    }
    private static double crisp(int[] pixels) {
        int sharp = 0;
        for (int i = 0; i + 1 < pixels.length; i++) if (Math.abs(luma(pixels[i]) - luma(pixels[i + 1])) >= 128) sharp++;
        return pixels.length < 2 ? 0 : Math.round(1000.0 * sharp / (pixels.length - 1)) / 1000.0;
    }
    private static int luma(int c) { return (Color.red(c) * 299 + Color.green(c) * 587 + Color.blue(c) * 114) / 1000; }

    private static double animatedStripes(AnimatedImageDrawable animated) {
        int w = animated.getIntrinsicWidth(), h = animated.getIntrinsicHeight();
        Bitmap row = Bitmap.createBitmap(w, 1, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(row); canvas.translate(0, -(1080 * h / 2160));
        animated.setBounds(0, 0, w, h);
        try { animated.draw(canvas); } catch (RuntimeException unsupported) { row.recycle(); return -1; }
        int[] pixels = new int[w]; row.getPixels(pixels, 0, w, 0, 0, w, 1); row.recycle();
        return crisp(pixels);
    }

    private JSONObject memory() throws Exception {
        for (int i = 0; i < 2; i++) { Runtime.getRuntime().gc(); System.runFinalization(); }
        Debug.MemoryInfo info = new Debug.MemoryInfo(); Debug.getMemoryInfo(info);
        ActivityManager.MemoryInfo system = new ActivityManager.MemoryInfo();
        ((ActivityManager) getSystemService(ACTIVITY_SERVICE)).getMemoryInfo(system);
        return new JSONObject().put("pssKiB", info.getTotalPss())
                .put("graphicsKiB", Integer.parseInt(info.getMemoryStat("summary.graphics")))
                .put("nativeHeapKiB", Integer.parseInt(info.getMemoryStat("summary.native-heap")))
                .put("javaHeapKiB", Integer.parseInt(info.getMemoryStat("summary.java-heap")))
                .put("availMiB", system.availMem >> 20).put("uptimeMs", SystemClock.uptimeMillis());
    }

    // ---- plumbing ---------------------------------------------------------------------------------------------

    private Wallpaper wallpaper(String raw, int type) {
        return new Wallpaper("android.resource://" + getPackageName() + "/raw/" + raw, type, 1, raw, null, "Homeboard 4K fixture", null);
    }
    private int resource(String raw) { return getResources().getIdentifier(raw, "raw", getPackageName()); }

    private void replaceSurface(WallpaperSurface next) {
        if (surface != null) { root.removeView(surface); surface.close(); }
        surface = next;
        if (next != null) { next.options(0, false); next.visible(true); root.addView(next, 0, new FrameLayout.LayoutParams(-1, -1)); }
    }

    private void show(String stage, Wallpaper item, Runnable ready) { show(stage, item, ready, false); }
    /** Shows an item and continues once WallpaperSurface reports it on screen; a refusal is fatal unless tolerated. */
    private void show(String stage, Wallpaper item, Runnable ready, boolean tolerateFailure) {
        arm(stage);
        surface.show(item, (ok, status) -> {
            if (finished) return;
            disarm();
            if (ok) { ready.run(); return; }
            if (tolerateFailure) {
                try { record(stage, new JSONObject().put("unavailable", status)); } catch (Exception ignored) { }
                next();
            } else fail(stage, status);
        });
    }

    private View media(Class<? extends View> kind) { return surface == null ? null : find(surface, kind); }
    private static View find(View view, Class<? extends View> kind) {
        if (kind.isInstance(view)) return view;
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            View found = find(((ViewGroup) view).getChildAt(i), kind);
            if (found != null) return found;
        }
        return null;
    }

    /** Continues after n frames have been drawn. */
    private void frames(int n, Runnable then) {
        if (n <= 0) { then.run(); return; }
        root.postOnAnimation(() -> frames(n - 1, then));
    }
    private void whenLaidOut(Runnable then) {
        root.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override public void onGlobalLayout() {
                int[] rendering = dashboardRenderingSurface();
                if (root.getWidth() == 0 || nativeRenderer && (rendering == null || rendering[0] == 0)) return;
                root.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                then.run();
            }
        });
    }
    /** The measurement window is the point of this wait: coordinator PSS samples land inside it. */
    private void hold(String stage, Runnable then) { log("hold " + stage); main.postDelayed(then, HOLD_MS); }
    private void measure(String stage) { holdThenRecord(stage, new JSONObject()); }
    private void holdThenRecord(String stage, JSONObject result) {
        hold(stage, () -> {
            try { record(stage, result.put("memory", memory())); } catch (Exception e) { fail(stage, e.toString()); return; }
            next();
        });
    }
    private void arm(String stage) { arm(stage, STAGE_DEADLINE_MS); }
    private void arm(String stage, long ms) { disarm(); deadline = () -> fail(stage, "no result within " + ms + " ms"); main.postDelayed(deadline, ms); }
    private void disarm() { if (deadline != null) main.removeCallbacks(deadline); deadline = null; }

    private void next() {
        if (finished) return;
        Runnable step = steps.poll();
        if (step != null) step.run();
    }
    private void record(String stage, JSONObject result) {
        try { summary.put(stage, result); } catch (Exception ignored) { }
        log(stage + " " + result);
    }
    private void fail(String stage, String reason) {
        if (finished) return;
        failures.put(stage + ": " + reason);
        disarm();
        complete();
    }
    private void log(String line) {
        Log.i(TAG, line);
        lines += line.length() > 160 ? line.substring(0, 160) + "…\n" : line + "\n";
        report.setText(lines);
    }
    private void emit(JSONObject result) {
        Log.i(TAG, result.toString());
        try { Files.write(new File(getExternalFilesDir(null), "wf.log").toPath(), result.toString().getBytes(StandardCharsets.UTF_8)); }
        catch (Exception e) { Log.e(TAG, "wf.log", e); }
        report.setText(lines + "\n" + result.optString("status").toUpperCase() + " " + result.optJSONArray("failures"));
    }

    @Override protected void onDestroy() {
        main.removeCallbacksAndMessages(null);
        WallpaperSurface.failNextUhdDecode = false;
        if (surface != null) surface.close();
        super.onDestroy();
    }
}
