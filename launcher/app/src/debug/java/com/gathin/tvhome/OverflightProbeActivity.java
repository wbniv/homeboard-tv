package com.gathin.tvhome;

/** Debug-only interoperability check with the separately installed, unmodified Overflight APK. */
public final class OverflightProbeActivity extends android.app.Activity {
    private ProjectivyWallpaperHost host;
    private WallpaperSurface surface;
    private android.widget.TextView report;
    private final android.os.Handler main = new android.os.Handler();
    private boolean finished;
    @Override public void onCreate(android.os.Bundle state) {
        super.onCreate(state);
        android.widget.FrameLayout root = new android.widget.FrameLayout(this); root.setBackgroundColor(0xff102333);
        surface = new WallpaperSurface(this); surface.options(0, false); surface.visible(true);
        root.addView(surface, new android.widget.FrameLayout.LayoutParams(-1, -1));
        report = new android.widget.TextView(this); report.setTextSize(24); report.setTextColor(-1); report.setBackgroundColor(0xdd102333);
        report.setPadding(24,24,24,24); root.addView(report, new android.widget.FrameLayout.LayoutParams(-1, -2)); setContentView(root);
        ProjectivyProviderRegistry.Provider provider = new ProjectivyProviderRegistry(this).discover().stream()
                .filter(p -> p.component.getClassName().contains("overflight")).findFirst().orElse(null);
        if (provider == null) { report.setText("FAIL: Overflight is not installed"); return; }
        host = new ProjectivyWallpaperHost(this, new ProjectivyWallpaperHost.Listener() {
            @Override public void wallpaper(tv.projectivy.plugin.wallpaperprovider.api.Wallpaper item) {
                if (finished) return;
                surface.show(item, (ready, status) -> {
                    if (finished) return;
                    if (ready) { finished = true; report.setText("PASS: unmodified Overflight rendered media type " + item.getType() + "\n" + item.getTitle() + "\n" + item.getAuthor()); }
                    else { report.setText("Trying another item: " + status); host.mediaFailed(status); }
                });
            }
            @Override public void status(String message) { if (!finished) report.setText("Overflight: " + message); }
            @Override public void fallback() { surface.solid(); }
        });
        WallpaperSettings selected = new WallpaperSettings(); selected.component = provider.component.flattenToString(); selected.uuid = provider.uuid;
        host.select(selected); host.visible(true);
        main.postDelayed(() -> { if (!finished) { finished = true; report.setText("FAIL: Overflight did not produce usable media within 50 seconds"); host.visible(false); surface.visible(false); } }, 50_000);
    }
    @Override protected void onDestroy() { if (host != null) host.close(); surface.close(); main.removeCallbacksAndMessages(null); super.onDestroy(); }
}
