package com.gathin.tvhome;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import tv.projectivy.plugin.wallpaperprovider.api.*;

/** Debug-only, offline device oracle. Never alters the user's saved layout/background. */
public final class WallpaperProbeActivity extends Activity {
    private final Handler main = new Handler();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private ProjectivyWallpaperHost host;
    private WallpaperSurface wallpaper;
    private IWallpaperProviderService control;
    private TextView report;
    private int stage;
    private boolean finished, bound;
    private String lines = "WALLPAPER PROBE\n";
    private final String[] modes = {"image", "drawable", "animated", "lottie", "video"};
    private ServiceConnection connection;
    private void log(String line) { lines += line + "\n"; report.setText(lines); }
    private void fail(String reason) { if (!finished) { finished = true; log("FAIL: " + reason); } }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        FrameLayout root = new FrameLayout(this); root.setBackgroundColor(Color.rgb(16, 24, 40));
        wallpaper = new WallpaperSurface(this); root.addView(wallpaper, new FrameLayout.LayoutParams(-1, -1));
        wallpaper.options(0, false); wallpaper.visible(true);
        report = new TextView(this); report.setTextColor(Color.WHITE); report.setTextSize(18);
        report.setPadding(22, 16, 22, 12); report.setBackgroundColor(0xff152339); report.setAlpha(.75f);
        root.addView(report, new FrameLayout.LayoutParams(620, -2, Gravity.TOP | Gravity.LEFT)); setContentView(root);
        PieMenuView pie = new PieMenuView(this, PaletteColors.from(0xff102333)); pie.setAlpha(WallpaperSettings.PIE_ALPHA);
        pie.setContent("Opacity", "UI 75% · pie 95%", "Probe", "Device validation", new String[]{"Up", "Right", "Down", "Left"},
                new MenuIconView.Kind[4], new boolean[4], null);
        FrameLayout.LayoutParams pieLp = new FrameLayout.LayoutParams(480, 480, Gravity.RIGHT | Gravity.CENTER_VERTICAL); root.addView(pie, pieLp);
        if (report.getAlpha() != .75f || pie.getAlpha() != .95f) { fail("opacity groups"); return; }
        log("PASS opacity: UI 75%, pie 95%");
        host = new ProjectivyWallpaperHost(this, new ProjectivyWallpaperHost.Listener() {
            @Override public void wallpaper(Wallpaper item) {
                if (finished) return;
                wallpaper.show(item, (ready, status) -> {
                    if (!ready) { fail(status); return; }
                    log("PASS render " + item.getType() + ": " + item.getTitle());
                    main.postDelayed(() -> advance(item), 300);
                });
            }
            @Override public void status(String message) {
                if (stage == 10 && message.startsWith("Provider did not respond")) {
                    finished = true; log("PASS hung provider timeout; UI alive\nALL CHECKS PASSED");
                    restoreProvider();
                }
            }
            @Override public void fallback() { wallpaper.solid(); }
        });
        ProjectivyProviderRegistry.Provider provider = new ProjectivyProviderRegistry(this).discover().stream()
                .filter(p -> p.component.getPackageName().equals(getPackageName()) && p.component.getClassName().equals("net.biohack.homeboard.wallpaperfixture.ProviderService")).findFirst().orElse(null);
        if (provider == null || !provider.usable()) { fail("debug fixture is not discoverable"); return; }
        log("PASS provider discovery");
        connection = new ServiceConnection() {
            @Override public void onServiceConnected(ComponentName name, IBinder binder) {
                control = IWallpaperProviderService.Stub.asInterface(binder);
                worker.execute(() -> {
                    try {
                        control.setPreferences("image");
                        if (!"image".equals(control.getPreferences())) throw new IllegalStateException("preferences round trip");
                        control.setPreferences("{}");
                        Event[] events = {Event.TimeElapsed.INSTANCE, new Event.CardFocused(4, 1, 2, "App", "example"),
                                new Event.LauncherIdleModeChanged(16, true), new Event.NowPlayingChanged(2, true, "Song", "Artist", "Album", null),
                                new Event.ProgramCardFocused(8, "Program", null, null, 0)};
                        for (Event event : events) {
                            Parcel parcel = Parcel.obtain(); event.writeToParcel(parcel, 0); parcel.setDataPosition(0);
                            Event restored = Event.CREATOR.createFromParcel(parcel); parcel.recycle();
                            if (restored == null || !restored.equals(event)) throw new IllegalStateException("event parcel");
                            Wallpaper item = control.getWallpapers(event).get(0);
                            Parcel imageParcel = Parcel.obtain(); item.writeToParcel(imageParcel, 0); imageParcel.setDataPosition(0);
                            Wallpaper image = Wallpaper.CREATOR.createFromParcel(imageParcel); imageParcel.recycle();
                            if (!item.equals(image)) throw new IllegalStateException("wallpaper parcel");
                        }
                        main.post(() -> {
                            log("PASS cross-process API and all event parcels");
                            WallpaperSettings settings = new WallpaperSettings(); settings.component = provider.component.flattenToString(); settings.uuid = provider.uuid;
                            host.select(settings); host.visible(true);
                        });
                    } catch (Exception e) { main.post(() -> fail(e.toString())); }
                });
            }
            @Override public void onServiceDisconnected(ComponentName name) { control = null; }
        };
        bound = bindService(new Intent(ProjectivyProviderRegistry.ACTION).setComponent(provider.component), connection, Context.BIND_AUTO_CREATE);
        main.postDelayed(() -> { if (!finished) fail("overall probe timeout"); }, 35_000);
    }
    private void advance(Wallpaper item) {
        if (finished) return;
        if (stage == 0) {
            if (!"#173f68".equals(item.getUri())) { fail("initial timer event"); return; }
            stage = 1; host.appFocused("App", "example", Color.CYAN, Color.BLUE);
        } else if (stage == 1) {
            if (!"#32676c".equals(item.getUri())) { fail("focused app event"); return; }
            stage = 2; host.setIdle(true);
        } else if (stage == 2) {
            if (!"#37294f".equals(item.getUri())) { fail("idle event"); return; }
            stage = 3; setMode(modes[0]);
        } else if (stage >= 3 && stage <= 6) {
            stage++; setMode(modes[stage - 3]);
        } else if (stage == 7) {
            log("PASS all six media types"); stage = 10; setMode("hang");
        }
    }
    private void setMode(String mode) {
        worker.execute(() -> {
            try { control.setPreferences(mode); main.post(() -> host.refresh()); }
            catch (Exception error) { main.post(() -> fail("set test mode")); }
        });
    }
    private void restoreProvider() {
        if (control != null) worker.execute(() -> { try { control.setPreferences("{}"); } catch (Exception ignored) { } });
    }
    @Override protected void onDestroy() {
        if (host != null) host.close(); wallpaper.close(); main.removeCallbacksAndMessages(null);
        restoreProvider(); if (bound) unbindService(connection); worker.shutdown(); super.onDestroy();
    }
}
