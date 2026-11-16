package com.gathin.tvhome;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;
import tv.projectivy.plugin.wallpaperprovider.api.Event;
import tv.projectivy.plugin.wallpaperprovider.api.IWallpaperProviderService;
import tv.projectivy.plugin.wallpaperprovider.api.Wallpaper;

/** Activity-scoped host. All synchronous IPC is off the main thread and globally bounded. */
final class ProjectivyWallpaperHost implements AutoCloseable {
    interface Listener { void wallpaper(Wallpaper item); void status(String message); void fallback(); }
    static final String UPDATED = "tv.projectivy.plugin.action.WALLPAPER_PROVIDER_UPDATED";
    static final String PROVIDER_ID = "tv.projectivy.plugin.extra.PROVIDER_ID";
    // A hung remote Binder call cannot be interrupted. Never replace it with unbounded new workers.
    private static final ThreadPoolExecutor IPC = new ThreadPoolExecutor(1, 1, 10, TimeUnit.SECONDS,
            new SynchronousQueue<>(), task -> { Thread t = new Thread(task, "wallpaper-ipc"); t.setDaemon(true); return t; });
    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ProjectivyProviderRegistry registry;
    private final Listener listener;
    private WallpaperSettings settings = new WallpaperSettings();
    private ProjectivyProviderRegistry.Provider provider;
    private IWallpaperProviderService remote;
    private ServiceConnection connection;
    private final List<Wallpaper> batch = new ArrayList<>();
    private long expires, generation, lastBroadcast;
    private int nextItem, candidateIndex;
    private List<Wallpaper> candidates = new ArrayList<>();
    private boolean visible, busy, closed, idle;
    private Event pending, focus;
    private final BroadcastReceiver updates = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            if (provider == null || !provider.uuid.equals(i.getStringExtra(PROVIDER_ID))) return;
            long now = SystemClock.elapsedRealtime();
            if (now - lastBroadcast < 1000) return;
            lastBroadcast = now; refresh();
        }
    };
    private final Runnable rotate = () -> tick();
    private final Runnable idleTimer = () -> setIdle(true);
    private final Runnable flushFocus = () -> { if (focus != null) request(focus); };
    private final Runnable requestDeadline = () -> fail("Provider did not respond. Retry after it recovers, or restart Homeboard.");
    private final Runnable bindDeadline = () -> fail("Provider could not be connected. Choose Retry.");

    ProjectivyWallpaperHost(Context context, Listener listener) {
        this.context = context; this.listener = listener; registry = new ProjectivyProviderRegistry(context);
        androidx.core.content.ContextCompat.registerReceiver(context, updates, new IntentFilter(UPDATED), androidx.core.content.ContextCompat.RECEIVER_EXPORTED);
    }
    void select(WallpaperSettings next) {
        boolean changed = !settings.identity().equals(next.identity());
        boolean rotationChanged = settings.rotationMinutes != next.rotationMinutes;
        settings = next.copy();
        if (changed) { disconnect(); batch.clear(); expires = 0; nextItem = 0; listener.fallback(); }
        if (settings.solid()) { listener.fallback(); listener.status("Solid color"); return; }
        if (visible && connection == null) connect();
        if (changed || rotationChanged) scheduleRotation();
    }
    void visible(boolean value) {
        if (closed) return;
        visible = value;
        if (value) {
            if (!settings.solid() && connection == null) connect();
            userActivity(); scheduleRotation();
        } else {
            main.removeCallbacks(rotate); main.removeCallbacks(idleTimer); main.removeCallbacks(flushFocus);
            idle = false; disconnect();
        }
    }
    void userActivity() {
        main.removeCallbacks(idleTimer);
        setIdle(false);
        if (visible) main.postDelayed(idleTimer, 60_000);
    }
    void appFocused(String title, String packageName, int light, int dark) {
        focus = new Event.CardFocused(4, light, dark, title, packageName);
        main.removeCallbacks(flushFocus);
        if (visible) main.postDelayed(flushFocus, 120);
    }
    void setIdle(boolean value) {
        if (!visible || idle == value) return;
        idle = value; request(new Event.LauncherIdleModeChanged(16, value));
    }
    void refresh() {
        batch.clear(); expires = 0;
        if (!visible || settings.solid()) return;
        if (remote == null) { disconnect(); connect(); }
        else initialRequest();
    }
    void packagesChanged() {
        if (!settings.solid()) { disconnect(); batch.clear(); expires = 0; connect(); }
    }
    private void connect() {
        if (!visible || closed || settings.solid()) return;
        provider = registry.find(settings);
        if (provider == null || !provider.usable()) { fail(provider == null ? "Selected provider is missing" : provider.problem); return; }
        final long token = ++generation;
        connection = new ServiceConnection() {
            @Override public void onServiceConnected(ComponentName name, IBinder binder) {
                if (token != generation) return;
                main.removeCallbacks(bindDeadline);
                remote = IWallpaperProviderService.Stub.asInterface(binder);
                initialRequest();
            }
            @Override public void onServiceDisconnected(ComponentName name) { if (token == generation) fail("Provider disconnected. Choose Retry."); }
            @Override public void onBindingDied(ComponentName name) { if (token == generation) fail("Provider was updated. Choose Retry."); }
            @Override public void onNullBinding(ComponentName name) { if (token == generation) fail("Provider returned no service"); }
        };
        listener.status("Connecting…");
        try {
            if (!context.bindService(new Intent(ProjectivyProviderRegistry.ACTION).setComponent(provider.component), connection, Context.BIND_AUTO_CREATE)) {
                fail("Provider could not be bound"); return;
            }
            main.postDelayed(bindDeadline, 5000);
        } catch (RuntimeException denied) { fail("Provider connection is unavailable"); }
    }
    private void initialRequest() {
        if (provider == null) return;
        if ((provider.events & 1) != 0) tick();
        else if ((provider.events & 4) != 0 && focus != null) request(focus);
        else if ((provider.events & 16) != 0) request(new Event.LauncherIdleModeChanged(16, idle));
        else listener.status("Focus an app to request a wallpaper");
    }
    private void tick() {
        if (!visible || provider == null || (provider.events & 1) == 0) return;
        if (!batch.isEmpty() && SystemClock.elapsedRealtime() < expires) {
            int start = nextItem++ % batch.size();
            candidates = new ArrayList<>();
            for (int i = 0; i < batch.size(); i++) candidates.add(batch.get((start + i) % batch.size()));
            candidateIndex = 0; listener.wallpaper(candidates.get(0)); scheduleRotation();
        } else request(Event.TimeElapsed.INSTANCE);
    }
    private void scheduleRotation() {
        main.removeCallbacks(rotate);
        if (visible && provider != null && (provider.events & 1) != 0)
            main.postDelayed(rotate, settings.rotationMinutes * 60_000L);
    }
    private void request(Event event) {
        if (!visible || remote == null || provider == null || (provider.events & event.getEventType()) == 0) return;
        if (busy) { pending = event; return; }
        busy = true;
        long token = generation;
        IWallpaperProviderService service = remote;
        listener.status("Loading wallpaper…");
        main.postDelayed(requestDeadline, 10_000);
        try {
            IPC.execute(() -> {
                List<Wallpaper> items = null; String error = null;
                try {
                    List<Wallpaper> result = service.getWallpapers(event);
                    if (result != null && result.size() > 1000) error = "Provider returned too many wallpapers";
                    else if (result != null) {
                        items = new ArrayList<>();
                        for (Wallpaper item : result) if (item != null && !item.getUri().isEmpty()) items.add(item);
                    }
                } catch (Exception invalid) { error = "Provider request failed"; }
                List<Wallpaper> delivered = items; String failure = error;
                main.post(() -> {
                    if (closed || token != generation) return;
                    main.removeCallbacks(requestDeadline); busy = false;
                    if (failure != null) listener.status(failure);
                    else if (delivered == null || delivered.isEmpty()) listener.status("No new wallpaper; retaining current background");
                    else {
                        if (event.getEventType() == 1) {
                            batch.clear(); batch.addAll(delivered); nextItem = 1;
                            expires = SystemClock.elapsedRealtime() + provider.cacheMillis;
                        }
                        candidates = delivered; candidateIndex = 0; listener.wallpaper(delivered.get(0));
                    }
                    scheduleRotation();
                    Event latest = pending; pending = null;
                    if (latest != null) request(latest);
                });
            });
        } catch (RejectedExecutionException hung) {
            main.removeCallbacks(requestDeadline); busy = false;
            listener.status("A provider is still busy. Retry after it recovers, or restart Homeboard.");
        }
    }
    void mediaFailed(String reason) {
        if (++candidateIndex < candidates.size()) {
            List<Wallpaper> snapshot = candidates; int index = candidateIndex; long token = generation;
            main.post(() -> { if (token == generation && candidates == snapshot && index == candidateIndex) listener.wallpaper(snapshot.get(index)); });
        }
        else listener.status(reason + "; no usable replacement in this batch");
    }
    private void fail(String reason) {
        disconnect(); batch.clear(); listener.fallback(); listener.status(reason);
    }
    private void disconnect() {
        generation++; remote = null; busy = false; pending = null; candidates.clear(); candidateIndex = 0;
        main.removeCallbacks(bindDeadline); main.removeCallbacks(requestDeadline); main.removeCallbacks(rotate);
        if (connection != null) try { context.unbindService(connection); } catch (IllegalArgumentException ignored) { }
        connection = null;
    }
    @Override public void close() {
        closed = true; visible = false; disconnect(); main.removeCallbacksAndMessages(null);
        context.unregisterReceiver(updates); listener.fallback();
    }
}
