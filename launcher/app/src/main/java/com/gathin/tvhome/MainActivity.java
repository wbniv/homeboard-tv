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
import org.json.JSONArray;
import org.json.JSONObject;
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
    private static final int MAX_UNDO_ENTRIES = 20;
    private static final String PREF_UNDO_HISTORY = "undo_history";
    private static final class MenuSpec {
        final String title, subtitle;
        final String[] labels;
        final java.util.function.IntConsumer action;
        int selectedIndex;
        final int pageCount;
        final int pageSize, gridColumns;
        final boolean compactCards, paletteGrid;
        final Runnable panelRenderer;
        int page, focusedChoice;
        MenuSpec(String title, String subtitle, String[] labels,
                 java.util.function.IntConsumer action, int selectedIndex) {
            this(title, subtitle, labels, action, selectedIndex, MENU_PAGE_SIZE, 4, false, false);
        }
        MenuSpec(String title, String subtitle, String[] labels,
                 java.util.function.IntConsumer action, int selectedIndex,
                 int pageSize, int gridColumns, boolean compactCards, boolean paletteGrid) {
            this(title, subtitle, labels, action, selectedIndex, pageSize, gridColumns, compactCards, paletteGrid, null);
        }
        MenuSpec(String title, Runnable panelRenderer) {
            this(title, "", new String[0], ignored -> {}, -1, MENU_PAGE_SIZE, 4, false, false, panelRenderer);
        }
        private MenuSpec(String title, String subtitle, String[] labels,
                 java.util.function.IntConsumer action, int selectedIndex,
                 int pageSize, int gridColumns, boolean compactCards, boolean paletteGrid, Runnable panelRenderer) {
            this.title = title; this.subtitle = subtitle; this.labels = labels;
            this.action = action; this.selectedIndex = selectedIndex;
            this.panelRenderer = panelRenderer;
            this.pageSize = pageSize; this.gridColumns = gridColumns; this.compactCards = compactCards;
            this.paletteGrid = paletteGrid;
            pageCount = Math.max(1, (labels.length + pageSize - 1) / pageSize);
        }
    }
    private static final class GridSelector {
        final boolean columns;
        final int[] values;
        final String[] labels;
        int index;
        GridSelector(boolean columns, int[] values, String[] labels, int index) {
            this.columns = columns; this.values = values; this.labels = labels; this.index = index;
        }
    }
    private static final class UndoEntry {
        final String label, kind, key, state;
        UndoEntry(String label, String kind, String key, String state) {
            this.label = label; this.kind = kind; this.key = key; this.state = state;
        }
    }
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService loader = Executors.newSingleThreadExecutor();
    private final Map<String, App> apps = new LinkedHashMap<>();
    private final Map<String, View> tiles = new LinkedHashMap<>();
    private final List<HomeLayout.Cell> visibleCells = new ArrayList<>();
    private FrameLayout root, surface, overlay;
    private View uiBackdrop;
    private int uiBackgroundOpacity = 75;
    private WallpaperSurface wallpaperSurface;
    private ProjectivyWallpaperHost wallpaperHost;
    private WallpaperSettings wallpaperDraft;
    private ProjectivyProviderRegistry.Provider wallpaperProvider;
    private tv.projectivy.plugin.wallpaperprovider.api.Wallpaper displayedWallpaper;
    private boolean wallpaperReady, wallpaperPreviewStarted, returningFromProviderSettings;
    private boolean backgroundInfo;
    private String wallpaperStatus = "Choose Preview to load this provider";
    private java.util.function.IntConsumer adjustmentArm;
    private Runnable adjustmentDone, adjustmentCancel;
    private int adjustmentValue;
    private String adjustmentTitle;
    private java.util.function.IntConsumer adjustmentPreview, adjustmentCommit;
    private View appFocusHighlight;
    private SpringAnimation appFocusX, appFocusY;
    private boolean appFocusHighlightShown;
    private boolean restoringSurface = true;
    private final List<MenuSpec> menuStack = new ArrayList<>();
    private MenuSpec activeMenu;
    private boolean invokingMenuAction;
    private int menuGeneration;
    private TextView footer;
    private FrameLayout undoBanner;
    private TextView undoButton;
    private TextView aboutBackButton;
    private TextView backgroundStatusText;
    private SharedPreferences preferences;
    private HomeLayout layout;
    private String lastSavedLayout;
    private String undoGroupStart;
    private String undoGroupLabel;
    private boolean applyingUndo;
    private GridSelector gridSelector;
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
                saveWithoutUndo();
            }
            loadApps();
            if (wallpaperHost != null) wallpaperHost.packagesChanged();
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
            // Move former theme defaults to a deterministic palette; retain any saved image data.
            layout.palette = "midnight";
            saveWithoutUndo();
        }
        if (encoded == null) saveWithoutUndo();
        lastSavedLayout = layout.encode();
        Config config = Config.load(this);
        configReducedMotion = config.reducedMotion;
        background = paletteColor();
        paletteColors = PaletteColors.from(background);
        root = new FrameLayout(this);
        root.setBackgroundColor(background);
        wallpaperSurface = new WallpaperSurface(this);
        root.addView(wallpaperSurface, new FrameLayout.LayoutParams(-1, -1));
        uiBackdrop = new View(this);
        uiBackdrop.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        root.addView(uiBackdrop, new FrameLayout.LayoutParams(-1, -1));
        wallpaperHost = new ProjectivyWallpaperHost(this, new ProjectivyWallpaperHost.Listener() {
            @Override public void wallpaper(tv.projectivy.plugin.wallpaperprovider.api.Wallpaper item) {
                wallpaperSurface.show(item, (ready, status) -> {
                    wallpaperReady = ready && (wallpaperDraft == null || wallpaperPreviewStarted);
                    if (ready) displayedWallpaper = item;
                    wallpaperStatus = status;
                    refreshProviderDetails();
                    if (!ready) wallpaperHost.mediaFailed(status);
                });
            }
            @Override public void status(String status) { wallpaperStatus = status; refreshProviderDetails(); }
            @Override public void fallback() { wallpaperReady = false; displayedWallpaper = null; wallpaperSurface.solid(); }
        });
        syncBackground();
        setContentView(root);
        root.getViewTreeObserver().addOnGlobalFocusChangeListener((previous, next) -> {
            // Android can dispatch this before the tile's own focus listener updates `focused`.
            boolean appTarget = false;
            for (Map.Entry<String, View> entry : tiles.entrySet()) {
                if (entry.getValue() == next && entry.getKey().startsWith("app:")) {
                    appTarget = true; break;
                }
            }
            if (!active || overlay != null || !appTarget)
                hideAppFocusHighlight();
        });
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
        wallpaperSurface.visible(true); wallpaperHost.visible(true);
        if (returningFromProviderSettings && wallpaperProvider != null) {
            returningFromProviderSettings = false;
            wallpaperPreviewStarted = true; syncBackground(); wallpaperHost.refresh();
            handler.post(() -> providerDetails(wallpaperProvider, false));
        }
        // Package broadcasts reconcile installs, removal and enable/disable changes. Avoid rebuilding
        // the same surface on every return from an app: that used to drop remote focus for a frame.
        root.post(() -> {
            if (active && overlay == null) {
                restoreFocus();
                snapAppHighlight();
            }
        });
    }
    @Override protected void onPause() {
        active = false; restoringSurface = true; cancelOk();
        if (adjustmentCancel != null) cancelAdjustment();
        if (!returningFromProviderSettings) cancelWallpaperPreview();
        wallpaperHost.visible(false); wallpaperSurface.visible(false);
        if (activeMenu != null && activeMenu.paletteGrid) finishUndoGroup();
        gridSelector = null;
        menuStack.clear(); activeMenu = null; aboutBackButton = null;
        dismissMenu();
        if (moving) cancelMove();
        hideAppFocusHighlight();
        for (View tile : tiles.values()) {
            tile.animate().cancel();
            if (tile instanceof CircularAppTile) ((CircularAppTile) tile).stopAppearanceAnimation();
            if (tile instanceof BannerAppTile) ((BannerAppTile) tile).setAppFocused(tile.isFocused(), false);
        }
        saveNavigationState();
        for (AirQualityCard widget : airWidgets.values()) widget.stop();
        super.onPause();
    }
    @Override protected void onDestroy() {
        closed = true; loadVersion++;
        wallpaperHost.close(); wallpaperSurface.close();
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
        if (adjustmentCancel != null) cancelAdjustment();
        cancelWallpaperPreview();
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
    private void save() { save("Home layout"); }
    private void save(String label) {
        String next = layout.encode();
        boolean changed = lastSavedLayout != null && !lastSavedLayout.equals(next);
        if (changed && !applyingUndo) {
            if (undoGroupStart != null) {
                // A focus-preview sequence (the palette) is one user action.
            } else {
                pushUndo("layout", "", lastSavedLayout, label);
            }
        }
        preferences.edit().putString("layout", next).apply();
        lastSavedLayout = next;
        if (changed && !applyingUndo && undoGroupStart == null) showUndoBanner(label);
    }
    private void saveWithoutUndo() {
        String next = layout.encode();
        preferences.edit().putString("layout", next).apply();
        lastSavedLayout = next;
    }
    private JSONArray storedUndoEntries() {
        try { return new JSONArray(preferences.getString(PREF_UNDO_HISTORY, "[]")); }
        catch (Exception invalid) { return new JSONArray(); }
    }
    private void pushUndo(String kind, String key, String state, String label) {
        JSONArray old = storedUndoEntries();
        JSONArray next = new JSONArray();
        try {
            for (int i = Math.max(0, old.length() - MAX_UNDO_ENTRIES + 1); i < old.length(); i++)
                next.put(old.getJSONObject(i));
            next.put(new JSONObject().put("kind", kind).put("key", key)
                    .put("state", state).put("label", label));
            preferences.edit().putString(PREF_UNDO_HISTORY, next.toString()).apply();
        } catch (Exception ignored) { }
    }
    private void recordAirUndo(String key, String previousConfiguration, String label) {
        pushUndo("air", key, previousConfiguration, label);
        showUndoBanner(label);
    }
    private void beginUndoGroup(String label) {
        if (undoGroupStart == null) { undoGroupStart = lastSavedLayout; undoGroupLabel = label; }
    }
    private void finishUndoGroup() {
        if (undoGroupStart == null) return;
        String before = undoGroupStart, label = undoGroupLabel;
        undoGroupStart = null; undoGroupLabel = null;
        String now = layout.encode();
        if (before != null && !before.equals(now)) {
            pushUndo("layout", "", before, label);
            showUndoBanner(label);
        }
    }
    private void showUndoBanner(String label) {
        if (root == null || closed) return;
        handler.removeCallbacks(hideUndoBanner);
        if (undoBanner != null) root.removeView(undoBanner);
        undoBanner = new FrameLayout(this);
        UiBackgroundDrawable undoFill = shape(paletteColors.raised, paletteColors.focus, 2);
        undoFill.setFillOpacity(WallpaperSettings.PIE_BACKGROUND_OPACITY);
        undoBanner.setBackground(undoFill);
        undoBanner.setFocusable(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(22), dp(6), dp(10), dp(6));
        TextView message = text(label, 17);
        message.setSingleLine(true); message.setEllipsize(android.text.TextUtils.TruncateAt.END);
        row.addView(message, new LinearLayout.LayoutParams(0, -1, 1f));
        undoButton = text("UNDO", 17); undoButton.setTypeface(null, Typeface.BOLD);
        undoButton.setGravity(Gravity.CENTER); undoButton.setFocusable(true); undoButton.setClickable(true);
        undoButton.setPadding(dp(20), 0, dp(20), 0);
        undoButton.setContentDescription("Undo " + label);
        undoButton.setBackground(shape(paletteColors.focus, paletteColors.text, 1));
        undoButton.setOnClickListener(v -> undoThrough(0));
        row.addView(undoButton, new LinearLayout.LayoutParams(-2, dp(48)));
        undoBanner.addView(row, new FrameLayout.LayoutParams(-1, -1));
        int width = Math.min(dp(820), getResources().getDisplayMetrics().widthPixels - dp(80));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(width, dp(66), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        lp.bottomMargin = dp(28);
        root.addView(undoBanner, lp);
        if (overlay != null) overlay.bringToFront(); else undoBanner.bringToFront();
        handler.postDelayed(hideUndoBanner, 8_000L);
    }
    private final Runnable hideUndoBanner = () -> {
        if (root != null && undoBanner != null) {
            boolean hadFocus = undoButton != null && undoButton.hasFocus();
            root.removeView(undoBanner); undoBanner = null; undoButton = null;
            if (hadFocus && overlay == null) restoreFocus();
        }
    };
    private List<UndoEntry> readUndoHistory() {
        List<UndoEntry> entries = new ArrayList<>();
        JSONArray stored = storedUndoEntries();
        for (int i = stored.length() - 1; i >= 0; i--) try {
            JSONObject item = stored.getJSONObject(i);
            entries.add(new UndoEntry(item.optString("label", "Homeboard change"),
                    item.optString("kind", "layout"), item.optString("key", ""), item.optString("state", "")));
        } catch (Exception ignored) { }
        return entries;
    }
    private void openUndoHistory() {
        List<UndoEntry> entries = readUndoHistory();
        if (entries.isEmpty()) {
            menu("Undo history", "No recent Homeboard changes can be undone.", new String[0], ignored -> {});
            return;
        }
        String[] labels = new String[entries.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = "Undo · " + entries.get(i).label;
        menu("Undo history", "Newest first. Undoing an older change also reverses newer changes.",
                labels, this::undoThrough);
    }
    private void undoThrough(int displayedIndex) {
        List<UndoEntry> entries = readUndoHistory();
        if (displayedIndex < 0 || displayedIndex >= entries.size()) return;
        int targetIndex = entries.size() - 1 - displayedIndex;
        String restoredLabel = entries.get(displayedIndex).label;
        try {
            applyingUndo = true;
            for (int i = entries.size() - 1; i >= targetIndex; i--) {
                UndoEntry entry = entries.get(entries.size() - 1 - i);
                if ("layout".equals(entry.kind)) layout = HomeLayout.parse(entry.state);
                else if ("home-app-size".equals(entry.kind)) {
                    JSONObject state = new JSONObject(entry.state);
                    layout = HomeLayout.parse(state.getString("layout"));
                    homePage = state.getInt("page"); homeFocus = state.getString("focus");
                    if (!allApps) focused = homeFocus;
                }
                else if ("air".equals(entry.kind)) airFor(entry.key).restoreConfigurationSnapshot(entry.state);
            }
            JSONArray stored = storedUndoEntries();
            JSONArray remaining = new JSONArray();
            for (int i = 0; i < targetIndex; i++) remaining.put(stored.getJSONObject(i));
            preferences.edit().putString(PREF_UNDO_HISTORY, remaining.toString())
                    .putString("layout", layout.encode()).apply();
            lastSavedLayout = layout.encode();
            undoGroupStart = null; undoGroupLabel = null;
            background = paletteColor(); paletteColors = PaletteColors.from(background);
            root.setBackgroundColor(background); setAirPalette();
        } catch (Exception failure) {
            toast("Could not undo that change");
            return;
        } finally { applyingUndo = false; }
        handler.removeCallbacks(hideUndoBanner);
        if (undoBanner != null) root.removeView(undoBanner);
        undoBanner = null; undoButton = null;
        menuStack.clear(); activeMenu = null; gridSelector = null;
        if (overlay != null) { root.removeView(overlay); overlay = null; }
        syncBackground();
        render(0); saveNavigationState();
        showUndoBanner(displayedIndex == 0 ? "Undid " + restoredLabel
                : "Undid " + restoredLabel + " and newer changes");
    }
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
                if (migrated) saveWithoutUndo();
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
        return homeGridColumnsFor(layout.homeAppSize);
    }
    private int homeGridColumnsFor(String size) {
        int fit = homeGridFitColumnsFor(size);
        return layout.homeColumns == 0 ? fit : Math.min(layout.homeColumns, fit);
    }
    private int appLabelHeightDp() {
        android.graphics.Paint paint = new android.graphics.Paint();
        android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
        paint.setTextSize(16 * metrics.scaledDensity);
        android.graphics.Paint.FontMetricsInt font = paint.getFontMetricsInt();
        return (int) Math.ceil((font.bottom - font.top) / metrics.density);
    }
    private int homeGridFitColumns() {
        return homeGridFitColumnsFor(layout.homeAppSize);
    }
    private int homeGridFitColumnsFor(String size) {
        int screenWidthDp = Math.round(getResources().getDisplayMetrics().widthPixels
                / getResources().getDisplayMetrics().density);
        return AppGridSizing.homeColumns(screenWidthDp - AppGridSizing.SIDE_MARGIN * 2,
                appLabelHeightDp(), HomeLayout.APP_BANNER.equals(size));
    }
    private int homeGridFitRows() {
        return homeGridFitRowsFor(layout.homeAppSize);
    }
    private int homeGridFitRowsFor(String size) {
        int screenHeightDp = Math.round(getResources().getDisplayMetrics().heightPixels
                / getResources().getDisplayMetrics().density);
        return Math.max(2, AppGridSizing.rows(screenHeightDp - AppGridSizing.HOME_TOP
                - AppGridSizing.BOTTOM_MARGIN, appLabelHeightDp(), HomeLayout.APP_BANNER.equals(size)));
    }
    private int homeGridRows() {
        return homeGridRowsFor(layout.homeAppSize);
    }
    private int homeGridRowsFor(String size) {
        int fit = homeGridFitRowsFor(size);
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
        uiBackgroundOpacity = layout.uiOpacityPercent;
        syncBackground();
        surface.setFocusable(true);
        surface.setFocusableInTouchMode(true);
        surface.setClipChildren(false);
        root.addView(surface, new FrameLayout.LayoutParams(-1, -1));
        if (overlay != null) overlay.bringToFront();
        else if (undoBanner != null) undoBanner.bringToFront();
        if (allApps) buildApps(); else buildHome();
        applyUiBackgroundOpacity(uiBackgroundOpacity);
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
            if (active && overlay == null && renderedSurface == surface) snapAppHighlight();
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
            widget = new AirQualityCard(this, this::menu, key, this::requestAirLocation, this::recordAirUndo);
            widget.setBackgroundOpacity(uiBackgroundOpacity);
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
        StateListDrawable states = new UiBackgroundDrawable.States();
        GradientDrawable selected = shape(paletteColors.raised, paletteColors.focus, 3);
        states.addState(new int[]{android.R.attr.state_focused}, selected);
        states.addState(new int[]{}, shape(paletteColors.surface, paletteColors.edge, 1));
        return states;
    }
    private UiBackgroundDrawable shape(int color, int stroke, int width) {
        UiBackgroundDrawable d = new UiBackgroundDrawable(color); d.setCornerRadius(dp(14));
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
        StateListDrawable states = new UiBackgroundDrawable.States();
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
        boolean appTile = key.startsWith("app:");
        boolean denseApp = appTile && !layout.appUsesBanner(!allApps);
        view.setFocusable(true);
        view.setBackground(appTile ? null : tileBackground());
        view.setPadding(dp(appTile ? 2 : 16), dp(appTile ? 2 : 12),
                dp(appTile ? 2 : 16), dp(appTile ? 2 : 12));
        view.setOnFocusChangeListener((v, hasFocus) -> {
            if (isAirWidgetKey(key)) airFor(key).setTileFocused(hasFocus);
            boolean animateTile = denseApp && !restoringSurface && !reducedMotion();
            if (view instanceof CircularAppTile) {
                ((CircularAppTile) view).setAppFocused(hasFocus, animateTile);
            } else if (view instanceof BannerAppTile) {
                ((BannerAppTile) view).setAppFocused(hasFocus, !restoringSurface && !reducedMotion());
            } else {
                view.animate().cancel();
                view.animate().scaleX(hasFocus && denseApp ? APP_FOCUS_SCALE : 1f)
                        .scaleY(hasFocus && denseApp ? APP_FOCUS_SCALE : 1f)
                        .setDuration(animateTile ? AppAppearance.TRANSITION_MS : 0)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
            }
            if (hasFocus) {
                focused = key; if (allApps) appsFocus = key; else homeFocus = key;
                sendWallpaperFocus(key);
                if (appTile) moveAppFocusHighlight(appHighlightTarget(view), !restoringSurface);
            }
        });
        view.setOnClickListener(v -> activate(key));
        view.setOnLongClickListener(v -> { contextMenu(key); return true; });
        tiles.put(key, view);
    }
    private void createAppFocusHighlight() {
        boolean banners = layout.appUsesBanner(!allApps);
        boolean circular = !banners && layout.circularIcons;
        if (banners) {
            appFocusHighlight = new BannerFocusView(this);
        } else if (circular) {
            appFocusHighlight = new AppFocusView(this);
        } else {
            GlassView glass = new GlassView(this, null);
            glass.setPalette(background); glass.setRadius(dp(18)); glass.setInset(0);
            appFocusHighlight = glass;
        }
        appFocusHighlight.setAlpha(0f);
        appFocusHighlight.setFocusable(false);
        appFocusHighlight.setClickable(false);
        appFocusHighlight.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        // Circular fill/halo belongs behind artwork; existing glass stays above unmasked artwork.
        surface.addView(appFocusHighlight, banners || circular ? 0 : surface.getChildCount(),
                new FrameLayout.LayoutParams(dp(100), dp(100)));
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
        float damping = banners || circular ? 1f : 0.58f;
        SpringForce spring = new SpringForce().setStiffness(260f).setDampingRatio(damping);
        appFocusX.setSpring(spring);
        appFocusY.setSpring(new SpringForce().setStiffness(260f).setDampingRatio(damping));
        if (reducedMotion()) { appFocusX.setSpring(new SpringForce().setStiffness(1000f).setDampingRatio(1f));
            appFocusY.setSpring(new SpringForce().setStiffness(1000f).setDampingRatio(1f)); }
    }
    private void moveAppFocusHighlight(View card, boolean animate) {
        if (!active || overlay != null || appFocusHighlight == null || card.getParent() == null
                || card.getWidth() <= 0 || card.getHeight() <= 0) return;
        Rect rect = new Rect(0, 0, card.getWidth(), card.getHeight());
        surface.offsetDescendantRectToMyCoords(card, rect);
        int width, height;
        if (appFocusHighlight instanceof AppFocusView) {
            ((AppFocusView) appFocusHighlight).setIconDiameter(card.getWidth());
            width = height = AppAppearance.focusExtent(card.getWidth());
        } else if (appFocusHighlight instanceof BannerFocusView) {
            width = (int) Math.ceil(card.getWidth() * APP_FOCUS_SCALE) + dp(20);
            height = (int) Math.ceil(card.getHeight() * APP_FOCUS_SCALE) + dp(20);
        } else {
            int padding = 1;
            width = (int) Math.ceil(card.getWidth() * APP_FOCUS_SCALE) + padding * 2;
            height = (int) Math.ceil(card.getHeight() * APP_FOCUS_SCALE) + padding * 2;
            ((GlassView) appFocusHighlight).setRadius(dp(10));
        }
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
    private void hideAppFocusHighlight() {
        if (appFocusX != null) { appFocusX.cancel(); }
        if (appFocusY != null) { appFocusY.cancel(); }
        if (appFocusHighlight != null) { appFocusHighlight.setAlpha(0f); }
        appFocusHighlightShown = false;
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
        gp.setMargins(dp(AppGridSizing.SIDE_MARGIN), dp(AppGridSizing.HOME_TOP),
                dp(AppGridSizing.SIDE_MARGIN), dp(AppGridSizing.BOTTOM_MARGIN));
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
        if (layout.appUsesBanner(dashboard)) {
            Drawable banner = null, icon = null;
            if (app != null) {
                try { banner = app.info.activityInfo.loadBanner(getPackageManager()); }
                catch (RuntimeException ignored) { }
                if (banner == null) try { icon = app.info.loadIcon(getPackageManager()); }
                catch (RuntimeException ignored) { }
            }
            String label = app == null ? "Unavailable app" : app.label;
            BannerAppTile tile = new BannerAppTile(this, banner == null ? icon : banner, banner == null, label);
            tile.setContentDescription(label + (app == null ? "; hold OK to remove" : "; hold OK for menu"));
            return tile;
        }
        if (layout.circularIcons) {
            Drawable artwork = null;
            try { if (app != null) artwork = app.info.loadIcon(getPackageManager()); }
            catch (RuntimeException ignored) { }
            String label = app == null ? "Unavailable app" : app.label;
            CircularAppTile circular = new CircularAppTile(this, artwork, label);
            circular.setContentDescription(label + (app == null ? "; hold OK to remove" : "; hold OK for menu"));
            return circular;
        }
        LinearLayout tile = new LinearLayout(this); tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        if (app != null) {
            ImageView icon = new ImageView(this);
            icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            try {
                icon.setImageDrawable(app.info.loadIcon(getPackageManager()));
            } catch (RuntimeException ignored) { }
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            int iconSize = dp(48);
            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(iconSize, iconSize);
            iconLp.bottomMargin = dp(3);
            tile.addView(icon, iconLp);
        }
        String label = app == null ? "Unavailable app" : app.label;
        TextView name = text(label, 16);
        name.setGravity(Gravity.CENTER); name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
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
        gp.setMargins(dp(AppGridSizing.SIDE_MARGIN), dp(AppGridSizing.APPS_TOP),
                dp(AppGridSizing.SIDE_MARGIN), dp(AppGridSizing.BOTTOM_MARGIN)); surface.addView(grid, gp);
        int start = appsPage * pageSize;
        for (int i = start; i < Math.min(start + pageSize, items.size()); i++) {
            String key = items.get(i); View tile = appTile(key, false); configureTile(tile, key);
            grid.addTile(tile, (i - start) % columns, (i - start) / columns, 1);
        }
        if (start < items.size()) createAppFocusHighlight();
        footer = text(moving ? "Move preview · arrows reposition · OK saves · Back cancels"
                : "Left edge / Back → Home    Hold OK → menu    Page " + (appsPage + 1)
                    + "    " + items.size() + " apps", 13);
        FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams(-1, dp(36), Gravity.BOTTOM);
        fp.setMargins(dp(50), 0, dp(50), dp(10)); surface.addView(footer, fp);
    }
    /** Measures each cell once before layout; never requests another layout while laying out. */
    private int appColumns() {
        int screenWidthDp = Math.round(getResources().getDisplayMetrics().widthPixels
                / getResources().getDisplayMetrics().density);
        return AppGridSizing.columns(screenWidthDp - AppGridSizing.SIDE_MARGIN * 2,
                appLabelHeightDp(), layout.appBanners);
    }
    private int appRows() {
        int screenHeightDp = Math.round(getResources().getDisplayMetrics().heightPixels
                / getResources().getDisplayMetrics().density);
        return AppGridSizing.rows(screenHeightDp - AppGridSizing.APPS_TOP - AppGridSizing.BOTTOM_MARGIN,
                appLabelHeightDp(), layout.appBanners);
    }
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
                int childWidth = Math.max(0, width * cell[2] / columns - dp(AppGridSizing.COLUMN_GAP));
                int childHeight = Math.max(0, height * cell[3] / rows - dp(AppGridSizing.ROW_GAP));
                getChildAt(i).measure(View.MeasureSpec.makeMeasureSpec(childWidth, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(childHeight, View.MeasureSpec.EXACTLY));
            }
        }
        @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            int width = right - left, height = bottom - top;
            for (int i = 0; i < getChildCount(); i++) {
                int[] cell = positions.get(i);
                View child = getChildAt(i);
                int x = width * cell[0] / columns + dp(AppGridSizing.COLUMN_GAP / 2f);
                int y = height * cell[1] / rows + dp(AppGridSizing.ROW_GAP / 2f);
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
    private void snapAppHighlight() {
        if (focused == null || !focused.startsWith("app:")) return;
        View target = tiles.get(focused);
        if (target == null) return;
        target.post(() -> {
            if (!active || overlay != null
                    || target != tiles.get(focused) || !target.hasFocus() || appFocusHighlight == null) return;
            View icon = appHighlightTarget(target);
            if (icon.getWidth() <= 0 || icon.getHeight() <= 0) {
                // A laid-out cell may have no artwork room at large text sizes; do not spin forever.
                if (!icon.isLaidOut()) icon.postOnAnimation(this::snapAppHighlight);
                else hideAppFocusHighlight();
                return;
            }
            if (target instanceof CircularAppTile) ((CircularAppTile) target).setAppFocused(true, false);
            if (target instanceof BannerAppTile) ((BannerAppTile) target).setAppFocused(true, false);
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
        if (wallpaperHost != null && event.getAction() == KeyEvent.ACTION_DOWN) wallpaperHost.userActivity();
        if (adjustmentArm != null) {
            if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
                if (key == KeyEvent.KEYCODE_BACK || key == KeyEvent.KEYCODE_ESCAPE) backAdjustment();
                else if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER || key == KeyEvent.KEYCODE_NUMPAD_ENTER) adjustmentDone.run();
                else if (key >= KeyEvent.KEYCODE_DPAD_UP && key <= KeyEvent.KEYCODE_DPAD_RIGHT)
                    adjustmentArm.accept(key == KeyEvent.KEYCODE_DPAD_UP ? 0 : key == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : key == KeyEvent.KEYCODE_DPAD_DOWN ? 2 : 3);
            }
            return true;
        }
        boolean ok = key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER || key == KeyEvent.KEYCODE_NUMPAD_ENTER;
        if (gridSelector != null) {
            if (key == KeyEvent.KEYCODE_BACK || key == KeyEvent.KEYCODE_ESCAPE) {
                if (event.getAction() == KeyEvent.ACTION_UP) finishGridSelector(false);
                return true;
            }
            if (ok) {
                if (event.getAction() == KeyEvent.ACTION_UP) finishGridSelector(true);
                return true;
            }
            if (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) stepGridSelector(key == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : 1);
                return true;
            }
            return true;
        }
        if (activeMenu != null && activeMenu.panelRenderer != null) {
            if (key == KeyEvent.KEYCODE_BACK || key == KeyEvent.KEYCODE_ESCAPE) {
                if (event.getAction() == KeyEvent.ACTION_UP) backMenu();
                return true;
            }
            return super.dispatchKeyEvent(event);
        }
        if (undoButton != null && undoButton.hasFocus()) return super.dispatchKeyEvent(event);
        if (aboutBackButton != null && aboutBackButton.hasFocus()) return super.dispatchKeyEvent(event);
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
                        if (aboutBackButton != null) backMenu();
                        else { dismissMenu(); menuStack.clear(); }
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
                snapAppHighlight();
            }
        });
    }
    @Override public void onBackPressed() {
        if (adjustmentCancel != null) { backAdjustment(); return; }
        if (overlay != null) backMenu(); else if (moving) cancelMove(); else if (allApps) showHome();
    }
    private void navigate(int key) {
        if (undoButton != null && undoButton.hasFocus()) return;
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
        else if (key == KeyEvent.KEYCODE_DPAD_DOWN && undoButton != null) undoButton.requestFocus();
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
            menu("Air quality widget", "Choose an action for this widget", new String[]{"Configure location and source", "Move", "Resize", "More…"}, choice -> {
                if (choice == 0) airFor(key).configureWidget();
                if (choice == 1) startMove(key);
                if (choice == 2) startResize(key);
                if (choice == 3) menu("More · Air quality", "Delete removes this widget panel; Undo restores it.",
                        new String[]{"Delete widget", "Refresh reading", "Homeboard settings"}, more -> {
                            if (more == 0) deleteTile(key);
                            else if (more == 1) airFor(key).refreshNow();
                            else homeSettings();
                        });
            }); return;
        }
        App app = apps.get(key);
        String deleteLabel = allApps ? "Hide from All Apps" : "Remove from Home";
        menu(app == null ? "Unavailable app" : app.label, "Choose an action for this app",
                new String[]{"Move", layout.widgets.contains(key) ? "Unfavorite" : "Favorite", deleteLabel, "More…"}, choice -> {
                    switch (choice) {
                        case 0: startMove(key); break;
                        case 1:
                            if (layout.widgets.contains(key)) { layout.widgets.remove(key); layout.homePositions.remove(key); }
                            else layout.favorite(key);
                            save(layout.widgets.contains(key) ? "Added app to Home" : "Removed app from Home");
                            render(0); break;
                        case 2: deleteTile(key); break;
                        default: menu("More · " + (app == null ? "App" : app.label), "Android owns app and system actions.",
                                new String[]{"Uninstall app", "App info", "Homeboard settings"}, more -> {
                                    if (more == 0) uninstall(key);
                                    else if (more == 1) {
                                        ComponentName c = ComponentName.unflattenFromString(key.substring(4));
                                        if (c != null) open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                Uri.parse("package:" + c.getPackageName())));
                                    } else homeSettings();
                                }); break;
                    }
                });
    }
    private void deleteTile(String key) {
        boolean dashboard = !allApps;
        App app = apps.get(key);
        String label = isAirWidgetKey(key) ? "Air quality panel" : app == null ? "app shortcut" : app.label;
        layout.delete(key, dashboard);
        save(dashboard ? "Removed " + label + " from Home" : "Hid " + label + " from All Apps");
        if (dashboard && !layout.widgets.isEmpty()) {
            homePage = Math.min(homePage, homePageCount(layout.widgets) - 1);
            if (!layout.widgets.contains(focused)) focused = layout.widgets.isEmpty() ? "" : layout.widgets.get(0);
        }
        render(0);
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
        moving = false; preview = null; previewHomePositions = null; save("Moved item"); render(0);
    }
    private void cancelMove() {
        moving = false; preview = null; previewHomePositions = null;
        if (allApps) { int i = appItems().indexOf(focused); appsPage = Math.max(0, i / appPageSize()); }
        else homePage = homePageOf(focused, layout.widgets);
        render(0);
    }
    private void homeSettings() {
        menu("Home settings", "Choose an area to customize", new String[]{
                "Widgets & restore", "Appearance", "Home behavior", "Undo history",
                "Default Home app · " + (isDefaultHomeApp() ? "Homeboard" : "Choose"), "About Homeboard TV"}, choice -> {
            switch (choice) {
                case 0: widgetsAndRestore(); break;
                case 1: appearanceSettings(); break;
                case 2: homeBehaviorSettings(); break;
                case 3: openUndoHistory(); break;
                case 4: chooseDefaultHomeApp(); break;
                case 5: aboutHomeboard(); break;
                default: break;
            }
        });
    }

    private void widgetsAndRestore() {
        menu("Widgets & restore", "Add a widget or restore an app shortcut",
                new String[]{"Add widget", "Restore hidden app"}, choice -> {
                    if (choice == 0) menu("Add to Home", "Choose a tile to add",
                            new String[]{"Air quality widget", "Favorite an app from All Apps"}, add -> {
                                if (add == 0) {
                                    String key = newAirWidgetKey(); layout.addPanel(key); save("Added air-quality panel"); allApps = false; focused = key;
                                    homePage = homePageOf(focused, layout.widgets); render(0); }
                                else { allApps = false; showApps(); }
                            });
                    else restoreHiddenApps();
                });
    }

    private String newAirWidgetKey() {
        if (!layout.widgets.contains(HomeLayout.AIR)) return HomeLayout.AIR;
        int suffix = 2;
        while (layout.widgets.contains(HomeLayout.AIR + ":" + suffix)) suffix++;
        return HomeLayout.AIR + ":" + suffix;
    }

    private void appearanceSettings() {
        menu("Appearance", "Customize backgrounds and dashboard elements",
                new String[]{"Background", "Palette", "Icons", "UI opacity · " + layout.uiOpacityPercent + "%"}, choice -> {
                    if (choice == 0) backgroundSettings();
                    else if (choice == 1) colorPaletteSettings();
                    else if (choice == 2) appArtworkSettings();
                    else startAdjustment("UI opacity", layout.uiOpacityPercent,
                            this::applyUiBackgroundOpacity, value -> {
                                layout.uiOpacityPercent = value; save("UI opacity: " + value + "%"); render(0);
                            });
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
        beginUndoGroup("Changed background color");
        paletteMenu("Palette", "Move to a color to preview it", labels, choice -> {
            layout.palette = ids[choice];
            applyBackgroundChoice();
        }, selected);
    }

    private void solidColorSettings() {
        cancelWallpaperPreview();
        beginUndoGroup("Changed solid background");
        layout.wallpaper.component = ""; layout.wallpaper.uuid = "";
        save("Background: solid color"); syncBackground(); render(0);
        colorPaletteSettings();
    }

    private void applyBackgroundChoice() {
        background = paletteColor(); paletteColors = PaletteColors.from(background);
        setAirPalette();
        root.setBackgroundColor(background); save("Changed background"); render(0);
    }

    private void appArtworkSettings() {
        menu("Icons", "Choose artwork and icon shape",
                new String[]{"Home favorites · " + homeAppSizeLabel(),
                        "All Apps · " + (layout.appBanners ? "TV banners" : "Dense icons"),
                        "Circular icon mask · " + (layout.circularIcons ? "On" : "Off")}, choice -> {
                    if (choice == 0) homeAppSizeSettings();
                    else if (choice == 1) allAppsArtworkSettings();
                    else menu("Circular icon mask", "The icon keeps the same size",
                            new String[]{"Original icon shape", "Circular mask"}, mask -> {
                                layout.circularIcons = mask == 1;
                                appsPage = Math.max(0, appItems().indexOf(focused) / appPageSize());
                                save("Changed app icon shape"); render(0);
                            }, layout.circularIcons ? 1 : 0);
                });
    }
    private String homeAppSizeLabel() {
        return HomeLayout.APP_BANNER.equals(layout.homeAppSize) ? "Banners" : "Small icons";
    }
    private void homeAppSizeSettings() {
        menu("Home favorites", "Banners use wider cells and taller rows. Tiles may move to another row or page.",
                new String[]{"Small icons", "Banners"}, choice -> changeHomeAppSize(
                        choice == 0 ? HomeLayout.APP_SMALL : HomeLayout.APP_BANNER),
                HomeLayout.APP_BANNER.equals(layout.homeAppSize) ? 1 : 0);
    }
    private void changeHomeAppSize(String size) {
        String snapshot;
        try {
            snapshot = new JSONObject().put("layout", layout.encode()).put("page", homePage)
                    .put("focus", allApps ? homeFocus : focused).toString();
        } catch (org.json.JSONException invalid) { throw new IllegalStateException(invalid); }
        if (!layout.setHomeAppSize(size, homeGridColumns(), homeGridRows(),
                homeGridColumnsFor(size), homeGridRowsFor(size))) return;
        String label = "Home favorites: " + homeAppSizeLabel();
        pushUndo("home-app-size", "", snapshot, label);
        saveWithoutUndo();
        String initiatingFocus = allApps ? homeFocus : focused;
        if (layout.widgets.contains(initiatingFocus)) homePage = homePageOf(initiatingFocus, layout.widgets);
        else homePage = Math.min(homePage, homePageCount(layout.widgets) - 1);
        saveNavigationState();
        render(0); showUndoBanner(label);
    }
    private void allAppsArtworkSettings() {
        boolean returnToAllApps = allApps;
        menu("All Apps", "Uses " + (layout.appBanners ? "TV banners · responsive grid" : "launcher icons · dense grid")
                        + " · Home favorites use " + homeAppSizeLabel(),
                new String[]{"Launcher icons · dense grid", "TV banners · responsive grid"}, artwork -> {
                    layout.appBanners = artwork == 1;
                    if (returnToAllApps) {
                        // The grid can change page size between icons and banners. Keep
                        // the user in All Apps and focus a tile that belongs to the new visible page.
                        allApps = true;
                        List<String> items = appItems();
                        if (!items.isEmpty()) {
                            int index = items.indexOf(focused);
                            if (index < 0) index = Math.min(appsPage * appPageSize(), items.size() - 1);
                            appsPage = index / appPageSize();
                            focused = items.get(index);
                            appsFocus = focused;
                        }
                    }
                    save("Changed All Apps artwork"); render(0);
                }, layout.appBanners ? 1 : 0);
    }

    private void homeBehaviorSettings() {
        String columns = layout.homeColumns == 0 ? "Auto" : String.valueOf(layout.homeColumns);
        String rows = layout.homeRows == 0 ? "Auto" : String.valueOf(Math.min(layout.homeRows, homeGridFitRows()));
        menu("Home behavior", "Choose grid density, new panel size or screen movement",
                new String[]{"Grid columns · " + columns,
                        "Grid rows · " + rows,
                        "New panel size · " + panelSizeLabel(layout.defaultPanelSize),
                        "Reduced motion · " + (layout.reducedMotion ? "On" : "Off")}, choice -> {
                    if (choice == 0) showGridSelector(true);
                    else if (choice == 1) showGridSelector(false);
                    else if (choice == 2) panelSizeMenu(null, true);
                    else menu("Reduced motion", "Choose how Home and All Apps move",
                            new String[]{"Off", "On"}, motion -> {
                                layout.reducedMotion = motion == 1; save("Changed reduced motion"); render(0);
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
        if (commit) { save("Resized panel"); }
        else if (resizePreviousSpan == null) { layout.panelSpans.remove(resizingKey); }
        else { layout.setPanelSpan(resizingKey, resizePreviousSpan.width, resizePreviousSpan.height); }
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
                    save(defaults ? "Changed new panel size" : "Changed panel size");
                    if (!defaults) homePage = homePageOf(key, layout.widgets);
                    render(0);
                    if (defaults) homeBehaviorSettings();
                }, selected - 1);
    }

    private void showGridSelector(boolean columns) {
        int fit = columns ? homeGridFitColumns() : homeGridFitRows();
        List<Integer> supported = new ArrayList<>();
        supported.add(0); // Auto always remains available.
        if (columns) {
            for (int n : new int[]{4, 6, 8, 10, 12}) if (n <= fit) supported.add(n);
        } else {
            for (int n = 2; n <= fit; n++) supported.add(n);
        }
        int[] values = new int[supported.size()];
        String[] labels = new String[supported.size()];
        for (int i = 0; i < supported.size(); i++) {
            values[i] = supported.get(i);
            labels[i] = values[i] == 0 ? "Auto" : columns ? values[i] + " columns" : values[i] + " rows";
        }
        int configured = columns ? layout.homeColumns : layout.homeRows;
        int effective = configured == 0 ? 0 : Math.min(configured, fit);
        int selected = 0;
        int nearestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == effective) { selected = i; nearestDistance = 0; break; }
            if (values[i] != 0 && Math.abs(values[i] - effective) < nearestDistance) {
                selected = i; nearestDistance = Math.abs(values[i] - effective);
            }
        }
        gridSelector = new GridSelector(columns, values, labels, selected);
        menuGeneration++;
        surface.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        surface.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        hideAppFocusHighlight();
        overlay = new FrameLayout(this);
        overlay.setBackgroundColor(paletteColors.scrim); overlay.setFocusable(true); overlay.setFocusableInTouchMode(true);
        overlay.setContentDescription((columns ? "Grid columns" : "Grid rows") + " selector");
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
        renderGridSelector();
    }
    private void renderGridSelector() {
        if (gridSelector == null || overlay == null) return;
        overlay.removeAllViews();
        GridSelector selector = gridSelector;
        boolean columns = selector.columns;
        String unit = columns ? "columns" : "rows";
        int fit = columns ? homeGridFitColumns() : homeGridFitRows();
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL); panel.setGravity(Gravity.CENTER);
        panel.setPadding(dp(32), dp(24), dp(32), dp(22));
        panel.setBackground(shape(paletteColors.raised, paletteColors.edge, 2));
        TextView title = text(columns ? "Grid columns" : "Grid rows", 26);
        title.setTypeface(null, Typeface.BOLD); title.setGravity(Gravity.CENTER);
        panel.addView(title, new LinearLayout.LayoutParams(-1, dp(42)));
        TextView subtitle = text("Bounded to this TV's fit: " + fit + " " + unit, 16);
        subtitle.setTextColor(paletteColors.muted); subtitle.setGravity(Gravity.CENTER);
        panel.addView(subtitle, new LinearLayout.LayoutParams(-1, dp(34)));

        LinearLayout choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.HORIZONTAL); choices.setGravity(Gravity.CENTER);
        String previous = selector.index > 0 ? selector.labels[selector.index - 1] : "End";
        String next = selector.index + 1 < selector.labels.length ? selector.labels[selector.index + 1] : "End";
        TextView before = text("‹   " + previous, 17); before.setTextColor(paletteColors.muted); before.setGravity(Gravity.CENTER);
        choices.addView(before, new LinearLayout.LayoutParams(0, dp(88), 1f));
        TextView current = text(selector.labels[selector.index], 27); current.setTypeface(null, Typeface.BOLD);
        current.setGravity(Gravity.CENTER); current.setTextColor(paletteColors.text);
        current.setBackground(shape(paletteColors.focus, paletteColors.text, 2));
        choices.addView(current, new LinearLayout.LayoutParams(0, dp(88), 1.3f));
        TextView after = text(next + "   ›", 17); after.setTextColor(paletteColors.muted); after.setGravity(Gravity.CENTER);
        choices.addView(after, new LinearLayout.LayoutParams(0, dp(88), 1f));
        LinearLayout.LayoutParams choicesLp = new LinearLayout.LayoutParams(-1, dp(100));
        choicesLp.topMargin = dp(18); panel.addView(choices, choicesLp);

        TextView hint = text("← / → change · OK apply · Back cancel", 15);
        hint.setTextColor(paletteColors.muted); hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(-1, dp(34));
        hintLp.topMargin = dp(14); panel.addView(hint, hintLp);
        int width = Math.min(dp(780), getResources().getDisplayMetrics().widthPixels - dp(80));
        int height = dp(300);
        overlay.addView(panel, new FrameLayout.LayoutParams(width, height, Gravity.CENTER));
        overlay.requestFocus();
    }
    private void stepGridSelector(int direction) {
        if (gridSelector == null) return;
        int index = Math.max(0, Math.min(gridSelector.values.length - 1, gridSelector.index + direction));
        if (index != gridSelector.index) { gridSelector.index = index; renderGridSelector(); }
    }
    private void finishGridSelector(boolean apply) {
        if (gridSelector == null) return;
        GridSelector selector = gridSelector;
        gridSelector = null;
        if (apply) {
            int value = selector.values[selector.index];
            if (selector.columns) layout.homeColumns = value; else layout.homeRows = value;
            save(selector.columns ? "Changed Home grid columns" : "Changed Home grid rows");
            homePage = homePageOf(focused, layout.widgets);
            render(0);
            dismissMenu();
            if (!menuStack.isEmpty()) {
                activeMenu = menuStack.remove(menuStack.size() - 1);
                showMenuPage(activeMenu, activeMenu.page, activeMenu.focusedChoice);
            }
        } else {
            MenuSpec parent = activeMenu;
            dismissMenu();
            activeMenu = parent;
            if (parent != null) showMenuPage(parent, parent.page, parent.focusedChoice);
        }
    }

    private void aboutHomeboard() {
        openMenu(new MenuSpec("About Homeboard TV", this::showAboutDialog));
    }
    private void showAboutDialog() {
        if (overlay != null) root.removeView(overlay);
        surface.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        surface.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        hideAppFocusHighlight();
        overlay = new FrameLayout(this);
        overlay.setBackgroundColor(paletteColors.scrim);
        overlay.setFocusable(true); overlay.setFocusableInTouchMode(true);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(26), dp(18), dp(26), dp(14));
        panel.setBackground(shape(paletteColors.raised, paletteColors.edge, 2));
        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.HORIZONTAL); heading.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icon = new ImageView(this);
        try { icon.setImageDrawable(getPackageManager().getApplicationIcon(getPackageName())); }
        catch (PackageManager.NameNotFoundException ignored) { }
        heading.addView(icon, new LinearLayout.LayoutParams(dp(56), dp(56)));
        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleBlockLp = new LinearLayout.LayoutParams(0, -2, 1f);
        titleBlockLp.leftMargin = dp(22); heading.addView(titleBlock, titleBlockLp);
        TextView title = text("Homeboard TV", 29); title.setTypeface(null, Typeface.BOLD);
        TextView version = text("Version " + appVersion() + " · Your apps and widgets, arranged your way", 16);
        version.setTextColor(paletteColors.muted);
        titleBlock.addView(title); titleBlock.addView(version);
        panel.addView(heading, new LinearLayout.LayoutParams(-1, dp(72)));

        TextView purpose = text("A customizable home screen for your TV, built on CleanYourTV.", 16);
        purpose.setPadding(0, dp(8), 0, dp(8));
        panel.addView(purpose);
        TextView sourcesHeading = text("PROJECT SOURCES & LICENSES", 13);
        sourcesHeading.setTextColor(paletteColors.focus); sourcesHeading.setTypeface(null, Typeface.BOLD);
        sourcesHeading.setPadding(0, dp(2), 0, dp(8)); panel.addView(sourcesHeading);

        LinearLayout sourceGrid = new LinearLayout(this);
        sourceGrid.setOrientation(LinearLayout.VERTICAL);
        AboutSource[] sources = AboutSource.values();
        for (int row = 0; row < 2; row++) {
            LinearLayout sourceRow = new LinearLayout(this);
            sourceRow.setOrientation(LinearLayout.HORIZONTAL);
            for (int column = 0; column < 2; column++) {
                View link = aboutSourceLink(sources[row * 2 + column]);
                LinearLayout.LayoutParams linkLp = new LinearLayout.LayoutParams(0, dp(56), 1f);
                linkLp.setMargins(column == 0 ? 0 : dp(10), dp(4), column == 0 ? dp(5) : 0, dp(4));
                sourceRow.addView(link, linkLp);
            }
            sourceGrid.addView(sourceRow, new LinearLayout.LayoutParams(-1, dp(64)));
        }
        panel.addView(sourceGrid);
        TextView wallpaperCredits = text("Projectivy wallpaper API · Spocky · Apache-2.0\nLottie Android · Airbnb · Apache-2.0", 12);
        wallpaperCredits.setPadding(0, dp(6), 0, 0); panel.addView(wallpaperCredits);
        TextView linkHint = text("Select a source to show its web address and phone QR code.", 13);
        linkHint.setPadding(0, dp(6), 0, 0); panel.addView(linkHint);

        TextView closeText = text("←  BACK", 15); closeText.setTypeface(null, Typeface.BOLD);
        closeText.setTextColor(paletteColors.text); closeText.setGravity(Gravity.CENTER);
        closeText.setFocusable(true); closeText.setClickable(true);
        closeText.setPadding(dp(14), 0, dp(14), 0);
        closeText.setBackground(tileBackground());
        closeText.setOnClickListener(v -> backMenu());
        aboutBackButton = closeText;
        LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(-2, dp(42));
        backLp.topMargin = dp(8); panel.addView(closeText, backLp);

        int width = Math.min(dp(900), getResources().getDisplayMetrics().widthPixels - dp(96));
        overlay.addView(panel, new FrameLayout.LayoutParams(width, -2, Gravity.CENTER));
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
        closeText.requestFocus();
    }
    private View aboutSourceLink(AboutSource source) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(7), dp(14), dp(7));
        card.setBackground(tileBackground());
        card.setFocusable(true); card.setClickable(true);
        card.setContentDescription(source.title + ". " + source.detail + ". Show web address and phone QR code.");
        TextView heading = text(source.title + "  →", 17); heading.setTypeface(null, Typeface.BOLD);
        TextView caption = text(source.detail, 13); caption.setTextColor(paletteColors.muted);
        card.addView(heading); card.addView(caption);
        card.setOnClickListener(v -> openMenu(new MenuSpec(source.title, () -> showAboutSource(source))));
        return card;
    }
    private Intent sourceIntent(AboutSource source) {
        return new Intent(Intent.ACTION_VIEW, Uri.parse(source.url)).addCategory(Intent.CATEGORY_BROWSABLE);
    }
    private void showAboutSource(AboutSource source) {
        if (overlay != null) root.removeView(overlay);
        overlay = new FrameLayout(this); overlay.setBackgroundColor(paletteColors.scrim);
        LinearLayout panel = new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(26), dp(22), dp(26), dp(20));
        panel.setBackground(shape(paletteColors.raised, paletteColors.edge, 2));
        TextView title = text(source.title, 26); title.setTypeface(null, Typeface.BOLD);
        panel.addView(title, new LinearLayout.LayoutParams(-1, dp(44)));
        LinearLayout body = new LinearLayout(this); body.setGravity(Gravity.CENTER_VERTICAL);
        ImageView qr = new ImageView(this); qr.setImageResource(source.qrResource);
        qr.setContentDescription("QR code for " + source.url); qr.setScaleType(ImageView.ScaleType.FIT_CENTER);
        if (qr.getDrawable() instanceof android.graphics.drawable.BitmapDrawable)
            ((android.graphics.drawable.BitmapDrawable) qr.getDrawable()).setFilterBitmap(false);
        body.addView(qr, new LinearLayout.LayoutParams(dp(240), dp(240)));
        LinearLayout words = new LinearLayout(this); words.setOrientation(LinearLayout.VERTICAL);
        words.setPadding(dp(28), 0, 0, 0);
        words.addView(text("Open on your phone", 22));
        words.addView(text("Scan the QR code with your phone's camera.", 17));
        TextView address = text(source.url, 16); address.setTextIsSelectable(false);
        address.setPadding(0, dp(18), 0, dp(18)); words.addView(address);
        TextView status = text(source.detail, 15); status.setTextColor(paletteColors.muted); words.addView(status);
        if (getPackageManager().resolveActivity(sourceIntent(source), PackageManager.MATCH_DEFAULT_ONLY) != null) {
            TextView openOnTv = infoButton("Open on this TV", () -> {
                try { startActivity(sourceIntent(source)); }
                catch (RuntimeException unavailable) { status.setText("This TV could not open the page. Scan the QR code with your phone."); }
            });
            LinearLayout.LayoutParams openLp = new LinearLayout.LayoutParams(-1, dp(52)); openLp.topMargin = dp(14);
            words.addView(openOnTv, openLp);
        } else {
            status.setText(source.detail + "\nNo web browser is available on this TV. Scan with your phone.");
        }
        body.addView(words, new LinearLayout.LayoutParams(0, -2, 1));
        panel.addView(body, new LinearLayout.LayoutParams(-1, dp(260)));
        aboutBackButton = infoButton("Back to About", this::backMenu);
        LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(-2, dp(48)); backLp.topMargin = dp(12);
        panel.addView(aboutBackButton, backLp);
        int width = Math.min(dp(880), getResources().getDisplayMetrics().widthPixels - dp(64));
        overlay.addView(panel, new FrameLayout.LayoutParams(width, -2, Gravity.CENTER));
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1)); aboutBackButton.requestFocus();
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
    private void restoreHiddenApps() {
        List<String> keys = new ArrayList<>(layout.hidden); Collections.sort(keys);
        if (keys.isEmpty()) {
            menu("Restore hidden app", "No hidden app shortcuts. Apps remain installed when a shortcut is hidden.",
                    new String[0], ignored -> {});
            return;
        }
        String[] labels = new String[keys.size()];
        for (int i = 0; i < keys.size(); i++) labels[i] = apps.containsKey(keys.get(i)) ? apps.get(keys.get(i)).label : keys.get(i).substring(4);
        menu("Restore hidden app", "Restores this shortcut to All Apps; it does not reinstall the app.", labels, choice -> {
            String restored = labels[choice];
            layout.hidden.remove(keys.get(choice)); save("Restored " + restored + " to All Apps"); render(0);
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
        if (activeMenu != null && (invokingMenuAction || activeMenu.panelRenderer != null)) menuStack.add(activeMenu);
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
        if (spec.panelRenderer != null) { spec.panelRenderer.run(); return; }
        if (!spec.paletteGrid) {
            showPieMenuPage(spec, page, focusChoice);
            return;
        }
        surface.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        surface.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        if (overlay != null) root.removeView(overlay);
        hideAppFocusHighlight();
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
        hideAppFocusHighlight();
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
        String centerAction = spec.labels.length == 0 ? "Back" : spec.pageCount > 1
                ? page + 1 < spec.pageCount ? "More" : "First page"
                : menuStack.isEmpty() ? "Close" : "Back";
        String pageHint = spec.pageCount > 1 ? "Page " + (page + 1) + " of " + spec.pageCount : "";
        String breadcrumb = "Provider background".equals(spec.title) || spec.labels.length == 0 ? spec.subtitle : spec.labels.length == 0 && "Restore hidden app".equals(spec.title)
                ? "No hidden app shortcuts"
                : menuStack.isEmpty() ? spec.subtitle
                : menuStack.get(menuStack.size() - 1).title + " › " + spec.title;
        String interactionHint = spec.pageCount > 1
                ? "Page " + (page + 1) + " of " + spec.pageCount + " · OK changes page"
                : "Directions choose · Back returns";
        if (spec.labels.length == 0 && "Restore hidden app".equals(spec.title))
            interactionHint = "Apps stay installed · Back returns";
        PieMenuView pie = new PieMenuView(this, paletteColors);
        if ("Provider background".equals(spec.title) && wallpaperProvider != null) interactionHint = wallpaperProvider.readiness();
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
        if (value.startsWith("undo") || value.contains("undo history")) return MenuIconView.Kind.UNDO;
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
        backgroundInfo = false;
        backgroundStatusText = null;
        if (overlay == null) return;
        if (activeMenu != null && activeMenu.paletteGrid) finishUndoGroup();
        root.removeView(overlay); overlay = null;
        aboutBackButton = null;
        surface.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        surface.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
        restoreFocus();
        if (undoBanner != null) undoBanner.bringToFront();
    }
    private void backMenu() {
        if (overlay == null) return;
        if (adjustmentCancel != null) { backAdjustment(); return; }
        if (activeMenu != null && "Provider background".equals(activeMenu.title)) cancelWallpaperPreview();
        if (activeMenu != null && activeMenu.page > 0) {
            int page = activeMenu.page - 1;
            int focus = Math.min(page * activeMenu.pageSize, activeMenu.labels.length - 1);
            showMenuPage(activeMenu, page, focus);
            return;
        }
        if (!menuStack.isEmpty()) {
            dismissMenu();
            activeMenu = menuStack.remove(menuStack.size() - 1);
            if ("Background".equals(activeMenu.title)) activeMenu = backgroundMenuSpec();
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

    @Override public void onUserInteraction() {
        super.onUserInteraction();
        if (wallpaperHost != null) wallpaperHost.userActivity();
    }
    private void syncBackground() {
        if (wallpaperHost == null) return;
        WallpaperSettings selected = wallpaperDraft == null || !wallpaperPreviewStarted ? layout.wallpaper : wallpaperDraft;
        wallpaperSurface.options(selected.solid() ? 0 : selected.dimPercent, reducedMotion());
        wallpaperHost.select(selected);
    }
    private void applyUiBackgroundOpacity(int percent) {
        uiBackgroundOpacity = WallpaperSettings.percent(percent);
        uiBackdrop.setBackgroundColor(UiBackgroundDrawable.colorWithOpacity(background, uiBackgroundOpacity));
        for (AirQualityCard widget : airWidgets.values()) widget.setBackgroundOpacity(uiBackgroundOpacity);
        UiBackgroundDrawable.apply(surface, uiBackgroundOpacity);
    }
    private void sendWallpaperFocus(String key) {
        App app = apps.get(key);
        if (app == null || wallpaperHost == null) return;
        // Sample app artwork, not dashboard palette colors, for the public CardFocused event.
        int r = 0, g = 0, b = 0, count = 0;
        try {
            Drawable icon = app.info.loadIcon(getPackageManager());
            android.graphics.Bitmap sample = android.graphics.Bitmap.createBitmap(8, 8, android.graphics.Bitmap.Config.ARGB_8888);
            icon.setBounds(0, 0, 8, 8); icon.draw(new android.graphics.Canvas(sample));
            for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) {
                int color = sample.getPixel(x, y);
                if (Color.alpha(color) > 128) { r += Color.red(color); g += Color.green(color); b += Color.blue(color); count++; }
            }
            sample.recycle();
        } catch (RuntimeException ignored) { }
        int base = count == 0 ? Color.GRAY : Color.rgb(r / count, g / count, b / count);
        int light = Color.rgb((Color.red(base) + 255) / 2, (Color.green(base) + 255) / 2, (Color.blue(base) + 255) / 2);
        int dark = Color.rgb(Color.red(base) / 2, Color.green(base) / 2, Color.blue(base) / 2);
        wallpaperHost.appFocused(app.label, app.component.getPackageName(), light, dark);
    }
    private void backgroundSettings() {
        openMenu(backgroundMenuSpec());
    }
    private MenuSpec backgroundMenuSpec() {
        String[] choices = layout.wallpaper.solid()
                ? new String[]{"Solid color", "Projectivy plugins", "Background details"}
                : new String[]{"Solid color", "Projectivy plugins", "Background details", "Retry provider"};
        return new MenuSpec("Background", layout.wallpaper.solid() ? "Solid color" : "Projectivy wallpaper provider selected",
                choices, choice -> {
                    if (choice == 0) solidColorSettings();
                    else if (choice == 1) providerList();
                    else if (choice == 2) backgroundDetails();
                    else {
                        if (layout.wallpaper.solid()) { providerList(); return; }
                        cancelWallpaperPreview(); syncBackground(); wallpaperHost.refresh(); backgroundDetails();
                    }
                }, -1);
    }
    private void providerList() {
        List<ProjectivyProviderRegistry.Provider> providers = new ProjectivyProviderRegistry(this).discover();
        String[] labels = new String[providers.size()];
        for (int i = 0; i < labels.length; i++) {
            ProjectivyProviderRegistry.Provider p = providers.get(i);
            long sameUuid = providers.stream().filter(other -> other.uuid.equals(p.uuid)).count();
            labels[i] = p.name + (sameUuid > 1 ? " · " + p.component.getPackageName() : "") + " · " + p.readiness();
        }
        menu("Projectivy plugins", providers.isEmpty() ? "No providers installed. Install a wallpaper-provider app, then return here."
                : "Choose an installed provider to preview", labels, choice -> {
                    ProjectivyProviderRegistry.Provider p = providers.get(choice);
                    if (!p.usable()) { toast(p.problem); providerList(); return; }
                    wallpaperProvider = p;
                    wallpaperDraft = layout.wallpaper.copy();
                    wallpaperDraft.component = p.component.flattenToString(); wallpaperDraft.uuid = p.uuid;
                    wallpaperReady = false; wallpaperPreviewStarted = false; wallpaperStatus = "Choose Preview to load this provider";
                    providerDetails(p, false);
                });
    }
    private void providerDetails(ProjectivyProviderRegistry.Provider p, boolean replace) {
        MenuSpec spec = new MenuSpec("Provider background", wallpaperStatus + " · " + p.name + " · " + p.readiness(),
                new String[]{"Preview", "Plugin settings", wallpaperReady ? "Use background" : "Use background · preview first", "More options"}, choice -> {
                    if (choice == 0) {
                        if (wallpaperDraft == null) { wallpaperDraft = layout.wallpaper.copy(); wallpaperDraft.component = p.component.flattenToString(); wallpaperDraft.uuid = p.uuid; }
                        wallpaperPreviewStarted = true; wallpaperReady = false; wallpaperStatus = "Loading wallpaper…";
                        syncBackground(); wallpaperHost.refresh(); providerDetails(p, true);
                    } else if (choice == 1) {
                        Intent settings = new ProjectivyProviderRegistry(this).settings(p);
                        if (settings == null) { toast("No accessible plugin settings"); providerDetails(p, true); }
                        else {
                            returningFromProviderSettings = true;
                            try { startActivity(settings); }
                            catch (RuntimeException invalid) { returningFromProviderSettings = false; toast("Plugin settings are unavailable"); providerDetails(p, true); }
                        }
                    } else if (choice == 2) {
                        if (!wallpaperReady || wallpaperDraft == null) { toast("Preview a usable wallpaper first"); providerDetails(p, true); return; }
                        layout.wallpaper = wallpaperDraft.copy(); wallpaperDraft = null;
                        save("Background: " + p.name); syncBackground(); render(0);
                    } else providerOptions(p);
                }, -1);
        if (replace) {
            // Returning from provider options replaces that subtree. Back must reach the
            // provider list, rather than a stale options callback after discarding the draft.
            for (int i = menuStack.size() - 1; i >= 0; i--) {
                if ("Provider background".equals(menuStack.get(i).title)) {
                    while (menuStack.size() > i) menuStack.remove(menuStack.size() - 1);
                    break;
                }
            }
            activeMenu = spec; menuGeneration++; showMenuPage(spec, 0, 0);
        } else openMenu(spec);
    }
    private void refreshProviderDetails() {
        if (backgroundInfo && backgroundStatusText != null) backgroundStatusText.setText(backgroundInfoText());
        if (!closed && activeMenu != null && "Provider background".equals(activeMenu.title)
                && overlay != null && wallpaperProvider != null) providerDetails(wallpaperProvider, true);
    }
    private void providerOptions(ProjectivyProviderRegistry.Provider p) {
        if (wallpaperDraft == null) wallpaperDraft = layout.wallpaper.copy();
        menu("Wallpaper options", "Host display options; provider content stays in Plugin settings",
                new String[]{"Rotation · " + wallpaperDraft.rotationMinutes + " min", "Dimming · " + wallpaperDraft.dimPercent + "%", "Background details", "Back to provider"}, choice -> {
                    if (choice == 0) menu("Wallpaper rotation", "Batch cache expiry is controlled by the provider",
                            new String[]{"1 minute", "5 minutes", "15 minutes", "60 minutes"}, n -> {
                                wallpaperDraft.rotationMinutes = new int[]{1, 5, 15, 60}[n]; syncBackground(); providerDetails(p, true);
                            });
                    else if (choice == 1) startAdjustment("Wallpaper dimming", wallpaperDraft.dimPercent,
                            value -> wallpaperSurface.options(value, reducedMotion()), value -> {
                                wallpaperDraft.dimPercent = value; syncBackground(); providerDetails(p, true);
                            });
                    else if (choice == 2) backgroundDetails();
                    else providerDetails(p, true);
                });
    }
    private void backgroundDetails() {
        openMenu(new MenuSpec("Background details", this::drawBackgroundDetails));
    }
    private String backgroundInfoText() {
        WallpaperSettings selected = wallpaperDraft != null && wallpaperPreviewStarted ? wallpaperDraft : layout.wallpaper;
        String info;
        if (selected.solid()) {
            info = "Source: Solid color\nPalette: " + layout.palette + String.format(java.util.Locale.ROOT, " · #%06X", background & 0xffffff);
        } else {
            ProjectivyProviderRegistry.Provider provider = wallpaperProvider;
            if (provider == null || !provider.component.flattenToString().equals(selected.component))
                provider = new ProjectivyProviderRegistry(this).find(selected);
            info = "Source: " + (provider == null ? "Missing provider" : provider.name)
                    + "\n" + wallpaperStatus;
            if (provider != null && !provider.usable()) info += "\n" + provider.readiness();
        }
        info += "\nUI background opacity: " + layout.uiOpacityPercent + "%";
        if (!selected.solid() && layout.uiOpacityPercent == 100) info += " · covers wallpaper";
        tv.projectivy.plugin.wallpaperprovider.api.Wallpaper item = displayedWallpaper;
        if (item != null && !selected.solid()) info += "\nTitle: " + (item.getTitle() == null ? "Untitled" : item.getTitle())
                + "\nAuthor: " + (item.getAuthor() == null ? "Not provided" : item.getAuthor())
                + "\nSource: " + (item.getSource() == null ? "Not provided" : item.getSource());
        return info;
    }
    private void drawBackgroundDetails() {
        tv.projectivy.plugin.wallpaperprovider.api.Wallpaper item = displayedWallpaper;
        if (overlay != null) root.removeView(overlay);
        backgroundInfo = true;
        surface.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        overlay = new FrameLayout(this); overlay.setBackgroundColor(paletteColors.scrim);
        LinearLayout panel = new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(28), dp(24), dp(28), dp(20));
        UiBackgroundDrawable panelFill = shape(paletteColors.raised, paletteColors.edge, 1);
        panelFill.setFillOpacity(WallpaperSettings.PIE_BACKGROUND_OPACITY);
        panel.setBackground(panelFill);
        panel.addView(text("Background details", 24), new LinearLayout.LayoutParams(-1, -2));
        TextView details = text(backgroundInfoText(), 18); details.setMaxLines(8); details.setEllipsize(android.text.TextUtils.TruncateAt.END);
        backgroundStatusText = details;
        LinearLayout.LayoutParams detailLp = new LinearLayout.LayoutParams(-1, -2); detailLp.topMargin = dp(16); panel.addView(details, detailLp);
        LinearLayout buttons = new LinearLayout(this); buttons.setGravity(Gravity.CENTER_VERTICAL);
        if (item != null && item.getActionUri() != null) {
            TextView link = infoButton("Open wallpaper link", () -> openWallpaperLink(item.getActionUri()));
            buttons.addView(link, new LinearLayout.LayoutParams(0, dp(60), 1));
        }
        buttons.addView(infoButton("Choose color", this::solidColorSettings), new LinearLayout.LayoutParams(0, dp(60), 1));
        buttons.addView(infoButton("Choose provider", this::providerList), new LinearLayout.LayoutParams(0, dp(60), 1));
        aboutBackButton = infoButton("Back", this::backMenu);
        buttons.addView(aboutBackButton, new LinearLayout.LayoutParams(0, dp(60), 1));
        LinearLayout.LayoutParams buttonsLp = new LinearLayout.LayoutParams(-1, dp(64)); buttonsLp.topMargin = dp(16); panel.addView(buttons, buttonsLp);
        int width = Math.min(dp(840), getResources().getDisplayMetrics().widthPixels - dp(64));
        overlay.addView(panel, new FrameLayout.LayoutParams(width, -2, Gravity.CENTER));
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1)); aboutBackButton.requestFocus();
    }
    private TextView infoButton(String label, Runnable action) {
        TextView button = text(label, 17); button.setGravity(Gravity.CENTER); button.setFocusable(true); button.setClickable(true);
        button.setBackground(tileBackground()); button.setOnClickListener(v -> action.run()); return button;
    }
    private void openWallpaperLink(String value) {
        Uri uri = Uri.parse(value); String scheme = uri.getScheme();
        if (scheme == null || java.util.Arrays.asList("file", "content", "android.resource", "data", "javascript", "intent").contains(scheme.toLowerCase(java.util.Locale.ROOT))) {
            toast("This wallpaper link type is unsupported"); return;
        }
        open(new Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE));
    }
    private void cancelWallpaperPreview() {
        if (wallpaperDraft == null) return;
        wallpaperDraft = null; wallpaperProvider = null; wallpaperReady = false; wallpaperPreviewStarted = false;
        syncBackground();
    }
    private void startAdjustment(String title, int original, java.util.function.IntConsumer preview,
                                 java.util.function.IntConsumer commit) {
        dismissMenu(); activeMenu = null; menuGeneration++;
        adjustmentTitle = title; adjustmentValue = original; adjustmentPreview = preview; adjustmentCommit = commit;
        adjustmentCancel = () -> { preview.accept(original); };
        adjustmentArm = arm -> {
            adjustmentValue = "UI opacity".equals(title) ? WallpaperSettings.opacityStep(adjustmentValue, arm)
                    : arm == 0 ? WallpaperSettings.percent(adjustmentValue + 5) : arm == 1 ? 100
                    : arm == 2 ? WallpaperSettings.percent(adjustmentValue - 5) : 35;
            adjustmentPreview.accept(adjustmentValue); drawAdjustment();
        };
        adjustmentDone = () -> {
            int value = adjustmentValue; java.util.function.IntConsumer done = adjustmentCommit;
            clearAdjustment(); dismissMenu(); done.accept(value);
        };
        drawAdjustment();
    }
    private void drawAdjustment() {
        if (overlay != null) root.removeView(overlay);
        surface.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        overlay = new FrameLayout(this); overlay.setFocusable(true);
        // Keep the live background visible; the pie itself is always at the fixed menu opacity.
        PieMenuView pie = new PieMenuView(this, paletteColors);
        pie.setContent(adjustmentTitle,
                ("UI opacity".equals(adjustmentTitle) ? "Backgrounds · " : "") + adjustmentValue + "%", "Done", "Back cancels",
                new String[]{"+5%", "100%", "−5%", "Reset " + ("UI opacity".equals(adjustmentTitle) ? "75%" : "35%")},
                new MenuIconView.Kind[]{MenuIconView.Kind.ADD, MenuIconView.Kind.PALETTE, MenuIconView.Kind.DELETE, MenuIconView.Kind.UNDO}, new boolean[4],
                new PieMenuView.Listener() {
                    @Override public void onArm(int arm) { if (adjustmentArm != null) adjustmentArm.accept(arm); }
                    @Override public void onCenter() { if (adjustmentDone != null) adjustmentDone.run(); }
                });
        int diameter = Math.min(dp(760), Math.min(getResources().getDisplayMetrics().widthPixels - dp(56), getResources().getDisplayMetrics().heightPixels - dp(40)));
        overlay.addView(pie, new FrameLayout.LayoutParams(diameter, diameter, Gravity.CENTER));
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1)); pie.requestFocus();
    }
    private void clearAdjustment() { adjustmentArm = null; adjustmentDone = null; adjustmentCancel = null; adjustmentPreview = null; adjustmentCommit = null; }
    private void backAdjustment() {
        String title = adjustmentTitle;
        cancelAdjustment();
        if ("UI opacity".equals(title)) appearanceSettings();
        else if (wallpaperProvider != null) providerDetails(wallpaperProvider, true);
    }
    private void cancelAdjustment() {
        Runnable cancel = adjustmentCancel; clearAdjustment(); dismissMenu(); if (cancel != null) cancel.run();
    }
}
