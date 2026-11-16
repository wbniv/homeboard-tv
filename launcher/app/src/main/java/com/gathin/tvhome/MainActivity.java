package com.gathin.tvhome;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.app.role.RoleManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Homeboard's widgets-first native Home shell, using CleanYourTV's launcher foundation. */
public class MainActivity extends Activity {
    private static final float APP_FOCUS_SCALE = 1.08f;
    static final String ACTION_LAUNCHER_SETTINGS = "com.gathin.tvhome.action.LAUNCHER_SETTINGS";
    static final String ACTION_APP_LIBRARY = "com.gathin.tvhome.action.APP_LIBRARY";
    static final String ACTION_VIEW_INPUTS = "com.android.tv.action.VIEW_INPUTS";
    static int COLUMNS = 4;
    private static final String SETTINGS = "control:settings";
    private static final String HOME_SETTINGS = "control:homeboard-settings";
    private static final String DEVICE_SETTINGS = "control:device-settings";
    private static final int REQUEST_HOME_ROLE = 7402;
    private static final int MENU_PAGE_SIZE = 4;
    private static final class MenuSpec {
        final String title, subtitle;
        final String[] labels;
        final java.util.function.IntConsumer action;
        int selectedIndex;
        final int pageCount;
        final int pageSize, gridColumns;
        final boolean compactCards, paletteGrid;
        int page, focusedChoice;
        MenuSpec(String title, String subtitle, String[] labels,
                 java.util.function.IntConsumer action, int selectedIndex) {
            this(title, subtitle, labels, action, selectedIndex, MENU_PAGE_SIZE, 4, false, false);
        }
        MenuSpec(String title, String subtitle, String[] labels,
                 java.util.function.IntConsumer action, int selectedIndex,
                 int pageSize, int gridColumns, boolean compactCards, boolean paletteGrid) {
            this.title = title; this.subtitle = subtitle; this.labels = labels;
            this.action = action; this.selectedIndex = selectedIndex;
            this.pageSize = pageSize; this.gridColumns = gridColumns; this.compactCards = compactCards;
            this.paletteGrid = paletteGrid;
            pageCount = Math.max(1, (labels.length + pageSize - 1) / pageSize);
        }
    }
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService loader = Executors.newSingleThreadExecutor();
    private final Map<String, App> apps = new LinkedHashMap<>();
    private final Map<String, View> tiles = new LinkedHashMap<>();
    private final List<HomeLayout.Cell> visibleCells = new ArrayList<>();
    private FrameLayout root, surface, overlay;
    private GlassView appFocusHighlight;
    private SpringAnimation appFocusX, appFocusY;
    private boolean appFocusHighlightShown;
    private boolean restoringSurface = true;
    private final List<MenuSpec> menuStack = new ArrayList<>();
    private MenuSpec activeMenu;
    private boolean invokingMenuAction;
    private int menuGeneration;
    private TextView footer;
    private SharedPreferences preferences;
    private HomeLayout layout;
    private final Map<String, AirQualityCard> airWidgets = new LinkedHashMap<>();
    private final List<Consumer<Location>> pendingLocationRequests = new ArrayList<>();
    private final Set<AirLocationLookup> activeLocationLookups = new LinkedHashSet<>();
    private Location cachedLocation;
    private long cachedLocationAt;
    private static final int REQUEST_AIR_LOCATION = 7403;
    private boolean allApps, active, closed, loading, moving, moveInApps, okHeld, consumeOkUp;
    private boolean configReducedMotion;
    private int homePage, appsPage, loadVersion;
    private String focused = HomeLayout.AIR, homeFocus = HomeLayout.AIR, appsFocus = "";
    private List<String> preview;
    private Map<String, HomeLayout.HomePosition> previewHomePositions;
    private String movingKey;
    private int background;
    private PaletteColors paletteColors = PaletteColors.from(0xff101010);
    private boolean resizing;
    private String resizingKey;
    private int resizeOriginalWidth, resizeOriginalHeight, resizeWidth, resizeHeight;
    private HomeLayout.PanelSpan resizePreviousSpan;

    private static final class App {
        final String key, label;
        final ComponentName component;
        final ResolveInfo info;
        App(ResolveInfo info, PackageManager pm) {
            this.info = info;
            component = new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
            key = "app:" + component.flattenToString();
            label = Cards.cleanLabel(String.valueOf(info.loadLabel(pm)), info.activityInfo.packageName);
        }
    }
    private final BroadcastReceiver packages = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction())
                    && !intent.getBooleanExtra(Intent.EXTRA_REPLACING, false) && intent.getData() != null) {
                layout.packageRemoved(intent.getData().getSchemeSpecificPart());
                save();
            }
            loadApps();
        }
    };
    private final Runnable heldOk = () -> {
        okHeld = false;
        consumeOkUp = true;
        if (moving) return;
        if (!allApps && layout.widgets.isEmpty()) homeSettings();
        else contextMenu(focused);
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences("widget-home", MODE_PRIVATE);
        String encoded = preferences.getString("layout", null);
        try { layout = encoded == null ? HomeLayout.firstRun() : HomeLayout.parse(encoded); }
        catch (Exception e) {
            // Preserve an unreadable user's layout for diagnosis; never replace it silently with defaults.
            preferences.edit().putString("unreadable-layout", encoded).apply();
            layout = new HomeLayout();
            Toast.makeText(this, "Saved layout could not be read. Starting with empty Home.", Toast.LENGTH_LONG).show();
        }
        if ("theme".equals(layout.palette)) {
            // Wallpaper selection is not part of Homeboard's current appearance controls.
            // Move former theme defaults to a deterministic palette; retain any saved image data.
            layout.palette = "midnight";
            save();
        }
        if (encoded == null) save();
        Config config = Config.load(this);
        configReducedMotion = config.reducedMotion;
        background = paletteColor();
        paletteColors = PaletteColors.from(background);
        root = new FrameLayout(this);
        root.setBackgroundColor(background);
        setContentView(root);
        if (savedInstanceState != null) {
            allApps = savedInstanceState.getBoolean("allApps");
            homePage = savedInstanceState.getInt("homePage");
            appsPage = savedInstanceState.getInt("appsPage");
            focused = savedInstanceState.getString("focus", HomeLayout.AIR);
            homeFocus = savedInstanceState.getString("homeFocus", HomeLayout.AIR);
            appsFocus = savedInstanceState.getString("appsFocus", "");
        } else {
            allApps = preferences.getBoolean("nav_all_apps", false);
            homePage = preferences.getInt("nav_home_page", 0);
            appsPage = preferences.getInt("nav_apps_page", 0);
            focused = preferences.getString("nav_focus", HomeLayout.AIR);
            homeFocus = preferences.getString("nav_home_focus", HomeLayout.AIR);
            appsFocus = preferences.getString("nav_apps_focus", "");
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        registerReceiver(packages, filter);
        render(0);
        loadApps();
        handleIntent(getIntent());
    }

    @Override protected void onResume() {
        super.onResume(); active = true;
        updateAir();
        // Package broadcasts reconcile installs, removal and enable/disable changes. Avoid rebuilding
        // the same surface on every return from an app: that used to drop remote focus for a frame.
        root.post(() -> {
            if (active && overlay == null) {
                restoreFocus();
                snapDenseAppHighlight();
            }
        });
    }
    @Override protected void onPause() {
        active = false; restoringSurface = true; cancelOk();
        menuStack.clear(); activeMenu = null;
        dismissMenu();
        if (moving) cancelMove();
        saveNavigationState();
        for (AirQualityCard widget : airWidgets.values()) widget.stop();
        super.onPause();
    }
    @Override protected void onDestroy() {
        closed = true; loadVersion++;
        unregisterReceiver(packages);
        handler.removeCallbacksAndMessages(null);
        loader.shutdownNow(); for (AirQualityCard widget : airWidgets.values()) widget.close();
        super.onDestroy();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("allApps", allApps); state.putInt("homePage", homePage);
        state.putInt("appsPage", appsPage); state.putString("focus", focused);
        state.putString("homeFocus", homeFocus); state.putString("appsFocus", appsFocus);
        super.onSaveInstanceState(state);
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_HOME_ROLE) {
            toast(isDefaultHomeApp() ? "Homeboard TV is now the default Home app"
                    : "Default Home app was not changed");
            render(0);
            return;
        }
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent); setIntent(intent);
        menuStack.clear(); activeMenu = null;
        dismissMenu(); cancelOk(); moving = false; preview = null;
        allApps = false;
        focused = layout.widgets.contains(homeFocus) ? homeFocus
                : (layout.widgets.isEmpty() ? "" : layout.widgets.get(0));
        homePage = focused.isEmpty() ? 0 : homePageOf(focused, layout.widgets);
        homeFocus = focused;
        saveNavigationState();
        render(-1); handleIntent(intent);
    }
    private void handleIntent(Intent intent) {
        if (intent == null) return;
        if (ACTION_APP_LIBRARY.equals(intent.getAction())) showApps();
        else if (ACTION_LAUNCHER_SETTINGS.equals(intent.getAction())) homeSettings();
        else if (ACTION_VIEW_INPUTS.equals(intent.getAction())) open(new Intent(ACTION_VIEW_INPUTS));
    }
    private void save() { preferences.edit().putString("layout", layout.encode()).apply(); }
    private void saveNavigationState() {
        preferences.edit().putBoolean("nav_all_apps", allApps)
                .putInt("nav_home_page", homePage).putInt("nav_apps_page", appsPage)
                .putString("nav_focus", focused).putString("nav_home_focus", homeFocus)
                .putString("nav_apps_focus", appsFocus).apply();
    }
    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private int paletteColor() {
        return paletteColorForId(layout.palette);
    }
    static int paletteColorForId(String palette) {
        switch (palette) {
            case "charcoal": return Color.rgb(32, 34, 36);
            case "indigo": return Color.rgb(20, 29, 53);
            case "cobalt": return Color.rgb(13, 39, 67);
            case "lagoon": return Color.rgb(22, 52, 56);
            case "pine": return Color.rgb(18, 43, 32);
            case "moss": return Color.rgb(43, 53, 37);
            case "emerald": return Color.rgb(12, 47, 39);
            case "olive": return Color.rgb(49, 47, 29);
            case "cedar": return Color.rgb(57, 39, 30);
            case "copper": return Color.rgb(62, 34, 24);
            case "amber": return Color.rgb(57, 43, 25);
            case "plumwine": return Color.rgb(51, 27, 43);
            case "mulberry": return Color.rgb(55, 32, 52);
            case "berry": return Color.rgb(59, 29, 38);
            case "graphite": return Color.rgb(40, 39, 42);
            case "smoke": return Color.rgb(48, 47, 49);
            case "laguneblue": return Color.rgb(23, 42, 52);
            case "arctic": return Color.rgb(24, 42, 58);
            case "deepteal": return Color.rgb(12, 42, 44);
            case "earth": return Color.rgb(50, 43, 34);
            case "forest": return Color.rgb(12, 30, 26);
            case "black": return Color.rgb(0, 0, 0);
            case "ocean": return Color.rgb(8, 37, 61);
            case "plum": return Color.rgb(46, 24, 54);
            case "sand": return Color.rgb(61, 47, 29);
            case "rose": return Color.rgb(60, 29, 39);
            case "teal": return Color.rgb(10, 48, 48);
            case "slate": return Color.rgb(35, 42, 54);
            case "sunset": return Color.rgb(66, 35, 30);
            case "midnight":
            default: return Color.rgb(16, 23, 35);
        }
    }
    private int contrastingText(int color) {
        double luminance = (0.2126 * Color.red(color) + 0.7152 * Color.green(color)
                + 0.0722 * Color.blue(color)) / 255.0;
        return luminance > 0.52 ? Color.rgb(24, 27, 30) : Color.WHITE;
    }

    private void loadApps() {
        if (closed || loading) return;
        loading = true;
        int version = ++loadVersion;
        Set<String> preferredKeys = new LinkedHashSet<>();
        preferredKeys.addAll(layout.widgets);
        preferredKeys.addAll(layout.order);
        preferredKeys.addAll(layout.hidden);
        loader.execute(() -> {
            List<App> found = new ArrayList<>();
            Map<String, List<ResolveInfo>> byPackage = new LinkedHashMap<>();
            PackageManager pm = getPackageManager();
            try {
                for (String category : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
                    for (ResolveInfo info : pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(category), 0)) {
                        if (getPackageName().equals(info.activityInfo.packageName)) continue;
                        App app = new App(info, pm);
                        if (isDeviceSettings(app, pm)) continue;
                        if (isDeveloperLogApp(app)) continue;
                        byPackage.computeIfAbsent(info.activityInfo.packageName, ignored -> new ArrayList<>()).add(info);
                    }
                }
                for (List<ResolveInfo> candidates : byPackage.values()) {
                    ResolveInfo chosen = candidates.get(0); // Leanback entries were queried first.
                    for (ResolveInfo candidate : candidates) {
                        String candidateKey = "app:" + new ComponentName(candidate.activityInfo.packageName,
                                candidate.activityInfo.name).flattenToString();
                        if (preferredKeys.contains(candidateKey)) { chosen = candidate; break; }
                    }
                    found.add(new App(chosen, pm));
                }
                found.sort(Comparator.comparing(a -> a.label.toLowerCase(java.util.Locale.ROOT)));
            } catch (RuntimeException e) {
                handler.post(() -> { loading = false; if (!closed) toast("Could not refresh installed apps"); });
                return;
            }
            handler.post(() -> {
                if (closed || version != loadVersion) return;
                loading = false;
                boolean migrated = false;
                for (App app : found) {
                    migrated |= layout.canonicalizeAppPackage(app.component.getPackageName(), app.key);
                }
                apps.clear();
                for (App app : found) apps.put(app.key, app);
                if (migrated) save();
                // A missing activity may be temporarily disabled. Keep its dashboard tile, marked unavailable.
                if (!moving && overlay == null) render(0);
            });
        });
    }
    private boolean isDeviceSettings(App app, PackageManager pm) {
        String pkg = app.component.getPackageName();
        if ("com.android.settings".equals(pkg) || "com.android.tv.settings".equals(pkg)
                || "com.google.android.tv.settings".equals(pkg)) return true;
        android.content.pm.ResolveInfo resolved = pm.resolveActivity(new Intent(Settings.ACTION_SETTINGS),
                PackageManager.MATCH_DEFAULT_ONLY);
        return resolved != null && resolved.activityInfo != null
                && pkg.equals(resolved.activityInfo.packageName);
    }
    private boolean isDeveloperLogApp(App app) {
        String label = app.label.trim().toLowerCase(java.util.Locale.ROOT);
        String pkg = app.component.getPackageName().toLowerCase(java.util.Locale.ROOT);
        String activity = app.component.getClassName().toLowerCase(java.util.Locale.ROOT);
        return containsLogUtilityTerm(label) || containsLogUtilityTerm(activity)
                || containsLogUtilityTerm(pkg) || pkg.endsWith(".logs");
    }
    private boolean containsLogUtilityTerm(String value) {
        return value.matches(".*\\b(log|logs|logcat|logfile|logfiles)\\b.*")
                || value.contains("log_viewer") || value.contains("logviewer");
    }
    private List<String> dashboardItems() { return moving && !moveInApps ? preview : layout.widgets; }
    private int homeGridColumns() {
        int fit = homeGridFitColumns();
        return layout.homeColumns == 0 ? Math.min(9, fit) : Math.min(layout.homeColumns, fit);
    }
    private int homeGridFitColumns() {
        int screenWidthDp = Math.round(getResources().getDisplayMetrics().widthPixels
                / getResources().getDisplayMetrics().density);
        return Math.max(4, Math.min(12, Math.max(1, screenWidthDp - 88) / 108));
    }
    private int homeGridFitRows() {
        int screenHeightDp = Math.round(getResources().getDisplayMetrics().heightPixels
                / getResources().getDisplayMetrics().density);
        // Keep 48 dp icons and a 16 sp label readable, with room for the top and bottom chrome.
        return Math.max(2, (screenHeightDp - 168) / 80);
    }
    private int homeGridRows() {
        int fit = homeGridFitRows();
        return layout.homeRows == 0 ? fit : Math.min(layout.homeRows, fit);
    }
    private List<HomeLayout.Cell> homeCells(List<String> items) {
        Map<String, HomeLayout.HomePosition> positions = moving && !moveInApps
                ? previewHomePositions : layout.homePositions;
        return layout.cells(items, homeGridColumns(), homeGridRows(), positions);
    }
    private int homePageOf(String key, List<String> items) {
        for (HomeLayout.Cell cell : homeCells(items)) if (cell.key.equals(key)) return cell.page;
        return 0;
    }
    private int homePageCount(List<String> items) {
        int pages = 0;
        for (HomeLayout.Cell cell : homeCells(items)) pages = Math.max(pages, cell.page + 1);
        return Math.max(1, pages);
    }
    private List<String> appItems() {
        return moving && moveInApps ? preview : layout.visibleApps(new ArrayList<>(apps.keySet()));
    }
    private boolean reducedMotion() {
        return layout.reducedMotion || configReducedMotion || !android.animation.ValueAnimator.areAnimatorsEnabled();
    }

    private void render(int direction) {
        if (closed) return;
        String requestedFocus = focused;
        FrameLayout old = surface;
        if (appFocusX != null) appFocusX.cancel();
        if (appFocusY != null) appFocusY.cancel();
        appFocusHighlight = null; appFocusX = null; appFocusY = null; appFocusHighlightShown = false;
        if (old != null) { old.animate().cancel(); root.removeView(old); }
        for (AirQualityCard widget : airWidgets.values()) widget.unbind();
        tiles.clear(); visibleCells.clear();
        surface = new FrameLayout(this);
        surface.setFocusable(true);
        surface.setFocusableInTouchMode(true);
        surface.setClipChildren(false);
        root.addView(surface, new FrameLayout.LayoutParams(-1, -1));
        if (overlay != null) overlay.bringToFront();
        if (allApps) buildApps(); else buildHome();
        updateAir();
        focused = requestedFocus;
        if (overlay == null) restoreFocus();
        if (direction != 0 && !reducedMotion()) {
            // Both surfaces stay in the same window. Outgoing content remains visible during the slide.
            int width = getResources().getDisplayMetrics().widthPixels;
            surface.setTranslationX(direction * width);
            surface.animate().translationX(0).setDuration(420).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
            if (old != null) {
                old.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
                old.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
                root.addView(old, Math.max(0, root.indexOfChild(surface)));
                old.setTranslationX(0);
                old.animate().translationX(-direction * width).setDuration(420).setInterpolator(new android.view.animation.DecelerateInterpolator())
                        .withEndAction(() -> root.removeView(old)).start();
            }
        }
        FrameLayout renderedSurface = surface;
        root.post(() -> {
            if (active && overlay == null && renderedSurface == surface) snapDenseAppHighlight();
        });
    }
    private void updateAir() {
        boolean visible = active && !allApps;
        Set<String> onPage = new LinkedHashSet<>();
        for (HomeLayout.Cell c : homeCells(dashboardItems()))
            if (isAirWidgetKey(c.key) && c.page == homePage) onPage.add(c.key);
        for (Map.Entry<String, AirQualityCard> entry : airWidgets.entrySet()) {
            if (visible && onPage.contains(entry.getKey())) entry.getValue().start();
            else entry.getValue().stop();
        }
    }
    private boolean isAirWidgetKey(String key) {
        return key != null && (HomeLayout.AIR.equals(key) || key.startsWith(HomeLayout.AIR + ":"));
    }
    private AirQualityCard airFor(String key) {
        AirQualityCard widget = airWidgets.get(key);
        if (widget == null) {
            widget = new AirQualityCard(this, this::menu, key, this::requestAirLocation);
            widget.setPalette(paletteColors); airWidgets.put(key, widget);
        }
        return widget;
    }
    private void setAirPalette() {
        for (AirQualityCard widget : airWidgets.values()) widget.setPalette(paletteColors);
    }
    private void requestAirLocation(Consumer<Location> callback) {
        long now = System.currentTimeMillis();
        if (cachedLocation != null && now - cachedLocationAt < 15 * 60_000L) {
            callback.accept(new Location(cachedLocation)); return;
        }
        boolean coarse = checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean fine = checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        if (!coarse && !fine) {
            pendingLocationRequests.add(callback);
            if (pendingLocationRequests.size() == 1)
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION,
                        android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_AIR_LOCATION);
            return;
        }
        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (manager == null) { callback.accept(null); return; }
        Location best = null;
        try {
            for (String provider : manager.getProviders(true)) {
                Location location = manager.getLastKnownLocation(provider);
                if (location != null && (best == null || location.getTime() > best.getTime())) best = location;
            }
        } catch (SecurityException ignored) { }
        if (best != null && now - best.getTime() < 6 * 60 * 60_000L) {
            cachedLocation = new Location(best); cachedLocationAt = now; callback.accept(new Location(best)); return;
        }
        AirLocationLookup lookup = new AirLocationLookup(manager, callback);
        activeLocationLookups.add(lookup); lookup.start();
    }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_AIR_LOCATION) return;
        boolean granted = false;
        for (int result : grantResults) if (result == PackageManager.PERMISSION_GRANTED) granted = true;
        List<Consumer<Location>> callbacks = new ArrayList<>(pendingLocationRequests);
        pendingLocationRequests.clear();
        for (Consumer<Location> callback : callbacks) {
            if (granted) requestAirLocation(callback); else callback.accept(null);
        }
    }
    private final class AirLocationLookup implements LocationListener {
        private final LocationManager manager;
        private final Consumer<Location> callback;
        private final Set<String> providers = new LinkedHashSet<>();
        private boolean finished;
        private final Runnable timeout = () -> finish(null);
        AirLocationLookup(LocationManager manager, Consumer<Location> callback) {
            this.manager = manager; this.callback = callback;
        }
        void start() {
            try {
                for (String provider : manager.getProviders(true)) {
                    try { manager.requestLocationUpdates(provider, 1000L, 0f, this, Looper.getMainLooper()); providers.add(provider); }
                    catch (SecurityException | IllegalArgumentException ignored) { }
                }
            } catch (SecurityException ignored) { }
            if (providers.isEmpty()) finish(null);
            else handler.postDelayed(timeout, 15_000L);
        }
        @Override public void onLocationChanged(Location location) { finish(location); }
        @Override public void onProviderDisabled(String provider) { }
        @Override public void onProviderEnabled(String provider) { }
        @SuppressWarnings("deprecation") @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
        private void finish(Location location) {
            if (finished) return;
            finished = true; handler.removeCallbacks(timeout); activeLocationLookups.remove(this);
            for (String provider : providers) try { manager.removeUpdates(this); break; } catch (SecurityException ignored) { }
            if (location != null) { cachedLocation = new Location(location); cachedLocationAt = System.currentTimeMillis(); }
            callback.accept(location == null ? null : new Location(location));
        }
    }
    private TextView text(String value, float size) {
        TextView v = new TextView(this);
        v.setText(value); v.setTextSize(size); v.setTextColor(paletteColors.text);
        v.setFontFeatureSettings("kern");
        return v;
    }
    private StateListDrawable tileBackground() {
        StateListDrawable states = new StateListDrawable();
        GradientDrawable selected = shape(paletteColors.raised, paletteColors.focus, 3);
        states.addState(new int[]{android.R.attr.state_focused}, selected);
        states.addState(new int[]{}, shape(paletteColors.surface, paletteColors.edge, 1));
        return states;
    }
    private GradientDrawable shape(int color, int stroke, int width) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(14));
        d.setStroke(dp(width), stroke); return d;
    }
    private void addTopSettingsButtons() {
        addTopSettingsButton(HOME_SETTINGS, R.drawable.ic_app_settings, "Homeboard settings",
                this::homeSettings, dp(110));
        addTopSettingsButton(DEVICE_SETTINGS, R.drawable.ic_settings, "Device settings",
                () -> open(new Intent(Settings.ACTION_SETTINGS)), dp(50));
    }
    private void addTopSettingsButton(String key, int icon, String description, Runnable action, int rightMargin) {
        ImageView button = new ImageView(this);
        button.setImageResource(icon);
        button.setContentDescription(description);
        button.setFocusable(true); button.setClickable(true);
        StateListDrawable states = new StateListDrawable();
        GradientDrawable selected = shape(paletteColors.raised, paletteColors.focus, 2);
        selected.setCornerRadius(dp(28));
        GradientDrawable normal = shape(paletteColors.surface, paletteColors.edge, 1);
        normal.setCornerRadius(dp(28));
        states.addState(new int[]{android.R.attr.state_focused}, selected);
        states.addState(new int[]{}, normal);
        button.setBackground(states);
        button.setPadding(dp(13), dp(13), dp(13), dp(13));
        button.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) { focused = key; if (allApps) appsFocus = key; else homeFocus = key; }
        });
        button.setOnClickListener(v -> action.run());
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(52), dp(52), Gravity.TOP | Gravity.RIGHT);
        lp.setMargins(0, dp(20), rightMargin, 0);
        surface.addView(button, lp);
        tiles.put(key, button);
    }
    private void configureTile(View view, String key) {
        boolean denseApp = key.startsWith("app:") && (!allApps || !layout.appBanners);
        view.setFocusable(true);
        view.setBackground(denseApp ? null : tileBackground());
        view.setPadding(dp(denseApp ? 2 : 16), dp(denseApp ? 2 : 12),
                dp(denseApp ? 2 : 16), dp(denseApp ? 2 : 12));
        view.setOnFocusChangeListener((v, hasFocus) -> {
            if (isAirWidgetKey(key)) airFor(key).setTileFocused(hasFocus);
            boolean animateTile = hasFocus && denseApp && !reducedMotion();
            view.animate().cancel();
            view.animate().scaleX(hasFocus && denseApp ? APP_FOCUS_SCALE : 1f)
                    .scaleY(hasFocus && denseApp ? APP_FOCUS_SCALE : 1f)
                    .setDuration(animateTile ? 180 : 0)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
            if (hasFocus) {
                focused = key; if (allApps) appsFocus = key; else homeFocus = key;
                if (denseApp) moveAppFocusHighlight(appHighlightTarget(view), !restoringSurface);
            }
        });
        view.setOnClickListener(v -> activate(key));
        view.setOnLongClickListener(v -> { contextMenu(key); return true; });
        tiles.put(key, view);
    }
    private void createAppFocusHighlight() {
        appFocusHighlight = new GlassView(this, null);
        appFocusHighlight.setPalette(background);
        appFocusHighlight.setRadius(dp(18));
        appFocusHighlight.setInset(0);
        appFocusHighlight.setAlpha(0f);
        appFocusHighlight.setFocusable(false);
        appFocusHighlight.setClickable(false);
        appFocusHighlight.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        surface.addView(appFocusHighlight, new FrameLayout.LayoutParams(dp(100), dp(100)));
        FloatPropertyCompat<View> x = new FloatPropertyCompat<View>("appFocusX") {
            @Override public float getValue(View view) { return view.getTranslationX(); }
            @Override public void setValue(View view, float value) { view.setTranslationX(value); }
        };
        FloatPropertyCompat<View> y = new FloatPropertyCompat<View>("appFocusY") {
            @Override public float getValue(View view) { return view.getTranslationY(); }
            @Override public void setValue(View view, float value) { view.setTranslationY(value); }
        };
        appFocusX = new SpringAnimation(appFocusHighlight, x);
        appFocusY = new SpringAnimation(appFocusHighlight, y);
        SpringForce spring = new SpringForce().setStiffness(260f).setDampingRatio(0.58f);
        appFocusX.setSpring(spring);
        appFocusY.setSpring(new SpringForce().setStiffness(260f).setDampingRatio(0.58f));
        if (reducedMotion()) { appFocusX.setSpring(new SpringForce().setStiffness(1000f).setDampingRatio(1f));
            appFocusY.setSpring(new SpringForce().setStiffness(1000f).setDampingRatio(1f)); }
    }
    private void moveAppFocusHighlight(View card, boolean animate) {
        if (appFocusHighlight == null || card.getParent() == null) return;
        Rect rect = new Rect(0, 0, card.getWidth(), card.getHeight());
        surface.offsetDescendantRectToMyCoords(card, rect);
        // The outline is clipped at this view's bounds, so one physical pixel of
        // view padding gives the same one-pixel breathing gap as the system launcher.
        int padding = 1;
        int width = (int) Math.ceil(card.getWidth() * APP_FOCUS_SCALE) + padding * 2;
        int height = (int) Math.ceil(card.getHeight() * APP_FOCUS_SCALE) + padding * 2;
        appFocusHighlight.setRadius(layout.circularIcons ? Math.min(width, height) / 2f : dp(10));
        ViewGroup.LayoutParams lp = appFocusHighlight.getLayoutParams();
        lp.width = width; lp.height = height;
        appFocusHighlight.setLayoutParams(lp);
        float x = rect.exactCenterX() - width / 2f;
        float y = rect.exactCenterY() - height / 2f;
        if (!appFocusHighlightShown || !animate || reducedMotion()) {
            appFocusX.cancel(); appFocusY.cancel();
            appFocusHighlight.setTranslationX(x); appFocusHighlight.setTranslationY(y);
        } else {
            appFocusX.animateToFinalPosition(x);
            appFocusY.animateToFinalPosition(y);
        }
        appFocusHighlightShown = true;
        appFocusHighlight.setAlpha(1f);
    }
    private View appHighlightTarget(View tile) {
        if (tile instanceof ViewGroup && ((ViewGroup) tile).getChildCount() > 0
                && ((ViewGroup) tile).getChildAt(0) instanceof ImageView)
            return ((ViewGroup) tile).getChildAt(0);
        return tile;
    }
    private void buildHome() {
        addTopSettingsButtons();
        List<String> items = dashboardItems();
        int columns = homeGridColumns(), rows = homeGridRows();
        homePage = Math.max(0, Math.min(homePage, homePageCount(items) - 1));
        if (items.isEmpty()) { focused = ""; return; } // Truly empty: no compulsory header, hint or dock.
        TileGrid grid = new TileGrid(this, columns, rows);
        FrameLayout.LayoutParams gp = new FrameLayout.LayoutParams(-1, -1);
        gp.setMargins(dp(44), dp(104), dp(44), dp(64));
        surface.addView(grid, gp);
        boolean hasAppTile = false;
        for (HomeLayout.Cell cell : homeCells(items)) {
            if (cell.page != homePage) continue;
            visibleCells.add(cell);
            if (cell.key.startsWith("app:")) hasAppTile = true;
            View tile = isAirWidgetKey(cell.key) ? airTile(cell.key, cell.width, cell.height) : appTile(cell.key, true);
            configureTile(tile, cell.key);
            if (isAirWidgetKey(cell.key)) airFor(cell.key).bind(tile);
            grid.addTile(tile, cell.column, cell.row, cell.width, cell.height);
        }
        if (hasAppTile) createAppFocusHighlight();
        footer = text(resizing ? resizeHelpText() : moving ? "Move preview · arrows place anywhere in the grid · OK saves · Back cancels"
                : "Home " + (homePage + 1) + "/" + homePageCount(items) + "     Right → All Apps     Hold OK → menu", 13);
        if (resizing) {
            footer.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
            footer.setPadding(dp(16), 0, dp(16), 0);
            footer.setSingleLine(true); footer.setEllipsize(android.text.TextUtils.TruncateAt.END);
            footer.setBackground(shape(paletteColors.raised, paletteColors.edge, 1));
        }
        FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams(-1, dp(40), Gravity.BOTTOM);
        fp.setMargins(dp(resizing ? 44 : 50), 0, dp(resizing ? 44 : 50), dp(10)); surface.addView(footer, fp);
    }
    private View airTile(String key, int widthCells, int heightCells) {
        LinearLayout tile = new LinearLayout(this); tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER_VERTICAL);
        int panelSize = layout.panelSize(key);
        boolean compact = panelSize == HomeLayout.PANEL_SMALL || widthCells == 1 || heightCells == 1;
        boolean large = panelSize == HomeLayout.PANEL_LARGE && widthCells >= 3 && heightCells >= 3;
        TextView station = text("", heightCells == 1 ? 9 : compact ? 12 : large ? 15 : 14); station.setId(R.id.airQualityStation);
        TextView value = text("", heightCells == 1 ? 15 : compact ? 19 : large ? 26 : 22); value.setId(R.id.airQualityValue);
        TextView condition = text("", heightCells == 1 ? 8 : compact ? 10 : large ? 13 : 12); condition.setId(R.id.airQualityCondition);
        condition.setTypeface(null, Typeface.BOLD); condition.setSingleLine(true);
        condition.setEllipsize(android.text.TextUtils.TruncateAt.END);
        AirQualityTrendView trend = new AirQualityTrendView(this);
        trend.setId(R.id.airQualityTrend);
        LinearLayout.LayoutParams trendLp = new LinearLayout.LayoutParams(-1, dp(compact ? 38 : large ? 48 : 42));
        trend.setVisibility(widthCells >= 2 && heightCells >= 2 ? View.VISIBLE : View.GONE);
        TextView detail = text("", compact ? 9 : large ? 11 : 10); detail.setId(R.id.airQualityDetail);
        detail.setVisibility(widthCells >= 2 && heightCells >= 2 ? View.VISIBLE : View.GONE);
        detail.setMaxLines(3);
        tile.addView(station); tile.addView(value); tile.addView(condition);
        tile.addView(trend, trendLp); tile.addView(detail);
        return tile;
    }
    private View appTile(String key, boolean dashboard) {
        App app = apps.get(key);
        LinearLayout tile = new LinearLayout(this); tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        if (app != null) {
            ImageView icon = new ImageView(this);
            icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            boolean compact = dashboard || !layout.appBanners;
            boolean useBanner = !dashboard && layout.appBanners;
            try {
                android.graphics.drawable.Drawable image = useBanner
                        ? app.info.activityInfo.loadBanner(getPackageManager()) : null;
                if (image == null) image = app.info.loadIcon(getPackageManager());
                icon.setImageDrawable(image);
            } catch (RuntimeException ignored) { }
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            if (useBanner) {
                tile.addView(icon, new LinearLayout.LayoutParams(-1, 0, 1));
            } else {
                int iconSize = dp(compact ? 48 : 76);
                LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(iconSize, iconSize);
                iconLp.bottomMargin = dp(compact ? 3 : 8);
                tile.addView(icon, iconLp);
                if (layout.circularIcons) {
                    icon.setClipToOutline(true);
                    icon.setOutlineProvider(new android.view.ViewOutlineProvider() {
                        @Override public void getOutline(View view, android.graphics.Outline outline) {
                            outline.setOval(0, 0, view.getWidth(), view.getHeight());
                        }
                    });
                }
            }
        }
        String label = app == null ? "Unavailable app" : app.label;
        boolean compact = dashboard || !layout.appBanners;
        TextView name = text(label, 16);
        name.setGravity(Gravity.CENTER); name.setMaxLines(compact ? 1 : 2);
        if (compact) {
            name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            tile.setPadding(dp(2), dp(1), dp(2), dp(1));
        }
        tile.addView(name, new LinearLayout.LayoutParams(-1, -2));
        tile.setContentDescription(label + (app == null ? "; hold OK to remove" : "; hold OK for menu"));
        return tile;
    }
    private void buildApps() {
        addTopSettingsButtons();
        List<String> items = appItems();
        int columns = appColumns(), rows = appRows(), pageSize = appPageSize();
        appsPage = Math.max(0, Math.min(appsPage, Math.max(0, (items.size() - 1) / pageSize)));
        TextView title = text("All Apps", 30); title.setTypeface(null, Typeface.BOLD);
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(-2, dp(50));
        tp.setMargins(dp(50), dp(22), 0, 0); surface.addView(title, tp);
        TileGrid grid = new TileGrid(this, columns, rows);
        FrameLayout.LayoutParams gp = new FrameLayout.LayoutParams(-1, -1);
        gp.setMargins(dp(44), dp(compactApps() ? 78 : 86), dp(44), dp(54)); surface.addView(grid, gp);
        int start = appsPage * pageSize;
        for (int i = start; i < Math.min(start + pageSize, items.size()); i++) {
            String key = items.get(i); View tile = appTile(key, false); configureTile(tile, key);
            grid.addTile(tile, (i - start) % columns, (i - start) / columns, 1);
        }
        if (compactApps()) createAppFocusHighlight();
        footer = text(moving ? "Move preview · arrows reposition · OK saves · Back cancels"
                : "Left edge / Back → Home    Hold OK → menu    Page " + (appsPage + 1)
                    + "    " + items.size() + " apps", 13);
        FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams(-1, dp(36), Gravity.BOTTOM);
        fp.setMargins(dp(50), 0, dp(50), dp(10)); surface.addView(footer, fp);
    }
    /** Measures each cell once before layout; never requests another layout while laying out. */
    private boolean compactApps() { return !layout.appBanners; }
    private int appColumns() {
        if (!compactApps()) return 4;
        int screenWidthDp = Math.round(getResources().getDisplayMetrics().widthPixels
                / getResources().getDisplayMetrics().density);
        int gridWidthDp = Math.max(1, screenWidthDp - 88);
        return Math.max(1, gridWidthDp / 108);
    }
    private int appRows() { return compactApps() ? 4 : 2; }
    private int appPageSize() { return appColumns() * appRows(); }
    private final class TileGrid extends ViewGroup {
        private final int columns, rows;
        private final List<int[]> positions = new ArrayList<>();
        TileGrid(Context context, int columns, int rows) {
            super(context); this.columns = columns; this.rows = rows;
            setClipChildren(false); setClipToPadding(false);
        }
        void addTile(View tile, int column, int row, int span) {
            addTile(tile, column, row, span, 1);
        }
        void addTile(View tile, int column, int row, int spanWidth, int spanHeight) {
            positions.add(new int[]{column, row, spanWidth, spanHeight});
            addView(tile, new ViewGroup.LayoutParams(0, 0));
        }
        @Override protected void onMeasure(int widthSpec, int heightSpec) {
            int width = View.MeasureSpec.getSize(widthSpec);
            int height = View.MeasureSpec.getSize(heightSpec);
            setMeasuredDimension(width, height);
            for (int i = 0; i < getChildCount(); i++) {
                int[] cell = positions.get(i);
                int childWidth = Math.max(0, width * cell[2] / columns - dp(12));
                int childHeight = Math.max(0, height * cell[3] / rows - dp(8));
                getChildAt(i).measure(View.MeasureSpec.makeMeasureSpec(childWidth, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(childHeight, View.MeasureSpec.EXACTLY));
            }
        }
        @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            int width = right - left, height = bottom - top;
            for (int i = 0; i < getChildCount(); i++) {
                int[] cell = positions.get(i);
                View child = getChildAt(i);
                int x = width * cell[0] / columns + dp(6);
                int y = height * cell[1] / rows + dp(4);
                child.layout(x, y, x + child.getMeasuredWidth(), y + child.getMeasuredHeight());
            }
        }
    }
    private void restoreFocus() {
        View target = tiles.get(focused);
        if (target == null && allApps) {
            List<String> items = appItems();
            if (!items.isEmpty()) {
                int index = items.indexOf(focused);
                if (index < 0) index = Math.min(appsPage * appPageSize(), items.size() - 1);
                focused = items.get(index);
                target = tiles.get(focused);
            }
        }
        if (target == null && !allApps) {
            for (HomeLayout.Cell cell : homeCells(dashboardItems())) {
                if (cell.page == homePage && tiles.containsKey(cell.key)) {
                    focused = cell.key; target = tiles.get(focused); break;
                }
            }
        }
        if (target == null && !tiles.isEmpty()) {
            focused = DEVICE_SETTINGS;
            target = tiles.get(DEVICE_SETTINGS);
        }
        if (target == null) target = surface;
        if (!target.requestFocus()) {
            View deferredTarget = target;
            target.post(() -> {
                if (overlay == null && surface != null
                        && (deferredTarget == surface || deferredTarget.getParent() != null))
                    deferredTarget.requestFocus();
            });
        }
    }
    /** Resume may retain Android focus without dispatching another focus-change callback. */
    private void snapDenseAppHighlight() {
        if ((allApps && layout.appBanners) || focused == null || !focused.startsWith("app:")) return;
        View target = tiles.get(focused);
        if (target == null) return;
        target.post(() -> {
            if (!active || overlay != null || (allApps && layout.appBanners)
                    || target != tiles.get(focused) || appFocusHighlight == null) return;
            View icon = appHighlightTarget(target);
            if (icon.getWidth() <= 0 || icon.getHeight() <= 0) {
                icon.postOnAnimation(this::snapDenseAppHighlight);
                return;
            }
            moveAppFocusHighlight(icon, false);
        });
    }
    private void focusFirstSurfaceItem() {
        if (allApps) {
            List<String> items = appItems();
            if (!items.isEmpty()) { focused = items.get(Math.min(appsPage * appPageSize(), items.size() - 1)); restoreFocus(); return; }
        } else {
            for (HomeLayout.Cell cell : homeCells(dashboardItems())) {
                if (cell.page == homePage) { focused = cell.key; restoreFocus(); return; }
            }
        }
        focused = DEVICE_SETTINGS;
        restoreFocus();
    }
    private void showApps() {
        if (moving) return;
        homeFocus = focused; allApps = true; focused = appsFocus; render(1);
    }
    private void showHome() {
        if (moving) return;
        appsFocus = focused; allApps = false; focused = homeFocus; render(-1);
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        int key = event.getKeyCode();
        boolean ok = key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER || key == KeyEvent.KEYCODE_NUMPAD_ENTER;
        if (resizing) {
            if (key == KeyEvent.KEYCODE_BACK || key == KeyEvent.KEYCODE_ESCAPE) {
                if (event.getAction() == KeyEvent.ACTION_UP) finishResize(false);
                return true;
            }
            if (ok) {
                if (event.getAction() == KeyEvent.ACTION_UP) finishResize(true);
                return true;
            }
            if (key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_DOWN
                    || key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) changeResize(key);
                return true;
            }
            return true;
        }
        if (ok && event.getAction() == KeyEvent.ACTION_UP && consumeOkUp) {
            consumeOkUp = false; return true;
        }
        if (overlay != null) {
            if (key == KeyEvent.KEYCODE_BACK || key == KeyEvent.KEYCODE_ESCAPE) {
                if (event.getAction() == KeyEvent.ACTION_UP) backMenu();
                return true;
            }
            if (activeMenu != null && activeMenu.paletteGrid) return super.dispatchKeyEvent(event);
            if (ok) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    if (activeMenu == null) {
                        dismissMenu(); menuStack.clear();
                    } else menuCenter(activeMenu);
                }
                return true;
            }
            if (activeMenu != null && (key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_RIGHT
                    || key == KeyEvent.KEYCODE_DPAD_DOWN || key == KeyEvent.KEYCODE_DPAD_LEFT)) {
                int arm = key == KeyEvent.KEYCODE_DPAD_UP ? 0 : key == KeyEvent.KEYCODE_DPAD_RIGHT ? 1
                        : key == KeyEvent.KEYCODE_DPAD_DOWN ? 2 : 3;
                if (overlay.getChildCount() > 0 && overlay.getChildAt(0) instanceof PieMenuView) {
                    PieMenuView pie = (PieMenuView) overlay.getChildAt(0);
                    if (event.getAction() == KeyEvent.ACTION_DOWN) pie.setHighlighted(arm);
                    else if (!event.isCanceled()) {
                        pie.setHighlighted(-1);
                        int choice = activeMenu.page * activeMenu.pageSize + arm;
                        if (choice < activeMenu.labels.length)
                            selectMenuChoice(activeMenu, activeMenu.page, choice);
                    }
                }
                return true;
            }
            return super.dispatchKeyEvent(event);
        }
        if (ok) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                okHeld = true;
                if (!moving) handler.postDelayed(heldOk, 600);
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                boolean shortPress = okHeld && !event.isCanceled(); cancelOk();
                if (shortPress) { if (moving) commitMove(); else activate(focused); }
            }
            return true;
        }
        if (key == KeyEvent.KEYCODE_BACK || key == KeyEvent.KEYCODE_ESCAPE) {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                if (moving) cancelMove(); else if (allApps) showHome();
            }
            return true;
        }
        if (key == KeyEvent.KEYCODE_MENU) {
            if (event.getAction() == KeyEvent.ACTION_UP) homeSettings();
            return true;
        }
        if (key >= KeyEvent.KEYCODE_DPAD_UP && key <= KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                restoringSurface = false;
                navigate(key);
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
    private void cancelOk() { handler.removeCallbacks(heldOk); okHeld = false; }
    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus) cancelOk();
        else if (root != null) root.post(() -> {
            if (active && overlay == null) {
                restoreFocus();
                snapDenseAppHighlight();
            }
        });
    }
    @Override public void onBackPressed() {
        if (overlay != null) backMenu(); else if (moving) cancelMove(); else if (allApps) showHome();
    }
    private void navigate(int key) {
        if (moving) {
            if (!moveInApps) { moveHomePreview(key); return; }
            int from = preview.indexOf(movingKey);
            int delta = key == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : key == KeyEvent.KEYCODE_DPAD_RIGHT ? 1
                    : key == KeyEvent.KEYCODE_DPAD_UP ? -(moveInApps ? appColumns() : homeGridColumns())
                    : (moveInApps ? appColumns() : homeGridColumns());
            HomeLayout.move(preview, from, Math.max(0, Math.min(preview.size() - 1, from + delta)));
            focused = movingKey;
            if (moveInApps) appsPage = preview.indexOf(movingKey) / appPageSize(); else homePage = homePageOf(movingKey, preview);
            render(0); return;
        }
        if (HOME_SETTINGS.equals(focused)) {
            if (key == KeyEvent.KEYCODE_DPAD_RIGHT) { focused = DEVICE_SETTINGS; restoreFocus(); }
            else if (key == KeyEvent.KEYCODE_DPAD_DOWN) focusFirstSurfaceItem();
            return;
        }
        if (DEVICE_SETTINGS.equals(focused)) {
            if (key == KeyEvent.KEYCODE_DPAD_DOWN) focusFirstSurfaceItem();
            else if (key == KeyEvent.KEYCODE_DPAD_LEFT) { focused = HOME_SETTINGS; restoreFocus(); }
            return;
        }
        if (allApps) {
            List<String> items = appItems(); int index = items.indexOf(focused);
            if (SETTINGS.equals(focused) || index < 0) {
                if (key == KeyEvent.KEYCODE_DPAD_LEFT) showHome();
                else if (key == KeyEvent.KEYCODE_DPAD_DOWN && !items.isEmpty()) { focused = items.get(appsPage * appPageSize()); restoreFocus(); }
                return;
            }
            int columns = appColumns(), rows = appRows(), start = appsPage * appPageSize();
            int local = index - start;
            int next = index;
            if (key == KeyEvent.KEYCODE_DPAD_LEFT) { if (local % columns == 0) { showHome(); return; } next--; }
            if (key == KeyEvent.KEYCODE_DPAD_RIGHT && local % columns < columns - 1) next++;
            if (key == KeyEvent.KEYCODE_DPAD_UP) {
                if (local < columns) {
                    if (appsPage > 0) {
                        appsPage--;
                        int previousPageStart = appsPage * appPageSize();
                        int previousIndex = previousPageStart + (rows - 1) * columns + local % columns;
                        focused = items.get(Math.min(previousIndex, items.size() - 1));
                        render(0);
                    } else { focused = HOME_SETTINGS; restoreFocus(); }
                    return;
                }
                next -= columns;
            }
            if (key == KeyEvent.KEYCODE_DPAD_DOWN) next += columns;
            if (next >= 0 && next < items.size()) {
                focused = items.get(next); int page = next / appPageSize();
                if (page != appsPage) { appsPage = page; render(0); } else restoreFocus();
            }
            return;
        }
        HomeLayout.Cell current = null;
        for (HomeLayout.Cell c : visibleCells) if (c.key.equals(focused)) current = c;
        if (current == null) {
            if (key == KeyEvent.KEYCODE_DPAD_RIGHT) showApps();
            else if (key == KeyEvent.KEYCODE_DPAD_UP) { focused = HOME_SETTINGS; restoreFocus(); }
            return;
        }
        if (key == KeyEvent.KEYCODE_DPAD_UP && current.row == 0) { focused = HOME_SETTINGS; restoreFocus(); return; }
        HomeLayout.Cell best = nearestHomeNeighbor(current, key);
        if (best != null) { focused = best.key; restoreFocus(); return; }
        if (key == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (homePage + 1 < homePageCount(layout.widgets)) { homePage++; focused = ""; render(1); }
            else showApps();
        } else if (key == KeyEvent.KEYCODE_DPAD_LEFT && homePage > 0) { homePage--; focused = ""; render(-1); }
    }
    private void moveHomePreview(int key) {
        HomeLayout.Cell current = null;
        for (HomeLayout.Cell cell : homeCells(preview)) if (cell.key.equals(movingKey)) { current = cell; break; }
        if (current == null) return;
        int row = current.row, column = current.column;
        if (key == KeyEvent.KEYCODE_DPAD_LEFT) column--;
        else if (key == KeyEvent.KEYCODE_DPAD_RIGHT) column++;
        else if (key == KeyEvent.KEYCODE_DPAD_UP) row--;
        else if (key == KeyEvent.KEYCODE_DPAD_DOWN) row++;
        if (row < 0 || column < 0 || row + current.height > homeGridRows()
                || column + current.width > homeGridColumns()) return;

        LinkedHashMap<String, HomeLayout.HomePosition> requested = new LinkedHashMap<>();
        requested.put(movingKey, new HomeLayout.HomePosition(current.page, row, column));
        if (previewHomePositions != null)
            for (Map.Entry<String, HomeLayout.HomePosition> entry : previewHomePositions.entrySet())
                if (!entry.getKey().equals(movingKey)) requested.put(entry.getKey(), entry.getValue());
        List<HomeLayout.Cell> resolved = layout.cells(preview, homeGridColumns(), homeGridRows(), requested);
        previewHomePositions = new LinkedHashMap<>();
        for (HomeLayout.Cell cell : resolved) if (cell.key.equals(movingKey))
            previewHomePositions.put(cell.key, new HomeLayout.HomePosition(cell.page, cell.row, cell.column));
        for (HomeLayout.Cell cell : resolved) if (!cell.key.equals(movingKey))
            previewHomePositions.put(cell.key, new HomeLayout.HomePosition(cell.page, cell.row, cell.column));
        focused = movingKey;
        homePage = homePageOf(movingKey, preview);
        render(0);
    }

    private HomeLayout.Cell nearestHomeNeighbor(HomeLayout.Cell current, int direction) {
        HomeLayout.Cell best = null;
        int currentX = current.column * 2 + current.width;
        int currentY = current.row * 2 + current.height;
        long bestScore = Long.MAX_VALUE;
        for (HomeLayout.Cell cell : visibleCells) {
            if (cell == current) continue;
            int cellX = cell.column * 2 + cell.width;
            int cellY = cell.row * 2 + cell.height;
            int forward;
            int cross;
            if (direction == KeyEvent.KEYCODE_DPAD_RIGHT) {
                forward = cell.column - (current.column + current.width);
                cross = Math.abs(cellY - currentY);
            } else if (direction == KeyEvent.KEYCODE_DPAD_LEFT) {
                forward = current.column - (cell.column + cell.width);
                cross = Math.abs(cellY - currentY);
            } else if (direction == KeyEvent.KEYCODE_DPAD_DOWN) {
                forward = cell.row - (current.row + current.height);
                cross = Math.abs(cellX - currentX);
            } else {
                forward = current.row - (cell.row + cell.height);
                cross = Math.abs(cellX - currentX);
            }
            if (forward < 0) continue;
            long score = (long) forward * 10000L + (long) cross * cross;
            if (score < bestScore) { best = cell; bestScore = score; }
        }
        return best;
    }

    private void activate(String key) {
        if (SETTINGS.equals(key)) { homeSettings(); return; }
        if (HOME_SETTINGS.equals(key)) { homeSettings(); return; }
        if (DEVICE_SETTINGS.equals(key)) { open(new Intent(Settings.ACTION_SETTINGS)); return; }
        if (isAirWidgetKey(key)) { airFor(key).showActions(); return; }
        App app = apps.get(key);
        if (app == null) { if (!key.isEmpty()) toast("App unavailable; hold OK to remove this tile"); return; }
        open(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(app.component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
    }
    private void contextMenu(String key) {
        if (DEVICE_SETTINGS.equals(key)) { open(new Intent(Settings.ACTION_SETTINGS)); return; }
        if (HOME_SETTINGS.equals(key)) { homeSettings(); return; }
        if (key.isEmpty() || SETTINGS.equals(key)) { homeSettings(); return; }
        if (isAirWidgetKey(key)) {
            menu("Air quality widget", "Choose an action for this widget", new String[]{"Configure location and source", "Move", "Resize", "Delete widget…", "Home settings"}, choice -> {
                if (choice == 0) airFor(key).configureWidget();
                if (choice == 1) startMove(key);
                if (choice == 2) startResize(key);
                if (choice == 3) deleteTile(key);
                if (choice == 4) homeSettings();
            }); return;
        }
        App app = apps.get(key);
        menu(app == null ? "Unavailable app" : app.label, "Choose an action for this app",
                new String[]{"Move", layout.widgets.contains(key) ? "Unfavorite" : "Favorite", "Delete tile…",
                        "Uninstall app…", "App info", "Home settings"}, choice -> {
                    switch (choice) {
                        case 0: startMove(key); break;
                        case 1:
                            if (layout.widgets.contains(key)) { layout.widgets.remove(key); layout.homePositions.remove(key); }
                            else layout.favorite(key);
                            save(); render(0); toast("Favorites updated"); break;
                        case 2: deleteTile(key); break;
                        case 3: uninstall(key); break;
                        case 4:
                            ComponentName c = ComponentName.unflattenFromString(key.substring(4));
                            if (c != null) open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + c.getPackageName())));
                            break;
                        default: homeSettings(); break;
                    }
                });
    }
    private void deleteTile(String key) {
        boolean dashboard = !allApps;
        String title = dashboard ? "Delete this Home tile?" : "Delete this All Apps tile?";
        String detail = dashboard ? "The app stays installed. Restore it with Add widget or Favorite."
                : "The app stays installed. Restore its tile in Home settings.";
        menu(title, detail, new String[]{"Keep tile", "Delete tile"}, choice -> {
            if (choice == 1) { layout.delete(key, dashboard); save(); render(0); }
        });
    }
    private void uninstall(String key) {
        App app = apps.get(key);
        if (app == null) { toast("App is unavailable"); return; }
        if ((app.info.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
            menu("System app", "Android does not allow uninstalling this app here.", new String[]{"Keep app", "Open app info"}, choice -> {
                if (choice == 1) open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + app.component.getPackageName())));
            }); return;
        }
        // The system owns confirmation. Never delete tiles here; wait for PACKAGE_REMOVED.
        open(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + app.component.getPackageName())));
    }
    private void startMove(String key) {
        moveInApps = allApps; movingKey = key;
        preview = new ArrayList<>(allApps ? layout.visibleApps(new ArrayList<>(apps.keySet())) : layout.widgets);
        if (!moveInApps) {
            previewHomePositions = new LinkedHashMap<>();
            for (HomeLayout.Cell cell : homeCells(layout.widgets))
                previewHomePositions.put(cell.key, new HomeLayout.HomePosition(cell.page, cell.row, cell.column));
        }
        moving = true;
        render(0);
    }
    private void commitMove() {
        if (moveInApps) {
            List<String> previous = new ArrayList<>(layout.order);
            layout.order.clear(); layout.order.addAll(preview);
            for (String key : previous) if (!layout.order.contains(key)) layout.order.add(key);
        } else {
            layout.widgets.clear(); layout.widgets.addAll(preview);
            layout.homePositions.clear();
            if (previewHomePositions != null) layout.homePositions.putAll(previewHomePositions);
        }
        moving = false; preview = null; previewHomePositions = null; save(); render(0);
    }
    private void cancelMove() {
        moving = false; preview = null; previewHomePositions = null;
        if (allApps) { int i = appItems().indexOf(focused); appsPage = Math.max(0, i / appPageSize()); }
        else homePage = homePageOf(focused, layout.widgets);
        render(0);
    }
    private void homeSettings() {
        menu("Home settings", "Choose an area to customize", new String[]{
                "Widgets & restore", "Appearance",
                "Home behavior", "Default Home app · " + (isDefaultHomeApp() ? "Homeboard" : "Choose"),
                "About Homeboard TV"}, choice -> {
            switch (choice) {
                case 0: widgetsAndRestore(); break;
                case 1: appearanceSettings(); break;
                case 2: homeBehaviorSettings(); break;
                case 3: chooseDefaultHomeApp(); break;
                case 4: aboutHomeboard(); break;
                default: break;
            }
        });
    }

    private void widgetsAndRestore() {
        menu("Widgets & restore", "Add something to Home or restore a hidden app tile",
                new String[]{"Add widget", "Manage deleted app tiles"}, choice -> {
                    if (choice == 0) menu("Add to Home", "Choose a tile to add",
                            new String[]{"Air quality widget", "Favorite an app from All Apps"}, add -> {
                                if (add == 0) {
                                    String key = newAirWidgetKey(); layout.addPanel(key); save(); allApps = false; focused = key;
                                    homePage = homePageOf(focused, layout.widgets); render(0); }
                                else { allApps = false; showApps(); }
                            });
                    else restoreDeleted();
                });
    }

    private String newAirWidgetKey() {
        if (!layout.widgets.contains(HomeLayout.AIR)) return HomeLayout.AIR;
        int suffix = 2;
        while (layout.widgets.contains(HomeLayout.AIR + ":" + suffix)) suffix++;
        return HomeLayout.AIR + ":" + suffix;
    }

    private void appearanceSettings() {
        menu("Appearance", "Choose colors, icon shape or artwork",
                new String[]{"Background", "Circular icon mask · " + (layout.circularIcons ? "On" : "Off"), "Icons"}, choice -> {
                    if (choice == 0) colorPaletteSettings();
                    else if (choice == 1) menu("Circular icon mask", "The icon keeps the same size",
                            new String[]{"Original icon shape", "Circular mask"}, mask -> {
                                layout.circularIcons = mask == 1; save(); render(0);
                            }, layout.circularIcons ? 1 : 0);
                    else appArtworkSettings();
                });
    }

    private void colorPaletteSettings() {
        String[] ids = {"midnight", "forest", "ocean", "plum", "sand", "rose", "teal", "slate",
                "sunset", "black", "charcoal", "indigo", "lagoon", "moss", "cedar", "amber",
                "cobalt", "pine", "emerald", "copper"};
        String[] labels = {"Midnight", "Forest", "Ocean", "Plum", "Sand", "Rose", "Teal", "Slate",
                "Sunset", "Black", "Charcoal", "Indigo", "Lagoon", "Moss", "Cedar", "Amber",
                "Cobalt", "Pine", "Emerald", "Copper"};
        int selected = -1;
        for (int i = 0; i < ids.length; i++) if (ids[i].equals(layout.palette)) selected = i;
        if (selected < 0) {
            // Preserve an older selected palette while keeping the curated selector at twenty choices.
            selected = ids.length - 1;
            ids[selected] = layout.palette;
            labels[selected] = "Current palette";
        }
        if ("theme".equals(layout.palette)) selected = 0;
        paletteMenu("Background", "Move to a color to preview it", labels, choice -> {
            layout.palette = ids[choice];
            applyBackgroundChoice();
        }, selected);
    }

    private void applyBackgroundChoice() {
        background = paletteColor(); paletteColors = PaletteColors.from(background);
        setAirPalette();
        root.setBackgroundColor(background); save(); render(0);
    }

    private void appArtworkSettings() {
        menu("Icons", "Choose All Apps icon style · Home favorites stay compact",
                new String[]{"Launcher icons · full-size grid", "TV banners · 4 × 2"}, artwork -> {
                    layout.appBanners = artwork == 1; save(); render(0);
                }, layout.appBanners ? 1 : 0);
    }

    private void homeBehaviorSettings() {
        String columns = layout.homeColumns == 0 ? "Auto" : String.valueOf(layout.homeColumns);
        menu("Home behavior", "Choose grid density, new panel size or screen movement",
                new String[]{"Grid columns · " + columns,
                        "Grid rows · " + (layout.homeRows == 0 ? "Auto" : layout.homeRows),
                        "New panel size · " + panelSizeLabel(layout.defaultPanelSize),
                        "Reduced motion · " + (layout.reducedMotion ? "On" : "Off")}, choice -> {
                    if (choice == 0) homeGridColumnsSettings();
                    else if (choice == 1) homeGridRowsSettings();
                    else if (choice == 2) panelSizeMenu(null, true);
                    else menu("Reduced motion", "Choose how Home and All Apps move",
                            new String[]{"Off", "On"}, motion -> {
                                layout.reducedMotion = motion == 1; save(); render(0);
                            }, layout.reducedMotion ? 1 : 0);
                });
    }

    private String panelSizeLabel(int size) {
        return size == HomeLayout.PANEL_SMALL ? "Small" : size == HomeLayout.PANEL_LARGE ? "Large" : "Medium";
    }

    private void startResize(String key) {
        HomeLayout.Cell cell = null;
        for (HomeLayout.Cell candidate : homeCells(layout.widgets)) if (candidate.key.equals(key)) { cell = candidate; break; }
        if (cell == null) return;
        resizing = true; resizingKey = key; resizePreviousSpan = layout.panelSpan(key);
        resizeOriginalWidth = resizeWidth = cell.width;
        resizeOriginalHeight = resizeHeight = cell.height;
        render(0);
    }

    private void changeResize(int key) {
        int columns = homeGridColumns(), rows = homeGridRows();
        if (key == KeyEvent.KEYCODE_DPAD_UP) resizeHeight = Math.max(1, resizeHeight - 1);
        else if (key == KeyEvent.KEYCODE_DPAD_DOWN) resizeHeight = Math.min(rows, resizeHeight + 1);
        else if (key == KeyEvent.KEYCODE_DPAD_LEFT) resizeWidth = Math.max(1, resizeWidth - 1);
        else if (key == KeyEvent.KEYCODE_DPAD_RIGHT) resizeWidth = Math.min(columns, resizeWidth + 1);
        layout.setPanelSpan(resizingKey, resizeWidth, resizeHeight);
        homePage = homePageOf(resizingKey, layout.widgets);
        render(0);
        // The dashboard footer itself is the resize help panel; render updates its live dimensions.
    }

    private String resizeHelpText() {
        return "Resize " + resizeWidth + "×" + resizeHeight
                + "   ← Narrower   ↑ Shorter   → Wider   ↓ Taller   ·   OK Save   ·   Back Cancel";
    }

    private void finishResize(boolean commit) {
        if (!resizing) return;
        if (commit) save();
        else if (resizePreviousSpan == null) layout.panelSpans.remove(resizingKey);
        else layout.setPanelSpan(resizingKey, resizePreviousSpan.width, resizePreviousSpan.height);
        resizing = false; resizingKey = null; resizePreviousSpan = null;
        if (overlay != null) root.removeView(overlay);
        overlay = null; activeMenu = null; menuStack.clear();
        surface.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        surface.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
        homePage = homePageOf(focused, layout.widgets);
        render(0);
    }

    private void panelSizeMenu(String key, boolean defaults) {
        int selected = defaults ? layout.defaultPanelSize : layout.panelSize(key);
        String[] labels = {"Small · about one third width", "Medium · about half width", "Large · about three quarters width"};
        labels[selected - 1] += " · Current";
        menu(defaults ? "New panel size" : "Panel size",
                defaults ? "Default size for panels added later; existing panels keep their size"
                        : "Resize this panel; height grows with size and the grid repacks around it",
                labels, choice -> {
                    int size = choice + 1;
                    if (defaults) layout.defaultPanelSize = size;
                    else layout.setPanelSize(key, size);
                    save();
                    if (!defaults) homePage = homePageOf(key, layout.widgets);
                    render(0);
                    if (defaults) homeBehaviorSettings();
                }, selected - 1);
    }

    private void homeGridColumnsSettings() {
        int[] values = {0, 4, 6, 8, 10, 12};
        String[] labels = {"Auto", "4 columns", "6 columns", "8 columns", "10 columns", "12 columns"};
        int selected = 0;
        for (int i = 0; i < values.length; i++) if (values[i] == layout.homeColumns) selected = i;
        String currentLabel = selected == 0 ? "Auto" : labels[selected];
        labels[selected] = currentLabel + " · Current";
        int fit = homeGridFitColumns();
        menu("Grid columns", "This TV fits up to " + fit + " columns",
                labels, choice -> {
                    layout.homeColumns = values[choice];
                    homePage = homePageOf(focused, layout.widgets); save(); render(0);
                }, selected);
    }

    private void homeGridRowsSettings() {
        int fit = homeGridFitRows();
        int[] values = new int[fit];
        String[] labels = new String[fit];
        values[0] = 0;
        labels[0] = "Auto";
        for (int rows = 2; rows <= fit; rows++) {
            values[rows - 1] = rows;
            labels[rows - 1] = rows + " rows";
        }
        int selected = 0;
        for (int i = 0; i < values.length; i++) if (values[i] == layout.homeRows) selected = i;
        labels[selected] += " · Current";
        menu("Grid rows", "Auto or any count up to this TV's fit (" + fit + ")",
                labels, choice -> {
                    layout.homeRows = values[choice];
                    homePage = homePageOf(focused, layout.widgets); save(); render(0);
                }, selected);
    }

    private void aboutHomeboard() {
        showAboutDialog();
    }
    private void showAboutDialog() {
        if (invokingMenuAction && activeMenu != null) menuStack.add(activeMenu);
        dismissMenu(); cancelOk();
        surface.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        surface.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        activeMenu = null; menuGeneration++;
        overlay = new FrameLayout(this);
        overlay.setBackgroundColor(paletteColors.scrim);
        overlay.setFocusable(true); overlay.setFocusableInTouchMode(true);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(30), dp(24), dp(30), dp(20));
        panel.setBackground(shape(paletteColors.raised, paletteColors.edge, 2));
        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.HORIZONTAL); heading.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icon = new ImageView(this);
        try { icon.setImageDrawable(getPackageManager().getApplicationIcon(getPackageName())); }
        catch (PackageManager.NameNotFoundException ignored) { }
        heading.addView(icon, new LinearLayout.LayoutParams(dp(76), dp(76)));
        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleBlockLp = new LinearLayout.LayoutParams(0, -2, 1f);
        titleBlockLp.leftMargin = dp(22); heading.addView(titleBlock, titleBlockLp);
        TextView title = text("About Homeboard TV", 27); title.setTypeface(null, Typeface.BOLD);
        TextView version = text("Version " + appVersion(), 17); version.setTextColor(paletteColors.muted);
        titleBlock.addView(title); titleBlock.addView(version);
        panel.addView(heading, new LinearLayout.LayoutParams(-1, dp(90)));

        TextView purpose = text("A customizable TV home screen for widgets and favorite apps.", 18);
        purpose.setPadding(0, dp(18), 0, dp(18));
        panel.addView(purpose);
        View divider = new View(this); divider.setBackgroundColor(paletteColors.edge);
        panel.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
        TextView base = text("Built on CleanYourTV", 18); base.setTypeface(null, Typeface.BOLD);
        base.setPadding(0, dp(16), 0, dp(4)); panel.addView(base);
        TextView attribution = text("CleanYourTV by Gathin and contributors · MIT License", 15);
        attribution.setTextColor(paletteColors.muted); panel.addView(attribution);
        TextView notices = text("Outfit font · SIL Open Font License 1.1\nAndroidX components · Apache License 2.0", 14);
        notices.setTextColor(paletteColors.muted); notices.setPadding(0, dp(12), 0, dp(14)); panel.addView(notices);

        LinearLayout closeHint = new LinearLayout(this);
        closeHint.setOrientation(LinearLayout.HORIZONTAL); closeHint.setGravity(Gravity.CENTER_VERTICAL);
        closeHint.addView(new MenuIconView(this, MenuIconView.Kind.CANCEL, paletteColors),
                new LinearLayout.LayoutParams(dp(18), dp(18)));
        TextView closeText = text("Back to Home settings", 14); closeText.setTextColor(paletteColors.muted);
        LinearLayout.LayoutParams closeTextLp = new LinearLayout.LayoutParams(-2, dp(28));
        closeTextLp.leftMargin = dp(6); closeHint.addView(closeText, closeTextLp);
        closeHint.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        panel.addView(closeHint, new LinearLayout.LayoutParams(-1, dp(30)));

        int width = Math.min(dp(760), getResources().getDisplayMetrics().widthPixels - dp(96));
        overlay.addView(panel, new FrameLayout.LayoutParams(width, -2, Gravity.CENTER));
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
        overlay.requestFocus();
    }
    private boolean isDefaultHomeApp() {
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            RoleManager roles = getSystemService(RoleManager.class);
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME))
                return roles.isRoleHeld(RoleManager.ROLE_HOME);
        }
        ResolveInfo home = getPackageManager().resolveActivity(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
                PackageManager.MATCH_DEFAULT_ONLY);
        return home != null && home.activityInfo != null
                && getPackageName().equals(home.activityInfo.packageName);
    }
    private void chooseDefaultHomeApp() {
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            RoleManager roles = getSystemService(RoleManager.class);
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME)) {
                try {
                    startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_HOME), REQUEST_HOME_ROLE);
                    return;
                } catch (RuntimeException unavailable) {
                    // Some TV builds expose the role but only implement the system Home settings screen.
                }
            }
        }
        Intent chooser = new Intent(Settings.ACTION_HOME_SETTINGS);
        if (getPackageManager().resolveActivity(chooser, PackageManager.MATCH_DEFAULT_ONLY) != null) {
            try { startActivityForResult(chooser, REQUEST_HOME_ROLE); }
            catch (RuntimeException unavailable) { toast("Android could not open Home app selection"); }
        }
        else toast("Android does not provide Home app selection on this device");
    }
    private String appVersion() {
        try {
            String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            return version == null ? "unknown" : version;
        } catch (PackageManager.NameNotFoundException ignored) { return "unknown"; }
    }
    private void restoreDeleted() {
        List<String> keys = new ArrayList<>(layout.hidden); Collections.sort(keys);
        if (keys.isEmpty()) { menu("No deleted app tiles", "Apps stay installed when their Homeboard tile is deleted.", new String[]{"Close"}, i -> {}); return; }
        String[] labels = new String[keys.size()];
        for (int i = 0; i < keys.size(); i++) labels[i] = apps.containsKey(keys.get(i)) ? apps.get(keys.get(i)).label : keys.get(i).substring(4);
        menu("Restore app tile", "Choose an app to show in All Apps again", labels, choice -> {
            layout.hidden.remove(keys.get(choice)); save(); render(0);
        });
    }

    private void menu(String title, String[] labels, java.util.function.IntConsumer action) {
        menu(title, "", labels, action, -1);
    }
    private void menu(String title, String subtitle, String[] labels, java.util.function.IntConsumer action) {
        menu(title, subtitle, labels, action, -1);
    }
    private void menu(String title, String subtitle, String[] labels, java.util.function.IntConsumer action,
                      int selectedIndex) {
        openMenu(new MenuSpec(title, subtitle, labels, action, selectedIndex));
    }
    private void paletteMenu(String title, String subtitle, String[] labels,
                             java.util.function.IntConsumer action, int selectedIndex) {
        openMenu(new MenuSpec(title, subtitle, labels, action, selectedIndex, labels.length, 4, true, true));
    }
    private void openMenu(MenuSpec spec) {
        if (invokingMenuAction && activeMenu != null) menuStack.add(activeMenu);
        else menuStack.clear();
        dismissMenu(); cancelOk();
        surface.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        surface.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        activeMenu = spec;
        menuGeneration++;
        int initialChoice = spec.selectedIndex >= 0 ? spec.selectedIndex : 0;
        showMenuPage(activeMenu, initialChoice / spec.pageSize, initialChoice);
    }
    /** Draw one fixed page inside Home's window; no menu content is placed in a scroll container. */
    private void showMenuPage(MenuSpec spec, int page, int focusChoice) {
        String title = spec.title, subtitle = spec.subtitle;
        String[] labels = spec.labels;
        int selectedIndex = spec.selectedIndex, pageCount = spec.pageCount;
        spec.page = page;
        if (!spec.paletteGrid) {
            showPieMenuPage(spec, page, focusChoice);
            return;
        }
        surface.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        surface.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        if (overlay != null) root.removeView(overlay);
        overlay = new FrameLayout(this);
        overlay.setBackgroundColor(paletteColors.scrim);
        overlay.setFocusable(true); overlay.setClickable(true);

        int firstChoice = page * spec.pageSize;
        int lastChoice = Math.min(labels.length, firstChoice + spec.pageSize);
        int count = lastChoice - firstChoice;
        int columns = spec.gridColumns;
        int rows = Math.max(1, (count + columns - 1) / columns);
        float density = getResources().getDisplayMetrics().density;
        int screenWidthDp = Math.round(getResources().getDisplayMetrics().widthPixels / density);
        int screenHeightDp = Math.round(getResources().getDisplayMetrics().heightPixels / density);
        int panelWidth = Math.max(dp(300), Math.min(dp(850), dp(screenWidthDp - 64)));
        int padding = dp(spec.compactCards ? 10 : 20);
        int cardHeight = dp(spec.paletteGrid ? 82 : spec.compactCards ? 56 : 104);
        int footerHeight = dp(spec.compactCards ? 32 : 46);
        int pagerHeight = pageCount > 1 ? dp(54) : 0;
        int titleHeight = dp(spec.compactCards ? 36 : 48);
        int subtitleLines = subtitle == null || subtitle.isEmpty() ? 0 : Math.max(1, Math.min(3, subtitle.split("\\n", -1).length));
        int subtitleHeight = subtitleLines == 0 ? 0 : dp(21 * subtitleLines + 5);
        int desiredHeight = padding * 2 + titleHeight + subtitleHeight + rows * cardHeight
                + pagerHeight + footerHeight + dp(10);
        int maximumHeight = Math.max(dp(340), dp(screenHeightDp - 48));
        if (desiredHeight > maximumHeight) {
            cardHeight = Math.max(dp(spec.paletteGrid ? 70 : spec.compactCards ? 56 : 78), cardHeight - (desiredHeight - maximumHeight) / rows);
            desiredHeight = padding * 2 + titleHeight + subtitleHeight + rows * cardHeight
                    + pagerHeight + footerHeight + dp(10);
        }
        int panelHeight = Math.min(desiredHeight, maximumHeight);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(padding, dp(16), padding, dp(14));
        panel.setBackground(shape(paletteColors.raised, paletteColors.edge, 1));
        TextView heading = text(title, 23);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setMaxLines(2);
        heading.setEllipsize(android.text.TextUtils.TruncateAt.END);
        panel.addView(heading, new LinearLayout.LayoutParams(-1, titleHeight));
        if (subtitleLines > 0) {
            TextView detail = text(subtitle, 15);
            detail.setTextColor(paletteColors.muted); detail.setMaxLines(subtitleLines);
            detail.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams detailLp = new LinearLayout.LayoutParams(-1, subtitleHeight);
            detailLp.bottomMargin = dp(5); panel.addView(detail, detailLp);
        }

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        View firstFocus = null;
        int preferredFocus = focusChoice >= firstChoice && focusChoice < lastChoice ? focusChoice
                : selectedIndex >= firstChoice && selectedIndex < lastChoice ? selectedIndex : firstChoice;
        int localIndex = 0;
        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int column = 0; column < columns; column++) {
                int choice = firstChoice + localIndex;
                if (choice < lastChoice) {
                    String label = labels[choice];
                    boolean isSelected = choice == selectedIndex;
                    LinearLayout card = new LinearLayout(this);
                    card.setOrientation(LinearLayout.VERTICAL);
                    card.setGravity(Gravity.CENTER);
                    card.setPadding(dp(spec.compactCards ? 3 : 6), dp(spec.compactCards ? 2 : 5),
                            dp(spec.compactCards ? 3 : 6), dp(spec.compactCards ? 2 : 5));
                    card.setFocusable(true); card.setClickable(true);
                    card.setContentDescription(label + (isSelected ? ", selected" : ""));
                    String paletteId = paletteIdForLabel(label);
                    int swatch = paletteId == null ? paletteColor() : paletteColorForId(paletteId);
                    if (spec.paletteGrid) {
                        if (swatch == 0) swatch = paletteColor();
                        int edge = isSelected ? paletteColors.text : paletteColors.edge;
                        card.setBackground(shape(swatch, edge, isSelected ? 3 : 1));
                    } else card.setBackground(tileBackground());
                    Drawable appIcon = spec.paletteGrid ? null : appIconForLabel(label);
                    if (appIcon != null) {
                        ImageView icon = new ImageView(this);
                        icon.setImageDrawable(appIcon); icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                        card.addView(icon, new LinearLayout.LayoutParams(dp(38), dp(38)));
                    } else if (!spec.paletteGrid) {
                        MenuIconView.Kind iconKind = menuIconFor(title, label);
                        MenuIconView icon = new MenuIconView(this, iconKind, paletteColors);
                        if (iconKind == MenuIconView.Kind.PALETTE) {
                            if (paletteId != null) icon.setSwatchColor(paletteColorForId(paletteId));
                            else if ("current palette".equalsIgnoreCase(label)
                                    || label.toLowerCase(java.util.Locale.ROOT).contains("choose color palette"))
                                icon.setSwatchColor(paletteColor());
                        }
                        icon.setSelectedChoice(isSelected);
                        int iconSize = spec.compactCards ? 18 : 38;
                        card.addView(icon, new LinearLayout.LayoutParams(dp(iconSize), dp(iconSize)));
                    }
                    String cardLabel = spec.paletteGrid && isSelected ? label + " · Current" : label;
                    TextView caption = text(cardLabel, spec.paletteGrid ? 18 : spec.compactCards ? 12 : 16);
                    if (spec.paletteGrid) caption.setTextColor(contrastingText(swatch));
                    caption.setGravity(Gravity.CENTER); caption.setMaxLines(spec.compactCards ? 1 : 2);
                    caption.setEllipsize(android.text.TextUtils.TruncateAt.END);
                    LinearLayout.LayoutParams captionLp = new LinearLayout.LayoutParams(-1, -2);
                    captionLp.topMargin = dp(spec.compactCards ? 1 : 3); card.addView(caption, captionLp);
                    card.setOnFocusChangeListener((v, hasFocus) -> {
                        if (!hasFocus) return;
                        spec.focusedChoice = choice;
                        if (spec.paletteGrid && spec.selectedIndex != choice) {
                            spec.selectedIndex = choice;
                            spec.action.accept(choice);
                            if (activeMenu == spec) showMenuPage(spec, page, choice);
                        }
                    });
                    card.setOnClickListener(v -> {
                        if (spec.paletteGrid) {
                            backMenu();
                            return;
                        }
                        selectMenuChoice(spec, page, choice);
                    });
                    LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(0, cardHeight, 1f);
                    cardLp.setMargins(dp(4), dp(4), dp(4), dp(4));
                    row.addView(card, cardLp);
                    if (firstFocus == null || choice == preferredFocus) firstFocus = card;
                    localIndex++;
                } else {
                    View spacer = new View(this);
                    row.addView(spacer, new LinearLayout.LayoutParams(0, cardHeight, 1f));
                }
            }
            grid.addView(row, new LinearLayout.LayoutParams(-1, cardHeight));
        }
        panel.addView(grid, new LinearLayout.LayoutParams(-1, rows * cardHeight));

        if (pageCount > 1) {
            LinearLayout pager = new LinearLayout(this);
            pager.setOrientation(LinearLayout.HORIZONTAL);
            pager.setGravity(Gravity.CENTER_VERTICAL);
            View previous = menuPagerControl("Previous", MenuIconView.Kind.PREVIOUS, page > 0);
            previous.setOnClickListener(v -> {
                if (page > 0) showMenuPage(spec, page - 1,
                        Math.max((page - 1) * spec.pageSize, spec.focusedChoice - spec.pageSize));
            });
            TextView countLabel = text("Page " + (page + 1) + " of " + pageCount, 15);
            countLabel.setGravity(Gravity.CENTER);
            View next = menuPagerControl("Next", MenuIconView.Kind.NEXT, page + 1 < pageCount);
            next.setOnClickListener(v -> {
                if (page + 1 < pageCount) showMenuPage(spec, page + 1,
                        Math.min((page + 1) * spec.pageSize, labels.length - 1));
            });
            pager.addView(previous, new LinearLayout.LayoutParams(0, dp(42), 1f));
            pager.addView(countLabel, new LinearLayout.LayoutParams(0, dp(42), 1f));
            pager.addView(next, new LinearLayout.LayoutParams(0, dp(42), 1f));
            panel.addView(pager, new LinearLayout.LayoutParams(-1, pagerHeight));
        }

        LinearLayout hints = new LinearLayout(this);
        hints.setOrientation(LinearLayout.HORIZONTAL); hints.setGravity(Gravity.CENTER_VERTICAL);
        if (spec.paletteGrid) {
            TextView hint = text("Move to preview a color · Back returns", 14);
            hint.setTextColor(paletteColors.muted); hints.addView(hint);
        } else {
            hints.addView(new MenuIconView(this, MenuIconView.Kind.CONFIRM, paletteColors), new LinearLayout.LayoutParams(dp(20), dp(20)));
            TextView selectHint = text("OK More / Back", 13); selectHint.setTextColor(paletteColors.muted);
            LinearLayout.LayoutParams selectLp = new LinearLayout.LayoutParams(-2, dp(26)); selectLp.leftMargin = dp(5);
            hints.addView(selectHint, selectLp);
            hints.addView(new MenuIconView(this, MenuIconView.Kind.CANCEL, paletteColors), new LinearLayout.LayoutParams(dp(20), dp(20)));
            TextView backHint = text("Back Parent", 13); backHint.setTextColor(paletteColors.muted);
            LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(-2, dp(26)); backLp.leftMargin = dp(5);
            hints.addView(backHint, backLp);
        }
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(-1, footerHeight);
        hintLp.topMargin = dp(3); panel.addView(hints, hintLp);

        FrameLayout.LayoutParams panelLp = new FrameLayout.LayoutParams(panelWidth, panelHeight, Gravity.CENTER);
        overlay.addView(panel, panelLp);
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
        if (firstFocus != null) firstFocus.requestFocus();
    }

    private void showPieMenuPage(MenuSpec spec, int page, int focusChoice) {
        spec.page = page;
        surface.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        surface.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        if (overlay != null) root.removeView(overlay);
        overlay = new FrameLayout(this);
        overlay.setBackgroundColor(paletteColors.scrim);
        overlay.setFocusable(true); overlay.setClickable(true);
        int first = page * spec.pageSize;
        int last = Math.min(spec.labels.length, first + spec.pageSize);
        String[] labels = new String[Math.max(0, last - first)];
        MenuIconView.Kind[] kinds = new MenuIconView.Kind[labels.length];
        boolean[] selected = new boolean[labels.length];
        for (int i = 0; i < labels.length; i++) {
            int choice = first + i;
            labels[i] = spec.labels[choice];
            kinds[i] = menuIconFor(spec.title, labels[i]);
            selected[i] = choice == spec.selectedIndex || labels[i].toLowerCase(java.util.Locale.ROOT).contains("current");
        }
        String centerAction = spec.pageCount > 1
                ? page + 1 < spec.pageCount ? "More" : "First page"
                : menuStack.isEmpty() ? "Close" : "Back";
        String pageHint = spec.pageCount > 1 ? "Page " + (page + 1) + " of " + spec.pageCount : "";
        String breadcrumb = menuStack.isEmpty() ? spec.subtitle
                : menuStack.get(menuStack.size() - 1).title + " › " + spec.title;
        String interactionHint = spec.pageCount > 1
                ? "Page " + (page + 1) + " of " + spec.pageCount + " · OK changes page"
                : "Directions choose · Back returns";
        PieMenuView pie = new PieMenuView(this, paletteColors);
        pie.setContent(spec.title, breadcrumb, centerAction, interactionHint, labels, kinds, selected,
                new PieMenuView.Listener() {
                    @Override public void onArm(int arm) {
                        int choice = spec.page * spec.pageSize + arm;
                        if (choice < spec.labels.length) selectMenuChoice(spec, spec.page, choice);
                    }
                    @Override public void onCenter() { menuCenter(spec); }
                });
        int width = Math.round(getResources().getDisplayMetrics().widthPixels - dp(56));
        int height = Math.round(getResources().getDisplayMetrics().heightPixels - dp(40));
        int diameter = Math.max(dp(360), Math.min(width, height));
        diameter = Math.min(diameter, dp(760));
        overlay.addView(pie, new FrameLayout.LayoutParams(diameter, diameter, Gravity.CENTER));
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
        pie.requestFocus();
    }

    private void selectMenuChoice(MenuSpec spec, int page, int choice) {
        if (activeMenu != spec || choice < 0 || choice >= spec.labels.length) return;
        spec.focusedChoice = choice; spec.page = page;
        int previousGeneration = menuGeneration;
        dismissMenu();
        invokingMenuAction = true;
        try { spec.action.accept(choice); }
        finally { invokingMenuAction = false; }
        if (menuGeneration == previousGeneration) {
            activeMenu = null; menuStack.clear();
        }
    }

    private void menuCenter(MenuSpec spec) {
        if (activeMenu != spec) return;
        if (spec.pageCount > 1) {
            int nextPage = (spec.page + 1) % spec.pageCount;
            int nextFocus = Math.min(nextPage * spec.pageSize, spec.labels.length - 1);
            showMenuPage(spec, nextPage, nextFocus);
        } else if (!menuStack.isEmpty()) {
            backMenu();
        } else {
            dismissMenu(); activeMenu = null; menuStack.clear();
        }
    }
    private View menuPagerControl(String label, MenuIconView.Kind icon, boolean enabled) {
        LinearLayout control = new LinearLayout(this);
        control.setOrientation(LinearLayout.HORIZONTAL); control.setGravity(Gravity.CENTER);
        control.setFocusable(true); control.setClickable(true); control.setEnabled(enabled);
        control.setBackground(tileBackground()); control.setAlpha(enabled ? 1f : 0.38f);
        control.setContentDescription(label + (enabled ? "" : ", unavailable"));
        control.addView(new MenuIconView(this, icon, paletteColors), new LinearLayout.LayoutParams(dp(21), dp(21)));
        TextView caption = text(label, 15);
        LinearLayout.LayoutParams captionLp = new LinearLayout.LayoutParams(-2, -2);
        captionLp.leftMargin = dp(5); control.addView(caption, captionLp);
        return control;
    }
    private Drawable appIconForLabel(String label) {
        for (App app : apps.values()) {
            if (app.label.equals(label)) {
                try { return app.info.loadIcon(getPackageManager()); }
                catch (RuntimeException ignored) { return null; }
            }
        }
        return null;
    }
    private MenuIconView.Kind menuIconFor(String title, String label) {
        String value = label.toLowerCase(java.util.Locale.ROOT);
        String paletteLabel = value.replace(" · current", "");
        String context = title.toLowerCase(java.util.Locale.ROOT);
        if (value.contains("favorite")) return MenuIconView.Kind.FAVORITE;
        if (value.contains("close") || value.contains("cancel") || value.contains("keep tile") || value.contains("keep app")) return MenuIconView.Kind.CANCEL;
        if (value.contains("open-source") || value.contains("acknowledgement") || value.contains("license")) return MenuIconView.Kind.LICENSE;
        if (value.contains("about")) return MenuIconView.Kind.INFO;
        if (value.contains("uninstall")) return MenuIconView.Kind.UNINSTALL;
        if (value.contains("delete") || value.contains("remove")) return MenuIconView.Kind.DELETE;
        if (value.contains("move")) return MenuIconView.Kind.MOVE;
        if (value.contains("resize") || value.contains("width") || value.contains("wide")) return MenuIconView.Kind.RESIZE;
        if (value.contains("air quality") || value.contains("refresh") || value.contains("station")) return MenuIconView.Kind.AIR;
        if (value.contains("add widget") || value.contains("add to home")) return MenuIconView.Kind.ADD;
        if (value.contains("widgets")) return MenuIconView.Kind.WIDGET;
        if (value.contains("grid")) return MenuIconView.Kind.GRID;
        if (value.contains("restore") || value.contains("deleted app")) return MenuIconView.Kind.RESTORE;
        if (value.contains("wallpaper") || value.contains("photo")) return MenuIconView.Kind.WALLPAPER;
        if (paletteIdForLabel(paletteLabel) != null || value.contains("palette")) return MenuIconView.Kind.PALETTE;
        if (value.contains("appearance")) return MenuIconView.Kind.APPEARANCE;
        if (value.contains("artwork") || value.contains("launcher icon") || value.contains("icon shape") || value.contains("original icon")) return MenuIconView.Kind.ARTWORK;
        if (value.contains("circular") || value.contains("circle mask")) return MenuIconView.Kind.CIRCLE;
        if (value.contains("behavior") || value.contains("reduced motion")) return MenuIconView.Kind.BEHAVIOR;
        if (value.contains("default home") || value.contains("homeboard")) return MenuIconView.Kind.HOME;
        if (value.contains("theme")) return MenuIconView.Kind.THEME;
        if (value.contains("midnight")) return MenuIconView.Kind.MOON;
        if (value.contains("forest")) return MenuIconView.Kind.FOREST;
        if (value.contains("black") || value.contains("circle")) return MenuIconView.Kind.CIRCLE;
        if (value.contains("banner")) return MenuIconView.Kind.BANNER;
        if (value.contains("app info") || value.contains("version")) return MenuIconView.Kind.INFO;
        if (value.contains("delete tile")) return MenuIconView.Kind.DELETE;
        if (value.contains("app")) return MenuIconView.Kind.APP;
        if (value.contains("home settings")) return MenuIconView.Kind.SETTINGS;
        if (value.contains("location") || context.startsWith("monitoring station")) return MenuIconView.Kind.LOCATION;
        if (value.contains("settings")) return MenuIconView.Kind.SETTINGS;
        if (value.contains("app")) return MenuIconView.Kind.APP;
        if (context.contains("air quality") || context.contains("station")) return MenuIconView.Kind.LOCATION;
        return MenuIconView.Kind.APP;
    }
    private String paletteIdForLabel(String label) {
        String normalized = label.toLowerCase(java.util.Locale.ROOT).trim().replace(" · current", "");
        switch (normalized) {
            case "midnight": return "midnight";
            case "forest": return "forest";
            case "ocean": return "ocean";
            case "plum": return "plum";
            case "sand": return "sand";
            case "rose": return "rose";
            case "teal": return "teal";
            case "slate": return "slate";
            case "sunset": return "sunset";
            case "black": return "black";
            case "charcoal": return "charcoal";
            case "indigo": return "indigo";
            case "cobalt": return "cobalt";
            case "lagoon": return "lagoon";
            case "pine": return "pine";
            case "moss": return "moss";
            case "emerald": return "emerald";
            case "olive": return "olive";
            case "cedar": return "cedar";
            case "copper": return "copper";
            case "amber": return "amber";
            case "plum wine": return "plumwine";
            case "mulberry": return "mulberry";
            case "berry": return "berry";
            case "graphite": return "graphite";
            case "smoke": return "smoke";
            case "lagune blue": return "laguneblue";
            case "arctic": return "arctic";
            case "deep teal": return "deepteal";
            case "earth": return "earth";
            default: return null;
        }
    }
    private void dismissMenu() {
        if (overlay == null) return;
        root.removeView(overlay); overlay = null;
        surface.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        surface.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
        restoreFocus();
    }
    private void backMenu() {
        if (overlay == null) return;
        if (activeMenu != null && activeMenu.page > 0) {
            int page = activeMenu.page - 1;
            int focus = Math.min(page * activeMenu.pageSize, activeMenu.labels.length - 1);
            showMenuPage(activeMenu, page, focus);
            return;
        }
        if (!menuStack.isEmpty()) {
            dismissMenu();
            activeMenu = menuStack.remove(menuStack.size() - 1);
            showMenuPage(activeMenu, activeMenu.page, activeMenu.focusedChoice);
        } else {
            dismissMenu(); activeMenu = null;
        }
    }
    private void open(Intent intent) {
        try { startActivity(intent); }
        catch (RuntimeException e) { toast("Android could not open that action"); }
    }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }
}
