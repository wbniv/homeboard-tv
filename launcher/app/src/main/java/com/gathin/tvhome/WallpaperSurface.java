package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.LottieCompositionFactory;
import com.airbnb.lottie.LottieResult;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import tv.projectivy.plugin.wallpaperprovider.api.Wallpaper;

/** A non-focusable wallpaper layer below the independent colored UI backdrops. */
final class WallpaperSurface extends FrameLayout implements AutoCloseable {
    private static final int MAX_BYTES = 16 * 1024 * 1024;
    private final Handler main = new Handler(Looper.getMainLooper());
    private static final ThreadPoolExecutor DECODER = new ThreadPoolExecutor(1, 1, 10, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1), new ThreadPoolExecutor.DiscardOldestPolicy());
    private final View dim;
    private FrameLayout media;
    private MediaPlayer player, preparing;
    private Surface preparingSurface;
    private FrameLayout preparingView;
    private Runnable preparingDeadline;
    private Surface videoSurface;
    private Animatable animated;
    private LottieAnimationView lottie;
    private long generation;
    private boolean active, reduced, closed;
    private Wallpaper last;
    private BiConsumer<Boolean, String> result;

    WallpaperSurface(Context context) {
        super(context); setFocusable(false); setClickable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        dim = new View(context); dim.setBackgroundColor(Color.BLACK);
        addView(dim, new LayoutParams(-1, -1));
    }
    void options(int dimPercent, boolean reducedMotion) {
        dim.setAlpha(WallpaperSettings.percent(dimPercent) / 100f);
        boolean changed = reduced != reducedMotion; reduced = reducedMotion;
        if (changed && last != null) show(last, result);
    }
    void visible(boolean visible) {
        active = visible;
        if (!visible) {
            generation++;
            releaseMotion();
        } else if (last != null && isMotion(last)) show(last, result);
    }
    private static boolean isMotion(Wallpaper w) { return w.getType() >= 2 && w.getType() <= 4; }
    void solid() {
        generation++; last = null;  releaseMotion();
        if (media != null) removeView(media); media = null;
    }
    void show(Wallpaper wallpaper, BiConsumer<Boolean, String> callback) {
        if (closed) return;
        long token = ++generation;
        cancelPreparing();
        result = callback;
        if (wallpaper.getType() < 0 || wallpaper.getType() > 5 || wallpaper.getDisplayMode() < 0 || wallpaper.getDisplayMode() > 5) {
            callback.accept(false, getContext().getString(R.string.hb_status_unsupported_wallpaper_format_or_display_mode)); return;
        }
        if (wallpaper.getType() == 5) {
            try {
                FrameLayout next = new FrameLayout(getContext()); next.setBackgroundColor(Color.parseColor(wallpaper.getUri()));
                install(next, wallpaper); callback.accept(true, getContext().getString(R.string.hb_status_preview_ready));
            } catch (IllegalArgumentException invalid) { callback.accept(false, getContext().getString(R.string.hb_status_invalid_wallpaper_color)); }
            return;
        }
        if (wallpaper.getType() == 3 && (wallpaper.getDisplayMode() == 5 || wallpaper.getDisplayMode() == 3 && Build.VERSION.SDK_INT < 31)) {
            callback.accept(false, getContext().getString(R.string.hb_status_this_animation_display_mode_is_unsupported_on_this_device)); return;
        }
        if (wallpaper.getType() == 4) { video(wallpaper, token, callback); return; }
        int width = Math.min(1920, Math.max(1, getResources().getDisplayMetrics().widthPixels));
        int height = Math.min(1080, Math.max(1, getResources().getDisplayMetrics().heightPixels));
        main.postDelayed(() -> {
            if (token == generation && !closed) {
                generation++; callback.accept(false, getContext().getString(R.string.hb_status_wallpaper_decoding_timed_out_retaining_previous_background));
            }
        }, 10_000);
        DECODER.execute(() -> {
            try {
                Uri uri = Uri.parse(MediaUris.secure(wallpaper.getUri()));
                Drawable drawable = null; LottieComposition composition = null;
                if (wallpaper.getType() == 1 || wallpaper.getType() == 2) {
                    if (!"android.resource".equals(uri.getScheme())) throw new IllegalArgumentException();
                    android.content.res.Resources resources = getContext().getPackageManager().getResourcesForApplication(uri.getAuthority());
                    java.util.List<String> segments = uri.getPathSegments();
                    int id = segments.size() == 1 ? Integer.parseInt(segments.get(0))
                            : segments.size() == 2 ? resources.getIdentifier(segments.get(1), segments.get(0), uri.getAuthority()) : 0;
                    drawable = resources.getDrawable(id, null);
                } else {
                    byte[] bytes = read(uri);
                    if (wallpaper.getType() == 3) {
                        LottieResult<LottieComposition> parsed = LottieCompositionFactory.fromJsonInputStreamSync(new java.io.ByteArrayInputStream(bytes), null);
                        composition = parsed.getValue(); if (composition == null) throw new IllegalArgumentException();
                    } else if (Build.VERSION.SDK_INT >= 28) {
                        drawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(ByteBuffer.wrap(bytes)), (d, info, source) -> {
                            int w = info.getSize().getWidth(), h = info.getSize().getHeight();
                            double scale = Math.min(1, Math.min((double) width / w, (double) height / h));
                            d.setTargetSize(Math.max(1, (int)(w * scale)), Math.max(1, (int)(h * scale)));
                            d.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                        });
                    } else {
                        BitmapFactory.Options options = new BitmapFactory.Options(); options.inJustDecodeBounds = true;
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
                        int sample = 1;
                        while (options.outWidth / sample > width * 2 || options.outHeight / sample > height * 2) sample *= 2;
                        options.inJustDecodeBounds = false; options.inSampleSize = sample;
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
                        if (bitmap == null) throw new IllegalArgumentException();
                        drawable = new BitmapDrawable(getResources(), bitmap);
                    }
                }
                Drawable image = drawable; LottieComposition animation = composition;
                main.post(() -> {
                    if (token != generation || closed) return;
                    FrameLayout next = new FrameLayout(getContext());
                    int mode = wallpaper.getDisplayMode();
                    if (image != null) {
                        if (mode == 3 || mode == 5) {
                            Bitmap tiny = Bitmap.createBitmap(32, 18, Bitmap.Config.ARGB_8888);
                            image.setBounds(0, 0, 32, 18); image.draw(new android.graphics.Canvas(tiny));
                            ImageView blur = new ImageView(getContext());
                            BitmapDrawable softened = new BitmapDrawable(getResources(), tiny); softened.setFilterBitmap(true);
                            blur.setImageDrawable(softened); blur.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            next.addView(blur, new LayoutParams(-1, -1));
                        }
                        if (mode != 3) {
                            ImageView view = new ImageView(getContext()); view.setImageDrawable(image);
                            view.setScaleType(scaleType(mode)); next.addView(view, new LayoutParams(-1, -1));
                        }
                    } else {
                        LottieAnimationView view = new LottieAnimationView(getContext()); view.setComposition(animation);
                        view.setRepeatCount(-1); view.setScaleType(scaleType(mode));
                        if ((mode == 3 || mode == 5) && Build.VERSION.SDK_INT >= 31)
                            view.setRenderEffect(android.graphics.RenderEffect.createBlurEffect(24, 24, android.graphics.Shader.TileMode.CLAMP));
                        next.addView(view, new LayoutParams(-1, -1));
                    }
                    generation++;
                    install(next, wallpaper);
                    if (image instanceof Animatable) { animated = (Animatable) image; if (active && !reduced) animated.start(); }
                    if (animation != null) { lottie = (LottieAnimationView) next.getChildAt(0); if (active && !reduced) lottie.playAnimation(); }
                    callback.accept(true, getContext().getString(R.string.hb_status_preview_ready));
                });
            } catch (Exception | OutOfMemoryError invalid) {
                main.post(() -> { if (token == generation && !closed) { generation++; callback.accept(false, "Cannot load wallpaper; check its format, URI or permissions"); } });
            }
        });
    }
    private static ImageView.ScaleType scaleType(int mode) {
        return mode == 2 ? ImageView.ScaleType.FIT_XY : mode == 4 || mode == 5 ? ImageView.ScaleType.FIT_CENTER : ImageView.ScaleType.CENTER_CROP;
    }
    private byte[] read(Uri uri) throws Exception {
        String scheme = uri.getScheme();
        HttpURLConnection connection = null;
        InputStream stream;
        if ("https".equals(scheme) || MediaUris.appleHttp(uri.toString())) {
            URL url = new URL(uri.toString());
            for (int redirects = 0; ; redirects++) {
                if (!"https".equals(url.getProtocol()) && !("http".equals(scheme) && MediaUris.appleHttp(url.toString()))) throw new IllegalArgumentException();
                connection = (HttpURLConnection) url.openConnection(); connection.setConnectTimeout(8000); connection.setReadTimeout(8000);
                connection.setInstanceFollowRedirects(false);
                int status = connection.getResponseCode();
                if (status >= 300 && status < 400 && redirects < 5) {
                    String location = connection.getHeaderField("Location"); connection.disconnect();
                    if (location == null) throw new IllegalArgumentException();
                    url = new URL(url, location); continue;
                }
                if (status < 200 || status >= 300 || connection.getContentLengthLong() > MAX_BYTES) { connection.disconnect(); throw new IllegalArgumentException(); }
                break;
            }
            stream = connection.getInputStream();
        } else if ("android.resource".equals(scheme) || "content".equals(scheme)) stream = getContext().getContentResolver().openInputStream(uri);
        else throw new IllegalArgumentException("Only accessible content/resources and HTTPS are supported");
        try (InputStream input = stream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (input == null) throw new IllegalArgumentException();
            byte[] buffer = new byte[8192]; int count;
            while ((count = input.read(buffer)) != -1) {
                if (output.size() + count > MAX_BYTES) throw new IllegalArgumentException();
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        } finally { if (connection != null) connection.disconnect(); }
    }
    private void install(FrameLayout next, Wallpaper wallpaper) {
        releaseMotion();
        if (media != null) removeView(media);
        media = next; last = wallpaper;
        if (next.getParent() == null) addView(next, 0, new LayoutParams(-1, -1));
        else next.bringToFront();
        dim.bringToFront();
    }
    private void video(Wallpaper w, long token, BiConsumer<Boolean, String> callback) {
        Uri uri;
        try { uri = Uri.parse(MediaUris.secure(w.getUri())); }
        catch (IllegalArgumentException invalid) { callback.accept(false, getContext().getString(R.string.hb_status_video_needs_https_or_an_accessible_content_resource_uri)); return; }
        String scheme = uri.getScheme();
        if (!"https".equals(scheme) && !MediaUris.appleHttp(uri.toString()) && !"content".equals(scheme) && !"android.resource".equals(scheme)) {
            callback.accept(false, getContext().getString(R.string.hb_status_video_needs_https_or_an_accessible_content_resource_uri)); return;
        }
        if (reduced) { solid(); callback.accept(false, getContext().getString(R.string.hb_status_video_paused_for_reduced_motion_using_solid_color)); return; }
        if (w.getDisplayMode() == 5 || w.getDisplayMode() == 3 && Build.VERSION.SDK_INT < 31) {
            callback.accept(false, getContext().getString(R.string.hb_status_this_video_display_mode_is_unsupported_on_this_device)); return;
        }
        FrameLayout next = new FrameLayout(getContext()); TextureView texture = new TextureView(getContext());
        texture.setOpaque(false); next.addView(texture, new LayoutParams(-1, -1));
        MediaPlayer pendingPlayer = new MediaPlayer(); preparing = pendingPlayer; preparingView = next;
        preparingDeadline = () -> {
            if (token == generation && preparing == pendingPlayer) {
                cancelPreparing(); callback.accept(false, getContext().getString(R.string.hb_status_video_preparation_timed_out));
            }
        };
        main.postDelayed(preparingDeadline, 10_000);
        texture.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override public void onSurfaceTextureAvailable(SurfaceTexture st, int width, int height) {
                if (token != generation || closed || preparing != pendingPlayer) return;
                preparingSurface = new Surface(st);
                try {
                    pendingPlayer.setSurface(preparingSurface); pendingPlayer.setVolume(0, 0); pendingPlayer.setLooping(true);
                    pendingPlayer.setOnPreparedListener(mp -> {
                        if (token != generation || closed || preparing != mp) return;
                        float vw = mp.getVideoWidth(), vh = mp.getVideoHeight();
                        if (vw > 0 && vh > 0 && w.getDisplayMode() != 2) {
                            boolean fit = w.getDisplayMode() == 4;
                            float scale = fit ? Math.min(width / vw, height / vh) : Math.max(width / vw, height / vh);
                            Matrix transform = new Matrix(); transform.setScale(vw * scale / width, vh * scale / height, width / 2f, height / 2f);
                            texture.setTransform(transform);
                        }
                        if (w.getDisplayMode() == 3 && Build.VERSION.SDK_INT >= 31)
                            texture.setRenderEffect(android.graphics.RenderEffect.createBlurEffect(24, 24, android.graphics.Shader.TileMode.CLAMP));
                        Surface readySurface = preparingSurface;
                        main.removeCallbacks(preparingDeadline);
                        preparing = null; preparingSurface = null; preparingView = null; preparingDeadline = null;
                        install(next, w); player = mp; videoSurface = readySurface;
                        next.setAlpha(1);
                        boolean[] rendered = {false};
                        mp.setOnInfoListener((playing, what, extra) -> {
                            if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START && token == generation && !closed) {
                                rendered[0] = true; callback.accept(true, getContext().getString(R.string.hb_status_preview_ready)); return true;
                            }
                            return false;
                        });
                        main.postDelayed(() -> {
                            if (token == generation && player == mp && !rendered[0] && !closed) {
                                releaseMotion(); callback.accept(false, getContext().getString(R.string.hb_status_video_produced_no_frame));
                            }
                        }, 10_000);
                        if (active) mp.start();
                    });
                    pendingPlayer.setOnErrorListener((mp, what, extra) -> {
                        if (token == generation && !closed) {
                            if (preparing == mp) cancelPreparing(); else releaseMotion();
                            callback.accept(false, getContext().getString(R.string.hb_status_video_playback_is_unavailable));
                        }
                        return true;
                    });
                    pendingPlayer.setDataSource(getContext(), uri); pendingPlayer.prepareAsync();
                } catch (Exception invalid) {
                    cancelPreparing(); if (token == generation && !closed) callback.accept(false, getContext().getString(R.string.hb_status_video_source_is_unavailable));
                }
            }
            @Override public void onSurfaceTextureSizeChanged(SurfaceTexture st, int w, int h) { }
            @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture st) { return true; }
            @Override public void onSurfaceTextureUpdated(SurfaceTexture st) { }
        });
        // Attach at zero opacity to obtain a SurfaceTexture while retaining the previous frame.
        next.setAlpha(0); addView(next, 0, new LayoutParams(-1, -1));
    }
    private void cancelPreparing() {
        if (preparingDeadline != null) main.removeCallbacks(preparingDeadline);
        preparingDeadline = null;
        if (preparing != null) { preparing.setOnPreparedListener(null); preparing.setOnErrorListener(null); preparing.release(); preparing = null; }
        if (preparingSurface != null) { preparingSurface.release(); preparingSurface = null; }
        if (preparingView != null) { removeView(preparingView); preparingView = null; }
    }
    private void releaseMotion() {
        cancelPreparing();
        if (animated != null) { animated.stop(); animated = null; }
        if (lottie != null) { lottie.cancelAnimation(); lottie = null; }
        if (player != null) { player.release(); player = null; }
        if (videoSurface != null) { videoSurface.release(); videoSurface = null; }
    }
    @Override public void close() { closed = true; solid();  main.removeCallbacksAndMessages(null); }
}
