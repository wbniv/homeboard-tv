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
import android.os.SystemClock;
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
public class MainActivity extends NativeDashboardActivity {
    private static final float APP_FOCUS_SCALE = AppFocusHighlight.SCALE;
    static final String ACTION_LAUNCHER_SETTINGS = "com.gathin.tvhome.action.LAUNCHER_SETTINGS";
    static final String ACTION_APP_LIBRARY = "com.gathin.tvhome.action.APP_LIBRARY";
    /** The remote's screensaver action for an app with its own preview screen: Home starts it, as Home idle does. */
    static final String ACTION_PLAY_SCREENSAVER = "com.gathin.tvhome.action.PLAY_SCREENSAVER";
    static final String ACTION_VIEW_INPUTS = "com.android.tv.action.VIEW_INPUTS";
    static int COLUMNS = 4;
    private static final String SETTINGS = "control:settings";
    private static final String HOME_SETTINGS = "control:homeboard-settings";
    /** Start screensaver, left of Homeboard settings on the top row (docs/plans/2026-11-26-top-row-start-screensaver.md). */
    private static final String START_SCREENSAVER = "control:start-screensaver";
    private static final int REQUEST_HOME_ROLE = 7402;
    private static final int MENU_PAGE_SIZE = 4;
    private static final int MAX_UNDO_ENTRIES = 20;
    private static final String PREF_UNDO_HISTORY = "undo_history";
    private static final class MenuSpec {
        final String title, subtitle;
        String explanation="";
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
    private int uiBackgroundOpacity = WallpaperSettings.DEFAULT_WIDGET_OPACITY;
    private WallpaperSurface wallpaperSurface;
    private ProjectivyWallpaperHost wallpaperHost;
    private WallpaperSettings wallpaperDraft;
    private ProjectivyProviderRegistry.Provider wallpaperProvider;
    private tv.projectivy.plugin.wallpaperprovider.api.Wallpaper displayedWallpaper;
    private boolean wallpaperReady, wallpaperPreviewStarted, returningFromProviderSettings;
    private boolean backgroundInfo;
    private String wallpaperStatus = "";
    private java.util.function.IntConsumer adjustmentArm;
    private Runnable adjustmentDone, adjustmentCancel;
    private int adjustmentValue;
    private String adjustmentTitle;
    private java.util.function.IntConsumer adjustmentPreview, adjustmentCommit;
    private AppFocusHighlight appHighlight;
    private boolean restoringSurface = true;
    private final List<MenuSpec> menuStack = new ArrayList<>();
    private MenuSpec activeMenu;
    private boolean invokingMenuAction;
    private int menuGeneration;
    private TextView footer;
    private FrameLayout undoBanner;
    private TextView undoButton,undoHint;
    private android.widget.ProgressBar undoProgress;
    private final BackUndoGesture backUndo=new BackUndoGesture();
    private String backUndoTarget="";
    private boolean backUndoProgressShown,backUndoFromButton,backUndoIgnoreRelease;
    private TextView aboutBackButton;
    private TextView backgroundStatusText;
    private SharedPreferences preferences;
    private HomeLayout layout;
    private String lastSavedLayout;
    private String undoGroupStart;
    private String undoGroupLabel;
    private boolean applyingUndo;
    private GridSelector gridSelector;
    private CalendarSnapshot calendarSnapshot;
    private CalendarData calendarData;
    private final Runnable calendarUpdated = this::refreshCalendarViews;
    protected CalendarData createCalendarData() { return CalendarData.get(this); }
    CalendarData calendarData() { return calendarData; }
    private void refreshCalendarViews() { for (CalendarWidgetView view : calendarWidgets.values()) view.refresh(); }
    private WidgetExpansion expandedWidget;
    private AndroidWidgetHostController androidWidgets;
    private boolean layoutReadOnly;
    private boolean calendarDayEditor;
    private final Map<String, CalendarWidgetView> calendarWidgets = new LinkedHashMap<>();
    private final Map<String, RainWidgetController> rainWidgets = new LinkedHashMap<>();
    private final Map<String,WorldClockWidgetView> clockWidgets=new LinkedHashMap<>();
    private WorldClockEditor clockEditor;
    private GroceryEditor groceryEditor;
    private final Map<String, AirQualityCard> airWidgets = new LinkedHashMap<>();
    private final List<Consumer<Location>> pendingLocationRequests = new ArrayList<>();
    private final List<Consumer<Location>> locationRequests = new ArrayList<>();
    private AirLocationLookup activeLocationLookup;
    private Location cachedLocation;
    private long cachedLocationAt;
    private static final int REQUEST_AIR_LOCATION = 7403;
    private boolean allApps, active, closed, loading, moving, moveInApps, okHeld, consumeOkUp;
    private boolean configReducedMotion;
    private int homePage, appsPage, loadVersion;
    private String focused = HomeLayout.AIR, homeFocus = HomeLayout.AIR, appsFocus = "";
    private FavoritesWidgetView favoritesWidget, favoritesBrowser;
    private String favoriteFocus="";
    private List<String> favoriteOrderPreview;
    private String favoriteMovingKey;
    private HomeLayout firstPageBefore;
    private int layoutPreviewPage;
    private String layoutPreviewFocus;
    private String layoutPreviewHomeFocus;
    private boolean layoutPreviewApps;
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
            if (active && screensaverChooserShowing()) refreshScreensaverChooser();
            if(active && activeMenu!=null && activeMenu.panelRenderer!=null &&
                    (activeMenu.title.equals(getString(R.string.hb_get_providers)) ||
                     ProviderCatalog.ENTRIES.stream().anyMatch(e->e.name.equals(activeMenu.title))))
                activeMenu.panelRenderer.run();
        }
    };
    private final Runnable heldOk = () -> {
        okHeld = false;
        consumeOkUp = true;
        if (moving) return;
        if (!allApps && layout.widgets.isEmpty()) homeSettings();
        else contextMenu(focused);
    };

    @Override protected void attachBaseContext(Context base) {
        super.attachBaseContext(AppLanguage.wrap(base));
    }
    private boolean reopenLanguageSettings;
    private void languageSettings() {
        String current = AppLanguage.mode(this);
        menu(getString(R.string.hb_language), current.isEmpty() ? getString(R.string.hb_follow_device) : "th".equals(current) ? getString(R.string.hb_thai) : getString(R.string.hb_english),
                new String[]{getString(R.string.hb_english), getString(R.string.hb_thai), getString(R.string.hb_follow_device), getString(R.string.hb_back)}, choice -> {
            if (choice == 3) { homeSettings(); return; }
            String mode = new String[]{"en", "th", ""}[choice];
            if (mode.equals(current)) { homeSettings(); return; }
            if (!AppLanguage.set(this, mode)) { toast(getString(R.string.hb_language_failed)); return; }
            pushUndo("language", "", current, getString(R.string.hb_language));
            reopenLanguageSettings = true;
            recreate();
        }, "en".equals(current) ? 0 : "th".equals(current) ? 1 : 2);
    }
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        KitchenTimerStore.reconcile(this);
        HomeKeyService.stateListener = this::homeButtonHelperChanged;
        backUndoIgnoreRelease=savedInstanceState!=null&&savedInstanceState.getBoolean("ignoreBackRelease",false);
        wallpaperStatus = getString(R.string.hb_choose_preview_to_load_this_provider);
        preferences = getSharedPreferences("widget-home", MODE_PRIVATE);
        String encoded = preferences.getString("layout", null);
        boolean layoutReadable=true;
        try { layout = encoded == null ? HomeLayout.firstRun() : HomeLayout.parse(encoded); }
        catch (Exception e) {
            layoutReadable=false;layoutReadOnly=e instanceof HomeLayout.NewerSchema;
            // Preserve an unreadable user's layout for diagnosis; never replace it silently with defaults.
            preferences.edit().putString("unreadable-layout", encoded).apply();
            layout = new HomeLayout();
            Toast.makeText(this, getString(layoutReadOnly?R.string.hb_aw_newer_layout:R.string.hb_saved_layout_could_not_be_read_starting_with_empty_home), Toast.LENGTH_LONG).show();
        }
        if (encoded == null) layout.setDashboardScale(DashboardScale.defaultRendererPercent(native4kWorkspace()), 8, 4);
        if ("theme".equals(layout.palette)) {
            // Move former theme defaults to a deterministic palette; retain any saved image data.
            layout.palette = "midnight";
            saveWithoutUndo();
        }
        if (encoded == null) {
            layout.arrangeDefaultDashboard(HomeLayout.RAIN, HomeLayout.AIR, homeGridColumns(), homeGridRows());
            saveWithoutUndo();
        }
        if(layoutReadable&&encoded!=null&&!encoded.equals(layout.encode()))saveWithoutUndo();
        lastSavedLayout = layout.encode();
        androidWidgets=new AndroidWidgetHostController(this,new AndroidWidgetHostController.Ui(){
            public HomeLayout layout(){return layout;}
            public int columns(){return homeGridColumns();} public int rows(){return homeGridRows();}
            public int gridWidth(){return androidGridWidth();}public int gridHeight(){return androidGridHeight();}
            public void menu(String title,String detail,String[] choices,java.util.function.IntConsumer selected){MenuSpec menu=new MenuSpec(title,detail,choices,selected,-1);menu.explanation=detail;openMenu(menu);}
            public void added(String key,AndroidWidgetSpec spec,int width,int height){addAndroidWidget(key,spec,width,height);}
            public void redraw(){render(0);}public boolean busy(){return overlay!=null||expandedWidget!=null;}
        });
        calendarData=createCalendarData();calendarSnapshot=calendarData.snapshot;
        Config config = Config.load(this);
        configReducedMotion = config.reducedMotion;
        background = paletteColor();
        paletteColors = PaletteColors.from(background);
        root = new FrameLayout(this);
        root.setBackgroundColor(background);
        wallpaperSurface = new WallpaperSurface(this);
        root.addView(wallpaperSurface, new FrameLayout.LayoutParams(-1, -1));
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
        androidx.core.content.ContextCompat.registerReceiver(this, screensaverAlarm, new IntentFilter(ScreensaverActivity.ACTION_END),
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);
        boolean repairedDashboard = false;
        if (!preferences.getBoolean("single-screen-default-v1",false)) {
            if(encoded!=null&&!layout.widgets.isEmpty()) {
                applyDefaultDashboard(firstDashboardInstance(true),firstDashboardInstance(false),false);
                repairedDashboard=true;
            }
            preferences.edit().putBoolean("single-screen-default-v1",true).apply();
        }
        if (encoded == null) { allApps=false;homePage=0;focused=HomeLayout.RAIN;homeFocus=focused; }
        favoriteFocus=preferences.getString("favorite-focus", "");
        render(0);
        if(repairedDashboard)save(getString(R.string.hb_dashboard_applied));
        loadApps();
        handleIntent(getIntent());
        Bundle picker = savedInstanceState == null ? null : savedInstanceState.getBundle("screensaverPicker");
        if (picker != null) {
            // Recreated with the chooser open or about to return: onResume reopens it on the same entry and position.
            screensaverPicker.browsed = picker.getString("browsed"); screensaverPicker.browsedIndex = picker.getInt("browsedIndex");
            screensaverPicker.anchor = picker.getString("anchor"); screensaverPicker.anchorOffset = picker.getInt("anchorOffset");
            screensaverPicker.action = picker.getString("action"); screensaverPicker.fromHomeSettings = picker.getBoolean("fromHomeSettings");
            screensaverOptionsOpen = picker.getBoolean("options");
            returningToScreensaver = screensaverPicker.browsed;
        }
        if (savedInstanceState != null && savedInstanceState.getBoolean("languageSettings")) {
            homeSettings();
            showUndoBanner(getString(R.string.hb_language));
        }
        if(savedInstanceState!=null&&savedInstanceState.getBundle("expandedWidget")!=null){
            Bundle expanded=savedInstanceState.getBundle("expandedWidget");
            root.post(()->{String key=expanded.getString("key","");if(HomeLayout.HEADER_CLOCK.equals(key)||layout.widgets.contains(key))expandWidget(key,expanded);});
        }
    }

    @Override protected void onResume() {
        super.onResume(); active = true;
        root.post(()->{if(active&&androidWidgets!=null&&!layoutReadOnly)androidWidgets.resume();});
        calendarData.listen(calendarUpdated);calendarData.schedule();calendarData.refresh(false,null);
        if(expandedWidget!=null)expandedWidget.setRunning(true);
        for (RainWidgetController widget : rainWidgets.values()) widget.resumeDetails();
        updateAir();
        wallpaperSurface.visible(true); wallpaperHost.visible(true);
        if (returningFromProviderSettings && wallpaperProvider != null) {
            returningFromProviderSettings = false;
            wallpaperPreviewStarted = true; syncBackground(); wallpaperHost.refresh();
            handler.post(() -> providerDetails(wallpaperProvider, false));
        }
        checkChosenScreensaverApp();
        lastHomeInput = SystemClock.uptimeMillis(); scheduleScreensaver();
        if (timersAfterAppScreen) { timersAfterAppScreen = false; returningToScreensaver = null; handler.post(() -> expandWidget(HomeLayout.TIMER, null)); }
        if (returningToScreensaver != null) { String entry = returningToScreensaver; returningToScreensaver = null; handler.post(() -> reopenScreensaver(entry)); }
        screensaverPausedPicker = null;
        resumeHomeButton();
        if(returningToProvider!=null) {
            ProviderCatalog.Entry entry=returningToProvider;returningToProvider=null;
            handler.post(()->{providerDirectory();openMenu(new MenuSpec(entry.name,()->drawProviderEntry(entry)));});
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
        if(androidWidgets!=null)androidWidgets.pause();
        calendarData.unlisten(calendarUpdated);
        cancelBackUndo(false);
        if(phoneEditor!=null)phoneEditor.stop();
        active = false; restoringSurface = true; cancelOk();
        if(expandedWidget!=null)expandedWidget.setRunning(false);
        if (firstPageBefore != null) finishLayoutPreview(false);
        if (adjustmentCancel != null) cancelAdjustment();
        if (!returningFromProviderSettings) cancelWallpaperPreview();
        wallpaperHost.visible(false); wallpaperSurface.visible(false);
        if (activeMenu != null && activeMenu.paletteGrid) finishUndoGroup();
        // Kept only for a recreation (onSaveInstanceState); a plain resume does not reopen the chooser.
        if (screensaverChooserShowing()) { captureScreensaverAnchor(); screensaverPausedPicker = screensaverPicker.copy(); }
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
        preferences.edit().putString("favorite-focus", favoriteFocus).apply();
        saveNavigationState();
        for (AirQualityCard widget : airWidgets.values()) widget.stop();
        for (RainWidgetController widget : rainWidgets.values()) widget.pause();
        if (activeLocationLookup != null) activeLocationLookup.finish(null);
        for (CalendarWidgetView widget : calendarWidgets.values()) widget.stop();
        for(WorldClockWidgetView widget:clockWidgets.values())widget.stop();
        handler.removeCallbacks(screensaverDue);
        super.onPause();
    }
    @Override protected void onDestroy() {
        if (HomeKeyService.stateListener != null) HomeKeyService.stateListener = null;
        handler.removeCallbacks(homeButtonGiveUp);
        if(androidWidgets!=null)androidWidgets.close();
        if(groceryEditor!=null){groceryEditor.setOnDismissListener(null);groceryEditor.dismiss();groceryEditor=null;}
        if(clockEditor!=null){clockEditor.setOnDismissListener(null);clockEditor.dismiss();clockEditor=null;}
        for(WorldClockWidgetView widget:clockWidgets.values())widget.stop();
        cancelBackUndo(false);
        closed = true; loadVersion++;
        if(expandedWidget!=null){expandedWidget.release();expandedWidget=null;}
        wallpaperHost.close(); wallpaperSurface.close();
        unregisterReceiver(packages);
        unregisterReceiver(screensaverAlarm);
        handler.removeCallbacksAndMessages(null);
        loader.shutdownNow(); for (AirQualityCard widget : airWidgets.values()) widget.close();
        for (RainWidgetController widget : rainWidgets.values()) widget.close();
        if (activeLocationLookup != null) activeLocationLookup.finish(null);
        pendingLocationRequests.clear(); locationRequests.clear();
        super.onDestroy();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("ignoreBackRelease",backUndoIgnoreRelease||backUndo.down());
        if(expandedWidget!=null){Bundle expanded=expandedWidget.state();expanded.putBoolean("originApps",allApps);state.putBundle("expandedWidget",expanded);}
        state.putBoolean("languageSettings", reopenLanguageSettings);
        // The chooser, if a return to it is pending or it was showing at the pause: reopened after a recreation.
        ScreensaverPicker.State picker = returningToScreensaver != null ? screensaverPicker : screensaverPausedPicker;
        if (picker != null && picker.browsed != null) {
            Bundle saved = new Bundle();
            saved.putString("browsed", picker.browsed); saved.putInt("browsedIndex", picker.browsedIndex);
            saved.putString("anchor", picker.anchor); saved.putInt("anchorOffset", picker.anchorOffset);
            saved.putString("action", picker.action); saved.putBoolean("fromHomeSettings", picker.fromHomeSettings);
            saved.putBoolean("options", returningToScreensaver != null && screensaverOptionsOpen);
            state.putBundle("screensaverPicker", saved);
        }
        state.putBoolean("allApps", allApps); state.putInt("homePage", homePage);
        state.putInt("appsPage", appsPage); state.putString("focus", focused);
        state.putString("homeFocus", homeFocus); state.putString("appsFocus", appsFocus);
        super.onSaveInstanceState(state);
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(androidWidgets!=null&&androidWidgets.result(requestCode,resultCode,data))return;
        boolean appScreen = appScreenRequest != 0 && requestCode == appScreenRequest;
        if (appScreen) appScreenEnded(timersAfterAppScreen ? "alarm" : "left");
        if (requestCode == REQUEST_APP_SCREENSAVER) return;
        if (requestCode == REQUEST_SCREENSAVER_PREVIEW) {
            // Preview: OK uses the entry (saved with Undo once the screen is back), Back saved nothing. An app's own
            // preview screen never chooses anything here, whatever it returns.
            screensaverAccepted = !appScreen && resultCode == RESULT_OK && data != null ? data.getStringExtra(ScreensaverActivity.EXTRA_ENTRY) : null;
            return;
        }
        if (requestCode == REQUEST_HOME_ROLE) {
            // The Home button screen comes back on resume (returningToHomeButton) and shows what Android now resolves.
            toast(getString(isDefaultHomeApp() ? R.string.hb_hb_role_done : R.string.hb_hb_role_unchanged));
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
        focused = HomeLayout.HEADER_CLOCK.equals(homeFocus)||layout.widgets.contains(homeFocus) ? homeFocus
                : (layout.widgets.isEmpty() ? "" : layout.widgets.get(0));
        homePage = focused.isEmpty() ? 0 : homePageOf(focused, layout.widgets);
        homeFocus = focused;
        saveNavigationState();
        render(-1); handleIntent(intent);
    }
    private void handleIntent(Intent intent) {
        if (intent == null) return;
        if (ScreensaverActivity.ACTION_OPEN_TIMERS.equals(intent.getAction())) { expandWidget(HomeLayout.TIMER,null); return; }
        if (ACTION_PLAY_SCREENSAVER.equals(intent.getAction())) {
            setIntent(new Intent());   // played once, not again when the activity is recreated
            try { playScreensaver(ScreensaverActivity.MODE_NOW); }
            catch (RuntimeException unavailable) { toast(getString(R.string.hb_android_could_not_open_that_action)); }
            return;
        }
        if (ACTION_APP_LIBRARY.equals(intent.getAction())) showApps();
        else if (ACTION_LAUNCHER_SETTINGS.equals(intent.getAction())) homeSettings();
        else if (ACTION_VIEW_INPUTS.equals(intent.getAction())) open(new Intent(ACTION_VIEW_INPUTS));
    }
    private void save() { save(getString(R.string.hb_hint_home_layout)); }
    private void save(String label) {
        if(layoutReadOnly){toast(getString(R.string.hb_aw_newer_layout));return;}
        String next=layout.encode();boolean changed=lastSavedLayout!=null&&!lastSavedLayout.equals(next);
        if(!changed)return;
        try{
            JSONArray undo=storedUndoEntries();
            if(!applyingUndo&&undoGroupStart==null)undo=appendedUndo(undo,"layout","",lastSavedLayout,label);
            PhoneConfigurationStore.commit(preferences,layout,PhoneConfigurationStore.air(preferences),undo,null,null);
            lastSavedLayout=next;
            if(androidWidgets!=null)androidWidgets.reconcile();
            if(!applyingUndo&&undoGroupStart==null)showUndoBanner(label);
        }catch(Exception failed){try{layout=HomeLayout.parse(lastSavedLayout);}catch(Exception ignored){}toast(getString(R.string.hb_configuration_save_failed));}
    }
    private JSONArray appendedUndo(JSONArray old,String kind,String key,String state,String label)throws org.json.JSONException {
        JSONArray next=new JSONArray();for(int i=Math.max(0,old.length()-MAX_UNDO_ENTRIES+1);i<old.length();i++)next.put(old.getJSONObject(i));
        return next.put(new JSONObject().put("kind",kind).put("key",key).put("state",state).put("label",label));
    }
    private int androidGridWidth(){float density=getResources().getDisplayMetrics().density;return Math.max(1,(int)((surface!=null&&surface.getWidth()>0?surface.getWidth():dashboardWidthPixels())/density)-AppGridSizing.SIDE_MARGIN*2)*100/dashboardScale();}
    private int androidGridHeight(){float density=getResources().getDisplayMetrics().density;return Math.max(1,(int)((surface!=null&&surface.getHeight()>0?surface.getHeight():dashboardHeightPixels())/density)-AppGridSizing.HOME_TOP-AppGridSizing.BOTTOM_MARGIN)*100/dashboardScale();}
    private void addAndroidWidget(String key,AndroidWidgetSpec spec,int width,int height){
        if(layoutReadOnly)return;
        try{
            HomeLayout next=HomeLayout.parse(layout.encode());next.androidWidgets.put(key,spec.json());next.addPanel(key);if(!layout.widgets.contains(key))next.setPanelSpan(key,width,height);
            JSONArray undo=appendedUndo(storedUndoEntries(),"layout","",layout.encode(),getString(R.string.hb_aw_title));
            PhoneConfigurationStore.commit(preferences,next,PhoneConfigurationStore.air(preferences),undo,null,null,true);
            layout=next;lastSavedLayout=next.encode();androidWidgets.reconcile();allApps=false;focused=homeFocus=key;homePage=homePageOf(key,layout.widgets);render(0);showUndoBanner(getString(R.string.hb_aw_title));
        }catch(Exception failed){androidWidgets.notice(getString(R.string.hb_aw_failed));}
    }
    int androidWidgetHostId(){return AndroidWidgetHostController.HOST_ID;}
    void beginAndroidWidgetTest(ComponentName component){androidWidgets.beginProvider(component);}
    private void saveWithoutUndo() {
        if(layoutReadOnly)return;
        String next=layout.encode();SharedPreferences.Editor edit=PhoneConfigurationStore.trackInstances(preferences,preferences.edit(),layout).putString("layout",next);
        if(!next.equals(preferences.getString("layout","")))PhoneConfigurationStore.revision(preferences,edit);
        edit.apply();lastSavedLayout=next;
    }
    private JSONArray storedUndoEntries() {
        try { return new JSONArray(preferences.getString(PREF_UNDO_HISTORY, "[]")); }
        catch (Exception invalid) { throw new IllegalStateException("Undo history unreadable",invalid); }
    }
    private void pushUndo(String kind, String key, String state, String label) {
        JSONArray old = storedUndoEntries();
        JSONArray next = new JSONArray();
        try {
            for (int i = Math.max(0, old.length() - MAX_UNDO_ENTRIES + 1); i < old.length(); i++)
                next.put(old.getJSONObject(i));
            next.put(new JSONObject().put("kind", kind).put("key", key)
                    .put("state", state).put("label", label));
            if(preferences.edit().putString(PREF_UNDO_HISTORY, next.toString()).commit()&&androidWidgets!=null)androidWidgets.reconcile();
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
    private void showUndoBanner(String label){showUndoBanner(label,true);}
    private void showUndoBanner(String label,boolean offerUndo) {
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
        offerUndo=offerUndo&&!readUndoHistory().isEmpty();
        LinearLayout copy=new LinearLayout(this);copy.setOrientation(LinearLayout.VERTICAL);copy.setGravity(Gravity.CENTER_VERTICAL);
        TextView message = text(label, 17);
        message.setSingleLine(true); message.setEllipsize(android.text.TextUtils.TruncateAt.END);
        copy.addView(message,new LinearLayout.LayoutParams(-1,-2));
        undoHint=text(offerUndo?getString(R.string.hb_undo_hold_hint):readUndoHistory().isEmpty()?"":getString(R.string.hb_undo_hold_target,readUndoHistory().get(0).label),12);undoHint.setMaxLines(2);
        undoHint.setVisibility(undoHint.getText().length()>0?View.VISIBLE:View.GONE);copy.addView(undoHint,new LinearLayout.LayoutParams(-1,-2));
        undoProgress=new android.widget.ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);undoProgress.setMax(100);undoProgress.setVisibility(View.GONE);
        copy.addView(undoProgress,new LinearLayout.LayoutParams(-1,dp(5)));
        row.addView(copy,new LinearLayout.LayoutParams(0,-2,1f));
        undoButton=null;
        if(offerUndo){
            undoButton = text(getString(R.string.hb_undo), 17); undoButton.setTypeface(null, Typeface.BOLD);
            undoButton.setGravity(Gravity.CENTER);undoButton.setFocusable(true);undoButton.setClickable(true);
            undoButton.setPadding(dp(20),0,dp(20),0);undoButton.setContentDescription(getString(R.string.hb_undo_5)+readUndoHistory().get(0).label);
            undoButton.setBackground(tileBackground());
            undoButton.setOnFocusChangeListener((v,has)->{handler.removeCallbacks(hideUndoBanner);updateUndoHint();if(!has)handler.postDelayed(hideUndoBanner,20_000L);});
            undoButton.setOnClickListener(v->{if(backUndoAllowed())undoThrough(0);});
            row.addView(undoButton,new LinearLayout.LayoutParams(-2,dp(48)));
        }
        row.setMinimumHeight(dp(66));
        undoBanner.addView(row, new FrameLayout.LayoutParams(-1, -1));
        int width = Math.min(dp(820), dashboardWidthPixels() - dp(80));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(width, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        lp.bottomMargin = dp(28);
        root.addView(undoBanner, lp);
        if (overlay != null) overlay.bringToFront(); else undoBanner.bringToFront();
        handler.postDelayed(hideUndoBanner, 20_000L);updateUndoHint();
    }
    private void updateUndoHint(){
        if(undoHint==null||undoButton==null||backUndoProgressShown)return;
        undoHint.setText(getString(!backUndoAllowed()?R.string.hb_undo_close_editor:undoButton.hasFocus()?R.string.hb_undo_ok_hint:R.string.hb_undo_hold_hint));
    }
    private final Runnable hideUndoBanner = () -> {
        if(undoButton!=null&&undoButton.hasFocus())return;
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
            entries.add(new UndoEntry(item.optString("label", getString(R.string.hb_homeboard_change)),
                    item.optString("kind", "layout"), item.optString("key", ""), item.optString("state", "")));
        } catch (Exception ignored) { }
        return entries;
    }
    private void openUndoHistory() {
        List<UndoEntry> entries = readUndoHistory();
        if (entries.isEmpty()) {
            menu(getString(R.string.hb_undo_history), getString(R.string.hb_no_recent_homeboard_changes_can_be_undone), new String[0], ignored -> {});
            return;
        }
        String[] labels = new String[entries.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = getString(R.string.hb_undo_9) + entries.get(i).label;
        menu(getString(R.string.hb_undo_history), getString(R.string.hb_newest_first_undoing_an_older_change_also_reverses_newer_changes),
                labels, this::undoThrough);
    }
    private void undoThrough(int displayedIndex) {
        if(layoutReadOnly){toast(getString(R.string.hb_aw_newer_layout));return;}
        List<UndoEntry> entries = readUndoHistory();
        if (displayedIndex < 0 || displayedIndex >= entries.size()) return;
        int targetIndex = entries.size() - 1 - displayedIndex;
        String restoredLabel = entries.get(displayedIndex).label;
        HomeLayout beforeUndo=layout;
        // A change to Android's screensaver is undone by writing back the previous values its entry carries: the
        // oldest such entry in the range wins, written once, read back, before anything is committed.
        ScreensaverValues systemTarget = null; boolean systemManaged = false;
        EverywhereScreensaver.Outcome systemUndone = null; boolean managedBeforeUndo = everywhere().managed();
        // A Home button change is undone by setting the choice the oldest entry in the range carries.
        Boolean homeButtonTarget = null;
        try {
            JSONObject restoredAir=PhoneConfigurationStore.air(preferences);
            applyingUndo = true;
            for (int i = entries.size() - 1; i >= targetIndex; i--) {
                UndoEntry entry = entries.get(entries.size() - 1 - i);
                if ("language".equals(entry.kind)) {
                    if (!AppLanguage.set(this, entry.state)) throw new IllegalStateException("Language save failed");
                    reopenLanguageSettings = true;
                }
                else if ("layout".equals(entry.kind)) layout = HomeLayout.restore(entry.state,layout.headerClock);
                else if ("home-app-size".equals(entry.kind)) {
                    JSONObject state = new JSONObject(entry.state);
                    layout = HomeLayout.restore(state.getString("layout"),layout.headerClock);
                    homePage = state.getInt("page"); homeFocus = state.getString("focus");
                    if (!allApps) focused = homeFocus;
                }
                else if ("air".equals(entry.kind)) restoredAir.put(entry.key,new JSONObject(entry.state));
                else if (UNDO_SYSTEM_SCREENSAVER.equals(entry.kind)) {
                    JSONObject state = new JSONObject(entry.state);
                    layout = HomeLayout.restore(state.getString("layout"),layout.headerClock);
                    systemTarget = ScreensaverValues.parse(state.optJSONObject("android"));
                    systemManaged = state.optBoolean("managed", false);
                    if (systemTarget == null) throw new IllegalStateException("Screensaver undo entry unreadable");
                }
                else if ("phone".equals(entry.kind)) {JSONObject aggregate=new JSONObject(entry.state);layout=HomeLayout.restore(aggregate.getJSONObject("layout").toString(),layout.headerClock);restoredAir=aggregate.getJSONObject("air");}
                else if (UNDO_HOME_BUTTON.equals(entry.kind)) homeButtonTarget = new JSONObject(entry.state).getBoolean("wanted");
            }
            if (systemTarget != null) {
                EverywhereScreensaver.Outcome undone = everywhere().undoTo(systemTarget, systemManaged, ScreensaverRegistry.installed(refreshScreensaverEntries()));
                if (!undone.ok()) throw new IllegalStateException("Android did not accept the screensaver undo: " + undone.result);
                systemUndone = undone;
            }
            JSONArray stored = storedUndoEntries();
            JSONArray remaining = new JSONArray();
            for (int i = 0; i < targetIndex; i++) remaining.put(stored.getJSONObject(i));
            PhoneConfigurationStore.commit(preferences,layout,restoredAir,remaining,null,null);
            if (homeButtonTarget != null) HomeButton.set(this, homeButtonTarget);
            for(AirQualityCard a:airWidgets.values())a.close();airWidgets.clear();
            for(RainWidgetController r:rainWidgets.values())r.close();rainWidgets.clear();
            lastSavedLayout = layout.encode();
            undoGroupStart = null; undoGroupLabel = null;
            background = paletteColor(); paletteColors = PaletteColors.from(background);
            root.setBackgroundColor(background); setAirPalette();
        } catch (Exception failure) {
            layout=beforeUndo;
            if (systemUndone != null && systemUndone.before != null)   // the layout did not save: Android goes back too
                everywhere().undoTo(systemUndone.before, managedBeforeUndo, ScreensaverRegistry.installed(screensaverEntries));
            toast(getString(R.string.hb_could_not_undo_that_change));
            showUndoBanner(getString(R.string.hb_could_not_undo_that_change));
            return;
        } finally { applyingUndo = false; }
        handler.removeCallbacks(hideUndoBanner);
        if (undoBanner != null) root.removeView(undoBanner);
        undoBanner = null; undoButton = null;
        if(androidWidgets!=null)androidWidgets.reconcile();
        menuStack.clear(); activeMenu = null; gridSelector = null;
        if (overlay != null) { root.removeView(overlay); overlay = null; }
        syncBackground();
        render(0); saveNavigationState();
        if (reopenLanguageSettings) recreate();
        showUndoBanner(displayedIndex == 0 ? getString(R.string.hb_undid) + restoredLabel
                : getString(R.string.hb_undid) + restoredLabel + " and newer changes",false);
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
                handler.post(() -> { loading = false; if (!closed) toast(getString(R.string.hb_could_not_refresh_installed_apps)); });
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
                migrated |= layout.seedFavoritesFromApps(new ArrayList<>(apps.keySet()));
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
    private int scalePreview;
    private boolean scaleOriginalApps;
    private int scaleOriginalPage;
    private int scaleOriginalColumns, scaleOriginalRows;
    private HomeLayout scaleDraft;
    private String scaleOriginalFocus;
    private int dashboardScale() { return scalePreview == 0 ? layout.dashboardScale : scalePreview; }
    private int scaledGridCount(int count, int minimum) { return Math.max(minimum, DashboardScale.cells(count, layout.dashboardScale, dashboardScale())); }
    private int homeGridColumns() {
        return homeGridColumnsFor(layout.homeAppSize);
    }
    private int homeGridColumnsFor(String size) {
        if(layout.favoritesContainer)size=HomeLayout.APP_SMALL;
        int fit = homeGridFitColumnsFor(size);
        return layout.homeColumns == 0 ? fit : layout.favoritesContainer ? scaledGridCount(layout.homeColumns,4) : Math.min(scaledGridCount(layout.homeColumns,4), fit);
    }
    private int appLabelHeightDp() {
        android.graphics.Paint paint = new android.graphics.Paint();
        android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
        paint.setTextSize(16 * metrics.scaledDensity);
        android.graphics.Paint.FontMetricsInt font = paint.getFontMetricsInt();
        return (int) Math.ceil((font.bottom - font.top) / metrics.density);
    }
    private int homeGridFitColumns() {
        return homeGridFitColumnsFor(layout.favoritesContainer ? HomeLayout.APP_SMALL : layout.homeAppSize);
    }
    private int homeGridFitColumnsFor(String size) { return homeGridFitColumnsFor(size, dashboardScale()); }
    private int homeGridFitColumnsFor(String size, int scale) {
        int screenWidthDp = Math.round(dashboardWidthPixels()
                / getResources().getDisplayMetrics().density);
        int fit = AppGridSizing.homeColumns(screenWidthDp - AppGridSizing.SIDE_MARGIN * 2,
                appLabelHeightDp(), HomeLayout.APP_BANNER.equals(size));
        return DashboardScale.cells(layout.favoritesContainer ? Math.max(8,fit) : fit, 100, scale);
    }
    private int homeGridFitRows() {
        return homeGridFitRowsFor(layout.homeAppSize);
    }
    private int homeGridFitRowsFor(String size) { return homeGridFitRowsFor(size, dashboardScale()); }
    private int homeGridFitRowsFor(String size, int scale) {
        if(layout.favoritesContainer)size=HomeLayout.APP_SMALL;
        int screenHeightDp = Math.round(dashboardHeightPixels()
                / getResources().getDisplayMetrics().density);
        return DashboardScale.cells(Math.max(layout.favoritesContainer ? 4 : 2, AppGridSizing.rows(screenHeightDp - AppGridSizing.HOME_TOP
                - AppGridSizing.BOTTOM_MARGIN, appLabelHeightDp(), HomeLayout.APP_BANNER.equals(size))), 100, scale);
    }
    private int homeGridRows() {
        return homeGridRowsFor(layout.homeAppSize);
    }
    private int homeGridRowsFor(String size) {
        if(layout.favoritesContainer)size=HomeLayout.APP_SMALL;
        int fit = homeGridFitRowsFor(size);
        return layout.homeRows == 0 ? fit : layout.favoritesContainer ? scaledGridCount(layout.homeRows,2) : Math.min(scaledGridCount(layout.homeRows,2), fit);
    }
    private List<HomeLayout.Cell> homeCells(List<String> items) {
        Map<String, HomeLayout.HomePosition> positions = moving && !moveInApps
                ? previewHomePositions : layout.homePositions;
        return (scalePreview != 0 && scaleDraft != null ? scaleDraft : layout).cells(items, homeGridColumns(), homeGridRows(), positions);
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
        if(androidWidgets!=null)androidWidgets.beginRender();
        String requestedFocus = focused;
        FrameLayout old = surface;
        if (appHighlight != null) appHighlight.cancel();
        appHighlight = null;
        if (old != null) { old.animate().cancel(); root.removeView(old); }
        for (AirQualityCard widget : airWidgets.values()) widget.unbind();
        for (RainWidgetController widget : rainWidgets.values()) widget.unbind();
        for (CalendarWidgetView widget : calendarWidgets.values()) widget.stop();
        calendarWidgets.clear();
        for(WorldClockWidgetView widget:clockWidgets.values())widget.stop();clockWidgets.clear();
        favoritesWidget=null;
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
        if(androidWidgets!=null)androidWidgets.endRender();
        applyUiBackgroundOpacity(uiBackgroundOpacity);
        updateAir();
        focused = requestedFocus;
        if (overlay == null) restoreFocus();
        if (direction != 0 && !reducedMotion()) {
            // Both surfaces stay in the same window. Outgoing content remains visible during the slide.
            int width = dashboardWidthPixels();
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
        updateUndoHint();
        if(phonePreviewBefore!=null){for(AirQualityCard a:airWidgets.values())a.stop();for(RainWidgetController r:rainWidgets.values())r.stop();for(WorldClockWidgetView w:clockWidgets.values())w.stop();return;}
        boolean visible = active && !allApps && overlay == null && hasWindowFocus();
        Set<String> onPage = new LinkedHashSet<>();
        for (HomeLayout.Cell c : homeCells(dashboardItems()))
            if (isAirWidgetKey(c.key) && c.page == homePage) onPage.add(c.key);
        for (CalendarWidgetView widget : calendarWidgets.values()) {
            if (visible) widget.start(); else widget.stop();
        }
        for(java.util.Map.Entry<String,WorldClockWidgetView> entry:clockWidgets.entrySet()){boolean clockVisible=HomeLayout.HEADER_CLOCK.equals(entry.getKey())?active&&overlay==null&&hasWindowFocus():visible;if(clockVisible&&clockEditor==null)entry.getValue().start();else entry.getValue().stop();}
        Set<String> rainOnPage = new LinkedHashSet<>();
        for (HomeLayout.Cell c : homeCells(dashboardItems())) if (HomeLayout.isRainWidget(c.key) && c.page == homePage) rainOnPage.add(c.key);
        for (Map.Entry<String, RainWidgetController> entry : rainWidgets.entrySet()) {
            if (visible && rainOnPage.contains(entry.getKey())) entry.getValue().start(); else entry.getValue().stop();
        }
        for (Map.Entry<String, AirQualityCard> entry : airWidgets.entrySet()) {
            if (visible && onPage.contains(entry.getKey())) entry.getValue().start();
            else entry.getValue().stop();
        }
    }
    private RainWidgetController rainFor(String key) {
        RainWidgetController widget = rainWidgets.get(key);
        if (widget == null) {
            widget = new RainWidgetController(this, key,
                    () -> layout.rainSettings.getOrDefault(key, new RainSettings()),
                    next -> { layout.rainSettings.put(key, next); save(getString(R.string.hb_rain_changed)); render(0); },
                    this::menu, new RainWidgetController.LocationAccess() {
                        @Override public boolean granted() { return locationGranted(); }
                        @Override public void request(boolean prompt, Consumer<Location> callback) { requestDeviceLocation(prompt, callback); }
                    }, () -> rainSize(key));
            rainWidgets.put(key, widget);
        }
        widget.style(paletteColors, uiBackgroundOpacity, reducedMotion());
        return widget;
    }
    private void rainSize(String key) {
        menu(getString(R.string.hb_rain_size), new String[]{getString(R.string.hb_rain_recommended),getString(R.string.hb_rain_compact),
                getString(R.string.hb_rain_summary),getString(R.string.hb_rain_glance),getString(R.string.hb_rain_fill),getString(R.string.hb_rain_directional)}, i -> {
            if (i == 5) { startResize(key); return; }
            int[][] spans={{4,3},{4,2},{2,2},{1,1},{homeGridColumns(),homeGridRows()}};
            startResize(key);
            resizeWidth=Math.min(homeGridColumns(),spans[i][0]);resizeHeight=Math.min(homeGridRows(),spans[i][1]);
            layout.setPanelSpan(key,resizeWidth,resizeHeight);
            homePage=homePageOf(key,layout.widgets);focused=key;render(0);
        });
    }
    private View rainTile(String key) {
        RainWidgetController controller=rainFor(key);RainWidgetView tile=new RainWidgetView(this,controller);
        tile.setStyle(paletteColors,uiBackgroundOpacity,reducedMotion());controller.bind(tile);return tile;
    }
    private View clockTile(String key) {
        WorldClockWidgetView tile=new WorldClockWidgetView(this,layout.clock(key),paletteColors,uiBackgroundOpacity);
        clockWidgets.put(key,tile);return tile;
    }
    private View groceryTile(String key){return new GroceryWidgetView(this,layout.grocery(key),paletteColors,uiBackgroundOpacity);}
    private void configureGroceries(String key){
        if(groceryEditor!=null)return;
        groceryEditor=new GroceryEditor(this,layout.groceries,layout.groceryLists.getOrDefault(key,GroceryData.DEFAULT),paletteColors,(book,id)->{
            layout.groceries=book;layout.groceryLists.put(key,id);save(getString(R.string.hb_grocery_updated));render(0);
        });
        groceryEditor.setOnDismissListener(d->{groceryEditor=null;handler.post(()->{if(active&&expandedWidget!=null&&expandedWidget.configuring())expandedWidget.resumeAfterConfiguration();});});groceryEditor.open();
    }
    private void configureClock(String key) {
        if(clockEditor!=null)return;
        clockEditor=new WorldClockEditor(this,layout.clock(key),paletteColors,next->{
            if(layout.clock(key).json().toString().equals(next.json().toString()))return;
            if(HomeLayout.HEADER_CLOCK.equals(key))layout.headerClock=next;else layout.clockSettings.put(key,next);save(getString(R.string.hb_wc_updated));render(0);
        });
        clockEditor.setOnDismissListener(d->{clockEditor=null;updateAir();handler.post(()->{if(active&&expandedWidget!=null&&expandedWidget.configuring())expandedWidget.resumeAfterConfiguration();});});
        clockEditor.open();updateAir();
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
        requestDeviceLocation(true, callback);
    }
    private boolean locationGranted() {
        boolean coarse = checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean fine = checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        return coarse || fine;
    }
    private void requestDeviceLocation(boolean prompt, Consumer<Location> callback) {
        if (closed) { callback.accept(null); return; }
        long now = System.currentTimeMillis();
        if (!locationGranted()) {
            cachedLocation = null;
            if (!prompt) { callback.accept(null); return; }
            pendingLocationRequests.add(callback);
            if (pendingLocationRequests.size() == 1)
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION,
                        android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_AIR_LOCATION);
            return;
        }
        if (cachedLocation != null && RainLocation.recent(cachedLocationAt, now, RainLocation.REFRESH)
                && validDeviceLocation(cachedLocation, now)) {
            callback.accept(new Location(cachedLocation)); return;
        }
        if (activeLocationLookup != null) { locationRequests.add(callback); return; }
        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (manager == null) { callback.accept(null); return; }
        Location best = null;
        try {
            for (String provider : manager.getProviders(true)) {
                Location location = manager.getLastKnownLocation(provider);
                if (validDeviceLocation(location, now) && (best == null || location.getTime() > best.getTime())) best = location;
            }
        } catch (SecurityException ignored) { }
        if (best != null) {
            cachedLocation = new Location(best); cachedLocationAt = now; callback.accept(new Location(best)); return;
        }
        locationRequests.add(callback);
        activeLocationLookup = new AirLocationLookup(manager);
        activeLocationLookup.start();
    }
    private boolean validDeviceLocation(Location location, long now) {
        return location != null && RainSettings.coordinates(location.getLatitude(), location.getLongitude())
                && RainLocation.recent(location.getTime(), now, RainLocation.MAX_AGE);
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
        private final Set<String> providers = new LinkedHashSet<>();
        private boolean finished;
        private final Runnable timeout = () -> finish(null);
        AirLocationLookup(LocationManager manager) {
            this.manager = manager;
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
        @Override public void onLocationChanged(Location location) {
            if (validDeviceLocation(location, System.currentTimeMillis())) finish(location);
        }
        @Override public void onProviderDisabled(String provider) { }
        @Override public void onProviderEnabled(String provider) { }
        @SuppressWarnings("deprecation") @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
        private void finish(Location location) {
            if (finished) return;
            finished = true; handler.removeCallbacks(timeout); activeLocationLookup = null;
            for (String provider : providers) try { manager.removeUpdates(this); break; } catch (SecurityException ignored) { }
            if (!locationGranted()) location = null;
            if (location != null) { cachedLocation = new Location(location); cachedLocationAt = System.currentTimeMillis(); }
            List<Consumer<Location>> callbacks = new ArrayList<>(locationRequests); locationRequests.clear();
            for (Consumer<Location> callback : callbacks) callback.accept(location == null ? null : new Location(location));
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
        WorldClockWidgetView clock=new WorldClockWidgetView(this,layout.headerClock,paletteColors,uiBackgroundOpacity);
        clock.asHeader(id->{Bundle selected=new Bundle();Bundle content=new Bundle();content.putString("selected",id);selected.putBundle("content",content);selected.putBoolean("fresh",true);expandWidget(HomeLayout.HEADER_CLOCK,selected);});
        clock.setFocusable(true);clock.setClickable(true);
        clock.setOnFocusChangeListener((v,has)->{if(has){focused=HomeLayout.HEADER_CLOCK;if(allApps)appsFocus=focused;else homeFocus=focused;}});
        clock.setOnLongClickListener(v->{configureClock(HomeLayout.HEADER_CLOCK);return true;});
        FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(-1,dp(60),Gravity.TOP);
        cp.setMargins(dp(32),dp(16),dp(174),0);surface.addView(clock,cp);tiles.put(HomeLayout.HEADER_CLOCK,clock);clockWidgets.put(HomeLayout.HEADER_CLOCK,clock);
        addTopSettingsButton(START_SCREENSAVER, R.drawable.ic_screensaver_start, getString(R.string.hb_ss_start_screensaver),
                this::startScreensaverFromTopRow, dp(110));
        addTopSettingsButton(HOME_SETTINGS, R.drawable.ic_app_settings, getString(R.string.hb_homeboard_settings),
                this::homeSettings, dp(50));
    }
    /**
     * Start screensaver: exactly the remote's "screensaver" action (RemoteKeys.startScreensaver), except that an app's
     * preview screen is played here directly instead of through an intent to Home itself. Any key ends it and Home
     * comes back with this button focused.
     */
    private void startScreensaverFromTopRow() {
        if (layout == null) return;
        RemoteKeys.startScreensaver(this, layout.screensaver.choice, () -> {
            try { playScreensaver(ScreensaverActivity.MODE_NOW); }
            catch (RuntimeException unavailable) { toast(getString(R.string.hb_android_could_not_open_that_action)); }
        });
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
        view.setBackground(appTile || HomeLayout.isCalendarWidget(key) || HomeLayout.isRainWidget(key) || HomeLayout.isWorldClockWidget(key) ? null : tileBackground());
        view.setPadding(dp(appTile ? 2 : 16), dp(appTile ? 2 : 12),
                dp(appTile ? 2 : 16), dp(appTile ? 2 : 12));
        view.setOnFocusChangeListener((v, hasFocus) -> {
            if (isAirWidgetKey(key)&&phonePreviewBefore==null) airFor(key).setTileFocused(hasFocus);
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
        // Circular fill/halo belongs behind artwork; existing glass stays above unmasked artwork.
        appHighlight = new AppFocusHighlight(this, surface, surface,
                AppFocusHighlight.behindArtwork(banners, circular) ? 0 : -1, banners, circular, background, reducedMotion());
    }
    private void moveAppFocusHighlight(View card, boolean animate) {
        if (!active || overlay != null || appHighlight == null) return;
        appHighlight.move(card, animate);
    }
    private void hideAppFocusHighlight() {
        if (appHighlight != null) appHighlight.hide();
    }
    private View appHighlightTarget(View tile) {
        return AppFocusHighlight.target(tile);
    }
    private void buildHome() {
        addTopSettingsButtons();
        List<String> items = dashboardItems();
        int columns = homeGridColumns(), rows = homeGridRows();
        homePage = Math.max(0, Math.min(homePage, homePageCount(items) - 1));
        surface.setContentDescription(null);
        if (items.isEmpty()) { surface.setContentDescription(getString(R.string.hb_dashboard_empty)); if(!tiles.containsKey(focused))focused = ""; return; }
        TileGrid grid = new TileGrid(this, columns, rows, dashboardScale());
        FrameLayout.LayoutParams gp = new FrameLayout.LayoutParams(-1, -1);
        gp.setMargins(dp(AppGridSizing.SIDE_MARGIN), dp(AppGridSizing.HOME_TOP),
                dp(AppGridSizing.SIDE_MARGIN), dp(AppGridSizing.BOTTOM_MARGIN));
        surface.addView(grid, gp);
        boolean hasAppTile = false;
        for (HomeLayout.Cell cell : homeCells(items)) {
            if (cell.page != homePage) continue;
            visibleCells.add(cell);
            if (cell.key.startsWith("app:")) hasAppTile = true;
            View tile = phonePreviewBefore!=null&&(isAirWidgetKey(cell.key)||HomeLayout.isRainWidget(cell.key))?phonePreviewTile(cell.key):HomeLayout.FAVORITES.equals(cell.key) ? favoritesTile(cell.height == 1) : isAirWidgetKey(cell.key) ? airTile(cell.key, cell.width, cell.height)
                    : AndroidWidgetSpec.key(cell.key) ? androidWidgets.tile(cell.key,moving||resizing||phonePreviewBefore!=null)
                    : HomeLayout.isRainWidget(cell.key) ? rainTile(cell.key)
                    : HomeLayout.TIMER.equals(cell.key) ? new KitchenTimerWidgetView(this,paletteColors,uiBackgroundOpacity)
                    : HomeLayout.isGroceryWidget(cell.key) ? groceryTile(cell.key)
                    : HomeLayout.isWorldClockWidget(cell.key) ? clockTile(cell.key)
                    : HomeLayout.isCalendarWidget(cell.key) ? calendarTile(cell.key) : cell.key.startsWith("widget:") ? text(getString(R.string.hb_aw_unknown),16) : appTile(cell.key, true);
            if(HomeLayout.FAVORITES.equals(cell.key)) {
                tiles.put(cell.key,tile);
                favoritesWidget.interactive(!moving&&!resizing&&firstPageBefore==null);
            } else configureTile(tile, cell.key);
            if (isAirWidgetKey(cell.key)&&phonePreviewBefore==null) airFor(cell.key).bind(tile);
            grid.addTile(tile, cell.column, cell.row, cell.width, cell.height);
        }
        if (hasAppTile) createAppFocusHighlight();
        if (visibleCells.isEmpty()) surface.setContentDescription(getString(R.string.hb_dashboard_empty));
        footer = text(firstPageBefore!=null?getString(R.string.hb_fav_preview):favoriteOrderPreview!=null?getString(R.string.hb_fav_reorder):resizing ? resizeHelpText() : moving ? getString(R.string.hb_move_preview_arrows_place_anywhere_in_the_grid_ok_saves_back_canc)
                : getString(R.string.hb_hint_home) + (homePage + 1) + "/" + homePageCount(items) + getString(R.string.hb_right_all_apps_hold_ok_menu), 13);
        if (resizing) {
            footer.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
            footer.setPadding(dp(16), 0, dp(16), 0);
            footer.setSingleLine(true); footer.setEllipsize(android.text.TextUtils.TruncateAt.END);
            footer.setBackground(shape(paletteColors.raised, paletteColors.edge, 1));
        }
        FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams(-1, dp(40), Gravity.BOTTOM);
        fp.setMargins(dp(resizing ? 44 : 50), 0, dp(resizing ? 44 : 50), dp(10)); surface.addView(footer, fp);
    }
    private FavoritesWidgetView activeFavorites(){return favoritesBrowser!=null?favoritesBrowser:favoritesWidget;}
    private View favoritesTile(boolean ribbon) {
        favoritesWidget=newFavorites(ribbon);return favoritesWidget;
    }
    private FavoritesWidgetView newFavorites(boolean ribbon) {
        return new FavoritesWidgetView(this,favoriteOrderPreview==null?layout.favorites:favoriteOrderPreview,
            key->appTile(key,true),key->{if(!key.isEmpty())favoriteFocus=key;focused=HomeLayout.FAVORITES;homeFocus=focused;},
            this::activateFavorite,this::favoriteContext,paletteColors,uiBackgroundOpacity,
            layout.appUsesBanner(true),!layout.appUsesBanner(true)&&layout.circularIcons,appLabelHeightDp(),reducedMotion(),ribbon,favoriteFocus);
    }
    private void activateFavorite() {
        FavoritesWidgetView panel=activeFavorites();String key=panel==null?"":panel.activeKey();
        if(key.isEmpty()){expandWidget(HomeLayout.FAVORITES,null);}
        else activate(key);
    }
    private void favoriteContext() {
        FavoritesWidgetView panel=activeFavorites();String key=panel==null?"":panel.activeKey();
        if(favoritesBrowser!=null)dismissMenu();
        if(key.isEmpty()){favoritesActions();return;}
        App app=apps.get(key);
        menu(app==null?getString(R.string.hb_unavailable_app):app.label,
            new String[]{getString(R.string.hb_move),getString(R.string.hb_unfavorite),getString(R.string.hb_fav_widget_actions),getString(R.string.hb_more)},i->{
                if(i==0){favoriteOrderPreview=new ArrayList<>(layout.favorites);favoriteMovingKey=key;favoriteFocus=key;focused=HomeLayout.FAVORITES;render(0);}
                else if(i==1){layout.removeFavorite(key);save(getString(R.string.hb_fav_changed));render(0);}
                else if(i==2)favoritesActions();
                else menu(getString(R.string.hb_more),new String[]{getString(R.string.hb_uninstall_app),getString(R.string.hb_app_info)},a->{
                    if(a==0)uninstall(key);else if(app!=null)open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+app.component.getPackageName())));
                });
            });
    }
    private void favoritesActions() {
        menu(getString(R.string.hb_fav_title),getString(R.string.hb_fav_remove_hint),new String[]{getString(R.string.hb_move),getString(R.string.hb_resize),getString(R.string.hb_appearance),getString(R.string.hb_more)},i->{
            if(i==0)startMove(HomeLayout.FAVORITES);
            else if(i==1)startResize(HomeLayout.FAVORITES);
            else if(i==2)homeAppSizeSettings();
            else menu(getString(R.string.hb_fav_title),getString(R.string.hb_fav_remove_hint),new String[]{getString(R.string.hb_fav_manage),getString(R.string.hb_fav_first_page),getString(R.string.hb_remove_from_home)},a->{
                if(a==0)showApps();else if(a==1)chooseFirstPageRain();else deleteTile(HomeLayout.FAVORITES);
            });
        });
    }
    private void openFavoritesBrowser() {
        dismissMenu();cancelOk();activeMenu=null;menuStack.clear();
        overlay=new FrameLayout(this);overlay.setBackgroundColor(paletteColors.scrim);
        favoritesBrowser=newFavorites(false);
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-1,-1);lp.setMargins(dp(32),dp(32),dp(32),dp(32));
        overlay.addView(favoritesBrowser,lp);surface.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        root.addView(overlay,new FrameLayout.LayoutParams(-1,-1));updateUndoHint();favoritesBrowser.post(favoritesBrowser::enter);
    }
    private void chooseFirstPageRain() {
        List<String> rains=new ArrayList<>();for(String key:layout.widgets)if(HomeLayout.isRainWidget(key))rains.add(key);
        if(rains.isEmpty())rains.add(HomeLayout.RAIN);
        String[] names=new String[rains.size()];for(int i=0;i<names.length;i++){RainSettings r=layout.rainSettings.get(rains.get(i));names[i]=getString(R.string.hb_rain_rain)+" "+(i+1)+(r==null?"":" · "+r.displayName());}
        menu(getString(R.string.hb_fav_first_page),names,i->firstPagePreview(rains.get(i)));
    }
    private void firstPagePreview(String rain) {
        if (!beginLayoutPreview()) return;
        layout.convertFavorites();layout.arrangeFirstPage(rain,homeGridColumns(),homeGridRows(),false);
        allApps=false;homePage=0;focused=rain;render(0);
    }
    private boolean beginLayoutPreview() {
        try { firstPageBefore=HomeLayout.parse(layout.encode()); } catch(Exception e) { return false; }
        layoutPreviewPage=homePage;layoutPreviewFocus=focused;layoutPreviewHomeFocus=homeFocus;layoutPreviewApps=allApps;
        return true;
    }
    private void finishLayoutPreview(boolean apply) {
        if (firstPageBefore == null) return;
        if (!apply) { layout=firstPageBefore;homePage=layoutPreviewPage;focused=layoutPreviewFocus;homeFocus=layoutPreviewHomeFocus;allApps=layoutPreviewApps; }
        firstPageBefore=null;
        if (apply) save(getString(R.string.hb_fav_arranged));
        render(0);
    }
    private void chooseDefaultDashboard() {
        chooseDashboardInstance(true, rain -> chooseDashboardInstance(false, air -> {
            applyDefaultDashboard(rain,air,true);
        }));
    }
    private String firstDashboardInstance(boolean rain) {
        for(String key:layout.widgets)if(rain?HomeLayout.isRainWidget(key):isAirWidgetKey(key))return key;
        return rain?HomeLayout.RAIN:HomeLayout.AIR;
    }
    private void applyDefaultDashboard(String rain,String air,boolean persist) {
        if (!layout.favoritesContainer) layout.convertFavorites();
        layout.homeColumns=8; layout.homeRows=4;
        layout.arrangeDefaultDashboard(rain,air,8,4);
        allApps=false;homePage=0;focused=rain;homeFocus=rain;
        if(persist){save(getString(R.string.hb_dashboard_applied));render(0);}
    }
    private void chooseDashboardInstance(boolean rain, Consumer<String> chosen) {
        List<String> instances=new ArrayList<>();
        for(String key:layout.widgets) if(rain?HomeLayout.isRainWidget(key):isAirWidgetKey(key)) instances.add(key);
        if(instances.size()<2){chosen.accept(instances.isEmpty()?(rain?HomeLayout.RAIN:HomeLayout.AIR):instances.get(0));return;}
        String[] names=new String[instances.size()];
        for(int i=0;i<names.length;i++) {
            RainSettings settings=rain?layout.rainSettings.get(instances.get(i)):null;
            names[i]=getString(rain?R.string.hb_rain_rain:R.string.hb_air_quality_widget)+" "+(i+1)+(settings==null?"":" · "+settings.displayName());
        }
        menu(getString(R.string.hb_dashboard_choose_instance),names,i->chosen.accept(instances.get(i)));
    }
    private CalendarWidgetController calendarController(String key) {
        return new CalendarWidgetController(this, calendarSnapshot, layout.calendarSettings.get(key), this::menu,
                this::calendarDetails, this::calendarDayEditor, () -> { save(getString(R.string.hb_cal_updated)); render(0); },
                () -> calendarWidgets.get(key), () -> openMenu(new MenuSpec(getString(R.string.hb_data_title), () -> drawCalendarSources(key))));
    }
    private void drawCalendarSources(String key) {
        if(overlay!=null)root.removeView(overlay);overlay=new FrameLayout(this);overlay.setBackgroundColor(paletteColors.scrim);
        CalendarSourcePanel panel=new CalendarSourcePanel(this,calendarData,layout.calendarSettings.get(key),paletteColors,uiBackgroundOpacity,this::backMenu);
        int width=Math.min(dp(840),dashboardWidthPixels()-dp(64));
        int height=Math.min(dp(510),dashboardHeightPixels()-dp(48));
        overlay.addView(panel,new FrameLayout.LayoutParams(width,height,Gravity.CENTER));root.addView(overlay,new FrameLayout.LayoutParams(-1,-1));updateUndoHint();
    }
    private void configureCalendar(String key) { calendarController(key).configure(); }
    private View calendarTile(String key) {
        CalendarWidgetView tile = new CalendarWidgetView(this, calendarSnapshot, layout.calendarSettings.get(key), paletteColors);
        tile.setFillOpacity(uiBackgroundOpacity); calendarWidgets.put(key, tile); return tile;
    }
    private void calendarDayEditor(int current, java.util.function.Consumer<Integer> saveValue) {
        if (invokingMenuAction && activeMenu != null) menuStack.add(activeMenu);
        dismissMenu(); cancelOk(); activeMenu = null; menuGeneration++; calendarDayEditor = true;
        surface.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        surface.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        overlay = new FrameLayout(this); overlay.setBackgroundColor(paletteColors.scrim);
        LinearLayout panel = new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(24), dp(20), dp(24), dp(20)); panel.setBackground(shape(paletteColors.raised, paletteColors.edge, 1));
        panel.addView(text(getString(R.string.hb_cal_day_count), 23));
        android.widget.EditText input = new android.widget.EditText(this);
        input.setTextColor(paletteColors.text); input.setHintTextColor(paletteColors.muted); input.setSingleLine(true);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER); input.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        input.setText(String.valueOf(current)); input.selectAll();
        input.setHint(getString(R.string.hb_cal_custom_hint)); input.setContentDescription(getString(R.string.hb_cal_custom_hint));
        panel.addView(input, new LinearLayout.LayoutParams(-1, dp(64)));
        Runnable apply = () -> {
            int value;
            try { value = CalendarWidgetSettings.days(input.getText().toString()); }
            catch (IllegalArgumentException invalid) { input.setError(getString(R.string.hb_cal_invalid_days)); input.requestFocus(); return; }
            ((android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(input.getWindowToken(), 0);
            dismissMenu(); activeMenu = null; menuStack.clear(); saveValue.accept(value);
        };
        input.setOnEditorActionListener((v, actionId, event) -> { if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) { apply.run(); return true; } return false; });
        LinearLayout buttons = new LinearLayout(this);
        buttons.addView(infoButton(getString(R.string.hb_cal_save), apply), new LinearLayout.LayoutParams(0, dp(52), 1));
        buttons.addView(infoButton(getString(R.string.hb_cal_cancel), this::backMenu), new LinearLayout.LayoutParams(0, dp(52), 1));
        panel.addView(buttons);
        overlay.addView(panel, new FrameLayout.LayoutParams(Math.min(dp(650), dashboardWidthPixels() - dp(48)), -2, Gravity.CENTER));
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1));updateUndoHint(); input.requestFocus();
    }
    private void calendarDetails(String title, String detail) {
        if (invokingMenuAction && activeMenu != null) menuStack.add(activeMenu);
        dismissMenu(); cancelOk(); activeMenu = null; menuGeneration++;
        surface.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        surface.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        hideAppFocusHighlight();
        overlay = new FrameLayout(this); overlay.setBackgroundColor(paletteColors.scrim);
        LinearLayout panel = new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(24), dp(18), dp(24), dp(18));
        panel.setBackground(shape(paletteColors.raised, paletteColors.edge, 1));
        TextView heading = text(title, 23); heading.setMaxLines(2);
        heading.setEllipsize(android.text.TextUtils.TruncateAt.END);
        panel.addView(heading, new LinearLayout.LayoutParams(-1, -2));
        android.widget.ScrollView scroll = new android.widget.ScrollView(this); scroll.setFillViewport(true);
        TextView body = text(detail, 18); body.setPadding(0, dp(12), 0, dp(12));
        scroll.addView(body, new android.widget.ScrollView.LayoutParams(-1, -2));
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        aboutBackButton = infoButton(getString(R.string.hb_fs_back), this::backMenu);
        panel.addView(aboutBackButton, new LinearLayout.LayoutParams(-1, dp(52)));
        int width = Math.min(dp(840), dashboardWidthPixels() - dp(64));
        int height = Math.min(dp(510), dashboardHeightPixels() - dp(48));
        overlay.addView(panel, new FrameLayout.LayoutParams(width, height, Gravity.CENTER));
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1));updateUndoHint(); aboutBackButton.requestFocus();
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
        LinearLayout.LayoutParams trendLp = new LinearLayout.LayoutParams(-1, dp(compact ? 44 : large ? 64 : 64));
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
            String label = app == null ? getString(R.string.hb_unavailable_app) : app.label;
            BannerAppTile tile = new BannerAppTile(this, banner == null ? icon : banner, banner == null, label);
            tile.setContentDescription(label + (app == null ? getString(R.string.hb_hint_hold_ok_to_remove) : getString(R.string.hb_hint_hold_ok_for_menu)));
            return tile;
        }
        if (layout.circularIcons) {
            Drawable artwork = null;
            try { if (app != null) artwork = app.info.loadIcon(getPackageManager()); }
            catch (RuntimeException ignored) { }
            String label = app == null ? getString(R.string.hb_unavailable_app) : app.label;
            CircularAppTile circular = new CircularAppTile(this, artwork, label);
            circular.setContentDescription(label + (app == null ? getString(R.string.hb_hint_hold_ok_to_remove) : getString(R.string.hb_hint_hold_ok_for_menu)));
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
        String label = app == null ? getString(R.string.hb_unavailable_app) : app.label;
        TextView name = text(label, 16);
        name.setGravity(Gravity.CENTER); name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tile.addView(name, new LinearLayout.LayoutParams(-1, -2));
        tile.setContentDescription(label + (app == null ? getString(R.string.hb_hint_hold_ok_to_remove) : getString(R.string.hb_hint_hold_ok_for_menu)));
        return tile;
    }
    private void buildApps() {
        addTopSettingsButtons();
        List<String> items = appItems();
        int columns = appColumns(), rows = appRows(), pageSize = appPageSize();
        if ((getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0)
            android.util.Log.i("HomeboardAppGrid", "metrics=" + getResources().getDisplayMetrics()
                + " root=" + root.getWidth() + "x" + root.getHeight()
                + " labelDp=" + appLabelHeightDp() + " grid=" + columns + "x" + rows);
        appsPage = Math.max(0, Math.min(appsPage, Math.max(0, (items.size() - 1) / pageSize)));
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
        footer = text(moving ? getString(R.string.hb_move_preview_arrows_reposition_ok_saves_back_cancels)
                : getString(R.string.hb_all_apps)+" · "+getString(R.string.hb_hint_left_edge_back_home_hold_ok_menu_page) + (appsPage + 1)
                    + "    " + items.size() + getString(R.string.hb_apps), 13);
        FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams(-1, dp(36), Gravity.BOTTOM);
        fp.setMargins(dp(50), 0, dp(50), dp(10)); surface.addView(footer, fp);
    }
    // Embedded hierarchies can outsize the Activity's resource/window metrics.
    // Once attached, fit the grid to the hierarchy that actually contains it.
    int dashboardWidthPixels() {
        return root != null && root.getWidth() > 0 ? root.getWidth() : getResources().getDisplayMetrics().widthPixels;
    }
    int dashboardHeightPixels() {
        return root != null && root.getHeight() > 0 ? root.getHeight() : getResources().getDisplayMetrics().heightPixels;
    }
    private int appColumns() {
        int screenWidthDp = Math.round(dashboardWidthPixels()
                / getResources().getDisplayMetrics().density);
        return AppGridSizing.columns(screenWidthDp - AppGridSizing.SIDE_MARGIN * 2,
                appLabelHeightDp(), layout.appBanners);
    }
    private int appRows() {
        int screenHeightDp = Math.round(dashboardHeightPixels()
                / getResources().getDisplayMetrics().density);
        return AppGridSizing.rows(screenHeightDp - AppGridSizing.APPS_TOP - AppGridSizing.BOTTOM_MARGIN,
                appLabelHeightDp(), layout.appBanners);
    }
    private int appPageSize() { return appColumns() * appRows(); }
    private final class TileGrid extends ViewGroup {
        private final int columns, rows, scale;
        private final List<int[]> positions = new ArrayList<>();
        TileGrid(Context context, int columns, int rows) { this(context, columns, rows, 100); }
        TileGrid(Context context, int columns, int rows, int scale) {
            super(context); this.columns = columns; this.rows = rows; this.scale = scale;
            setClipChildren(false); setClipToPadding(false);
        }
        void addTile(View tile, int column, int row, int span) {
            addTile(tile, column, row, span, 1);
        }
        void addTile(View tile, int column, int row, int spanWidth, int spanHeight) {
            positions.add(new int[]{column, row, spanWidth, spanHeight});
            if (scale == 100) { addView(tile, new ViewGroup.LayoutParams(0, 0)); return; }
            // Transform a parent so widget/app focus animations retain their own scale.
            FrameLayout cell = new FrameLayout(getContext());
            cell.setClipChildren(false); cell.setClipToPadding(false);
            cell.setPivotX(0); cell.setPivotY(0);
            cell.setScaleX(DashboardScale.factor(scale)); cell.setScaleY(DashboardScale.factor(scale));
            cell.addView(tile, new FrameLayout.LayoutParams(-1, -1));
            addView(cell, new ViewGroup.LayoutParams(0, 0));
        }
        @Override protected void onMeasure(int widthSpec, int heightSpec) {
            int width = View.MeasureSpec.getSize(widthSpec);
            int height = View.MeasureSpec.getSize(heightSpec);
            setMeasuredDimension(width, height);
            width = DashboardScale.logicalPixels(width, scale);
            height = DashboardScale.logicalPixels(height, scale);
            for (int i = 0; i < getChildCount(); i++) {
                int[] cell = positions.get(i);
                int childWidth = Math.max(0, width * cell[2] / columns - dp(AppGridSizing.COLUMN_GAP));
                int childHeight = Math.max(0, height * cell[3] / rows - dp(AppGridSizing.ROW_GAP));
                getChildAt(i).measure(View.MeasureSpec.makeMeasureSpec(childWidth, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(childHeight, View.MeasureSpec.EXACTLY));
            }
        }
        @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            int width = DashboardScale.logicalPixels(right - left, scale);
            int height = DashboardScale.logicalPixels(bottom - top, scale);
            for (int i = 0; i < getChildCount(); i++) {
                int[] cell = positions.get(i);
                View child = getChildAt(i);
                int x = width * cell[0] / columns + dp(AppGridSizing.COLUMN_GAP / 2f);
                int y = height * cell[1] / rows + dp(AppGridSizing.ROW_GAP / 2f);
                x = Math.round(x * DashboardScale.factor(scale));
                y = Math.round(y * DashboardScale.factor(scale));
                child.layout(x, y, x + child.getMeasuredWidth(), y + child.getMeasuredHeight());
            }
        }
    }
    private void restoreFocus() {
        View target = tiles.get(focused);
        if(!allApps&&HomeLayout.FAVORITES.equals(focused)&&favoritesWidget!=null){favoritesWidget.restore();return;}
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
            if (target == null && visibleCells.isEmpty()) { focused="";homeFocus="";target=surface; }
        }
        if (target == null && !tiles.isEmpty()) {
            focused = HOME_SETTINGS;
            target = tiles.get(HOME_SETTINGS);
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
                    || target != tiles.get(focused) || !target.hasFocus() || appHighlight == null) return;
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
            if(focusUndo())return;focused="";restoreFocus();return;
        }
        if(focusUndo())return;
        focused = HOME_SETTINGS;
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

    private boolean focusUndo(){return undoButton!=null&&undoButton.requestFocus();}
    private boolean backUndoAllowed(){
        return active&&!closed&&hasWindowFocus()&&overlay==null&&expandedWidget==null&&clockEditor==null
            &&!moving&&!resizing&&firstPageBefore==null&&favoriteOrderPreview==null&&undoGroupStart==null
            &&gridSelector==null&&adjustmentArm==null&&adjustmentCancel==null&&favoritesBrowser==null
            &&!phonePanelOpen&&phonePreviewBefore==null;
    }
    private String undoSnapshot(){return preferences.getString(PREF_UNDO_HISTORY,"[]");}
    private void surfaceBack(){if(allApps)showHome();else if(favoritesWidget!=null&&favoritesWidget.browsing())favoritesWidget.header();}
    private boolean backUndoKey(KeyEvent event){
        if(event.getAction()==KeyEvent.ACTION_DOWN){
            if(event.getRepeatCount()==0&&backUndoAllowed()&&backUndo.begin(android.os.SystemClock.uptimeMillis(),undoSnapshot())){
                backUndoFromButton=undoButton!=null&&undoButton.hasFocus();
                List<UndoEntry> entries=readUndoHistory();backUndoTarget=entries.isEmpty()?getString(R.string.hb_no_recent_homeboard_changes_can_be_undone):entries.get(0).label;
                handler.postDelayed(backUndoTick,250);
            }
        }else if(event.getAction()==KeyEvent.ACTION_UP){
            handler.removeCallbacks(backUndoTick);if(!event.isCanceled()&&backUndoAllowed()&&backUndo.due(android.os.SystemClock.uptimeMillis()))backUndoTick.run();boolean completed=backUndo.finished();boolean navigate=backUndo.release(event.isCanceled());
            if(backUndoProgressShown&&!completed)showUndoBanner(backUndoTarget);backUndoProgressShown=false;
            if(navigate){if(backUndoFromButton)restoreFocus();else surfaceBack();}
        }
        return true;
    }
    private final Runnable backUndoTick=new Runnable(){public void run(){
        if(!backUndo.down())return;
        if(!backUndoAllowed()){cancelBackUndo(true);return;}
        long now=android.os.SystemClock.uptimeMillis();
        if(backUndo.due(now)){
            boolean current=backUndo.finish(now,undoSnapshot());backUndoProgressShown=false;
            if(!current)showUndoBanner(getString(R.string.hb_undo_changed),false);
            else if(readUndoHistory().isEmpty())showUndoBanner(getString(R.string.hb_no_recent_homeboard_changes_can_be_undone),false);
            else undoThrough(0);
            return;
        }
        if(!backUndoProgressShown){showUndoBanner((readUndoHistory().isEmpty()?"":getString(R.string.hb_undo_5))+backUndoTarget,false);backUndoProgressShown=true;}
        undoHint.setText(getString(R.string.hb_undo_keep_holding));undoHint.setVisibility(View.VISIBLE);
        undoProgress.setProgress(backUndo.progress(now));undoProgress.setVisibility(View.VISIBLE);handler.postDelayed(this,50);
    }};
    private void cancelBackUndo(boolean restore){
        handler.removeCallbacks(backUndoTick);if(backUndo.down())backUndoIgnoreRelease=true;backUndo.cancel();
        if(backUndoProgressShown){backUndoProgressShown=false;if(restore&&active&&!closed)showUndoBanner(backUndoTarget);else hideUndoBanner.run();}
    }

    @Override protected FrameLayout dashboardOverlayRoot() { return super.dashboardOverlayRoot(); }
    protected Context dashboardDialogContext() { return this; }
    @Override protected void configureDashboardDialog(android.app.Dialog dialog) { super.configureDashboardDialog(dialog); }
    /** Window dispatch can be supplied by the native-resolution rendering proof. */
    @Override protected boolean dispatchDashboardWindowKey(KeyEvent event) { return super.dispatchDashboardWindowKey(event); }
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        android.app.Dialog nativeDialog = nativeDashboardDialog();
        if (nativeDialog != null) { noteHomeInput(); return nativeDialog.dispatchKeyEvent(event); }
        noteHomeInput();   // also for keys that return before Activity.dispatchKeyEvent calls onUserInteraction
        if(event.getKeyCode()==KeyEvent.KEYCODE_BACK){
            if(event.getAction()==KeyEvent.ACTION_UP&&backUndoIgnoreRelease){backUndoIgnoreRelease=false;return true;}
            if(event.getAction()==KeyEvent.ACTION_DOWN&&event.getRepeatCount()==0)backUndoIgnoreRelease=false;
        }
        if(event.getAction()==KeyEvent.ACTION_DOWN&&event.getKeyCode()!=KeyEvent.KEYCODE_BACK&&backUndo.down())cancelBackUndo(true);
        if(event.getKeyCode()==KeyEvent.KEYCODE_BACK&&(backUndo.down()||backUndoAllowed()))return backUndoKey(event);
        if(phonePreviewBefore!=null){if(event.getKeyCode()==KeyEvent.KEYCODE_BACK&&event.getAction()==KeyEvent.ACTION_UP)phoneCancelPreview();return true;}
        if(phonePanelOpen){if(event.getKeyCode()==KeyEvent.KEYCODE_BACK){if(event.getAction()==KeyEvent.ACTION_UP)phoneDismiss();return true;}return dispatchDashboardWindowKey(event);}
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
                // The chooser's action area: Back returns to the same list row; from the list it closes the chooser.
                boolean toRow = screensaverChooserShowing() && screensaverPane != null && screensaverPane.hasFocus();
                if (event.getAction() == KeyEvent.ACTION_UP) { if (toRow) focusScreensaverRow(screensaverPicker.browsedIndex); else backMenu(); }
                return true;
            }
            return dispatchDashboardWindowKey(event);
        }
        if (undoButton != null && undoButton.hasFocus() && backUndoAllowed()) {
            if(ok){if(event.getAction()==KeyEvent.ACTION_UP&&!event.isCanceled())undoButton.performClick();return true;}
            if(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_BACK||key==KeyEvent.KEYCODE_ESCAPE){if(event.getAction()==KeyEvent.ACTION_DOWN)restoreFocus();return true;}
            if(key==KeyEvent.KEYCODE_DPAD_RIGHT){if(event.getAction()==KeyEvent.ACTION_DOWN){if(!allApps)advanceHomePage(1);else restoreFocus();}return true;}
            if(key==KeyEvent.KEYCODE_DPAD_DOWN)return true;
            if(key==KeyEvent.KEYCODE_MENU){if(event.getAction()==KeyEvent.ACTION_UP)homeSettings();return true;}
            return dispatchDashboardWindowKey(event);
        }
        if (aboutBackButton != null && aboutBackButton.hasFocus()) return dispatchDashboardWindowKey(event);
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
        if(firstPageBefore!=null){
            if(event.getAction()==KeyEvent.ACTION_UP&&(ok||key==KeyEvent.KEYCODE_BACK||key==KeyEvent.KEYCODE_ESCAPE)){
                finishLayoutPreview(ok);
            } else if(event.getAction()==KeyEvent.ACTION_DOWN&&(key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT)) {
                int next=HomePageNavigation.next(homePage,homePageCount(layout.widgets),key==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1);
                if(next==HomePageNavigation.ALL_APPS){finishLayoutPreview(false);showApps();}
                else if(next!=homePage){homePage=next;focused="";render(key==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1);}
            }return true;
        }
        if(favoriteOrderPreview!=null){
            if(event.getAction()==KeyEvent.ACTION_DOWN&&key>=KeyEvent.KEYCODE_DPAD_UP&&key<=KeyEvent.KEYCODE_DPAD_RIGHT){
                int from=favoriteOrderPreview.indexOf(favoriteMovingKey);
                int delta=key==KeyEvent.KEYCODE_DPAD_LEFT?-1:key==KeyEvent.KEYCODE_DPAD_RIGHT?1:key==KeyEvent.KEYCODE_DPAD_UP?-1:1;
                HomeLayout.move(favoriteOrderPreview,from,Math.max(0,Math.min(favoriteOrderPreview.size()-1,from+delta)));render(0);
            }
            if(event.getAction()==KeyEvent.ACTION_UP&&(ok||key==KeyEvent.KEYCODE_BACK)){
                if(ok){layout.favorites.clear();layout.favorites.addAll(favoriteOrderPreview);save(getString(R.string.hb_fav_changed));}
                favoriteOrderPreview=null;favoriteMovingKey=null;render(0);
            }return true;
        }
        if (ok && event.getAction() == KeyEvent.ACTION_UP && consumeOkUp) {
            consumeOkUp = false; return true;
        }
        if(favoritesBrowser!=null){
            if(key==KeyEvent.KEYCODE_BACK){if(event.getAction()==KeyEvent.ACTION_UP)dismissMenu();return true;}
            if(ok){if(event.getAction()==KeyEvent.ACTION_DOWN&&event.getRepeatCount()==0){okHeld=true;handler.postDelayed(heldOk,600);}else if(event.getAction()==KeyEvent.ACTION_UP){boolean press=okHeld;cancelOk();if(press)activateFavorite();}return true;}
            if(key>=KeyEvent.KEYCODE_DPAD_UP&&key<=KeyEvent.KEYCODE_DPAD_RIGHT){if(event.getAction()==KeyEvent.ACTION_DOWN&&!favoritesBrowser.navigate(key))favoritesBrowser.header();return true;}
        }
        if (overlay != null) {
            if (calendarDayEditor) {
                if (key == KeyEvent.KEYCODE_BACK || key == KeyEvent.KEYCODE_ESCAPE) { if (event.getAction() == KeyEvent.ACTION_UP) backMenu(); return true; }
                return dispatchDashboardWindowKey(event);
            }
            if (key == KeyEvent.KEYCODE_BACK || key == KeyEvent.KEYCODE_ESCAPE) {
                if (event.getAction() == KeyEvent.ACTION_UP) backMenu();
                return true;
            }
            if (activeMenu != null && activeMenu.paletteGrid) return dispatchDashboardWindowKey(event);
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
            return dispatchDashboardWindowKey(event);
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
                if (moving) cancelMove(); else if (allApps) showHome(); else if(favoritesWidget!=null&&favoritesWidget.browsing())favoritesWidget.header();
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
        return dispatchDashboardWindowKey(event);
    }
    private void cancelOk() { handler.removeCallbacks(heldOk); okHeld = false; }
    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        scheduleScreensaver();
        if (surface != null) updateAir();
        if (!hasFocus) {cancelOk();cancelBackUndo(true);}
        else if (root != null) root.post(() -> {
            if(active&&overlay==null&&expandedWidget!=null&&expandedWidget.configuring()){expandedWidget.resumeAfterConfiguration();return;}
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
        if(allApps&&appItems().isEmpty()&&key==KeyEvent.KEYCODE_DPAD_LEFT){showHome();return;}
        if(HomeLayout.HEADER_CLOCK.equals(focused)){
            if(key==KeyEvent.KEYCODE_DPAD_RIGHT){focused=START_SCREENSAVER;restoreFocus();}
            else if(key==KeyEvent.KEYCODE_DPAD_DOWN)focusFirstSurfaceItem();
            else if(key==KeyEvent.KEYCODE_DPAD_LEFT&&allApps)showHome();
            return;
        }
        if (START_SCREENSAVER.equals(focused)) {
            if (key == KeyEvent.KEYCODE_DPAD_LEFT) { focused = HomeLayout.HEADER_CLOCK; restoreFocus(); }
            else if (key == KeyEvent.KEYCODE_DPAD_RIGHT) { focused = HOME_SETTINGS; restoreFocus(); }
            else if (key == KeyEvent.KEYCODE_DPAD_DOWN) focusFirstSurfaceItem();
            return;
        }
        if (HOME_SETTINGS.equals(focused)) {
            if (key == KeyEvent.KEYCODE_DPAD_DOWN) focusFirstSurfaceItem();
            else if (key == KeyEvent.KEYCODE_DPAD_LEFT) { focused = START_SCREENSAVER; restoreFocus(); }
            else if(key==KeyEvent.KEYCODE_DPAD_RIGHT&&!allApps)advanceHomePage(1);
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
            if (key == KeyEvent.KEYCODE_DPAD_DOWN) {next += columns;if(next>=items.size()&&focusUndo())return;}
            if (next >= 0 && next < items.size()) {
                focused = items.get(next); int page = next / appPageSize();
                if (page != appsPage) { appsPage = page; render(0); } else restoreFocus();
            }
            return;
        }
        if(HomeLayout.FAVORITES.equals(focused)&&favoritesWidget!=null&&favoritesWidget.navigate(key))return;
        HomeLayout.Cell current = null;
        for (HomeLayout.Cell c : visibleCells) if (c.key.equals(focused)) current = c;
        if (current == null) {
            if (key == KeyEvent.KEYCODE_DPAD_RIGHT) advanceHomePage(1);
            else if(key==KeyEvent.KEYCODE_DPAD_LEFT)advanceHomePage(-1);
            else if (key == KeyEvent.KEYCODE_DPAD_UP) { focused = HOME_SETTINGS; restoreFocus(); }
            else if(key==KeyEvent.KEYCODE_DPAD_DOWN)focusUndo();
            return;
        }
        if (key == KeyEvent.KEYCODE_DPAD_UP && current.row == 0) { focused = HOME_SETTINGS; restoreFocus(); return; }
        HomeLayout.Cell best = nearestHomeNeighbor(current, key);
        if (best != null) { focused = best.key; restoreFocus(); return; }
        if (key == KeyEvent.KEYCODE_DPAD_RIGHT) {
            advanceHomePage(1);
        } else if (key == KeyEvent.KEYCODE_DPAD_LEFT && homePage > 0) advanceHomePage(-1);
        else if (key == KeyEvent.KEYCODE_DPAD_DOWN) focusUndo();
    }
    private void advanceHomePage(int direction) {
        int next=HomePageNavigation.next(homePage,homePageCount(layout.widgets),direction);
        if(next==HomePageNavigation.ALL_APPS){showApps();return;}
        if(next!=homePage){homePage=next;focused="";render(direction);}
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
        if(HomeLayout.HEADER_CLOCK.equals(key)){expandWidget(key,null);return;}
        if (SETTINGS.equals(key)) { homeSettings(); return; }
        if (START_SCREENSAVER.equals(key)) { startScreensaverFromTopRow(); return; }
        if (HOME_SETTINGS.equals(key)) { homeSettings(); return; }
        if(AndroidWidgetSpec.key(key)){expandWidget(key,null);return;}
        if(HomeLayout.FAVORITES.equals(key)){activateFavorite();return;}
        if (HomeLayout.TIMER.equals(key)||HomeLayout.isRainWidget(key)||HomeLayout.isCalendarWidget(key)||isAirWidgetKey(key)||HomeLayout.isWorldClockWidget(key)||HomeLayout.isGroceryWidget(key)) { expandWidget(key,null); return; }
        App app = apps.get(key);
        if (app == null) { if (!key.isEmpty()) toast(getString(R.string.hb_app_unavailable_hold_ok_to_remove_this_tile)); return; }
        open(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(app.component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
    }
    private int widgetTitle(WidgetKind kind){return switch(kind){
        case ANDROID -> R.string.hb_aw_title;case AIR -> R.string.hb_air_quality_widget;case CALENDAR -> R.string.hb_cal_calendar;
        case RAIN -> R.string.hb_rain_rain;case FAVORITES -> R.string.hb_fav_title;
        case TIMER -> R.string.hb_timer_title;case GROCERY -> R.string.hb_grocery_title;case WORLD_CLOCK -> R.string.hb_wc_title;
    };}
    private void expandWidget(String key,Bundle saved){
        if(closed)return;WidgetKind kind=PhoneConfiguration.kind(key);if(kind==null)return;cancelOk();consumeOkUp=false;
        if(expandedWidget!=null){expandedWidget.release();expandedWidget=null;}
        CalendarWidgetView compact=calendarWidgets.get(key);
        java.time.LocalDate anchor=compact==null?null:compact.firstVisibleDate();
        if(anchor!=null&&anchor.isBefore(java.time.LocalDate.now(CalendarSnapshot.BANGKOK)))anchor=null;
        final java.time.LocalDate initialDate=anchor;
        WidgetExpansion.Factory factory=(host,state)->switch(kind){
            case ANDROID -> new AndroidWidgetExpanded(host,androidWidgets,key,layout);
            case CALENDAR -> new CalendarExpanded(host,calendarSnapshot,layout.calendarSettings.getOrDefault(key,new CalendarWidgetSettings()),initialDate,state);
            case RAIN -> new RainExpanded(host,rainFor(key),state);
            case AIR -> new AirExpanded(host,airFor(key),this::requestAirLocation,state);
            case TIMER -> new KitchenTimerExpanded(host,()->{expandedWidget.dismiss();if(phoneEditor==null)phoneEditor=new PhoneEditor(this);phoneEditor.open();},state);
            case GROCERY -> new GroceryExpanded(host,()->layout.grocery(key),()->expandedWidget.configure(()->configureGroceries(key)),()->{expandedWidget.dismiss();if(phoneEditor==null)phoneEditor=new PhoneEditor(this);phoneEditor.open();},state);
            case WORLD_CLOCK -> new WorldClockExpanded(host,layout.clock(key),state);
            case FAVORITES -> new FavoritesExpanded(host,()->new ArrayList<>(layout.favorites),this::activate,this::open,
                app->{layout.removeFavorite(app);save(getString(R.string.hb_removed_app_from_home));render(0);},
                order->{layout.favorites.clear();layout.favorites.addAll(order);save(getString(R.string.hb_fav_changed));render(0);},state);
        };
        final int origin=saved==null?homePage:saved.getInt("originPage",homePage);
        final boolean originApps=saved==null?allApps:saved.getBoolean("originApps",allApps);
        String expandedTitle=getString(widgetTitle(kind));
        if(kind==WidgetKind.ANDROID&&layout.androidWidget(key)!=null)expandedTitle=layout.androidWidget(key).label;
        if(kind==WidgetKind.WORLD_CLOCK&&!layout.clock(key).title.isEmpty())expandedTitle=layout.clock(key).title;
        expandedWidget=new WidgetExpansion(this,key,expandedTitle,paletteColors,uiBackgroundOpacity,origin,saved,factory,
            ()->{switch(kind){case ANDROID:androidWidgets.configure(key);break;case TIMER:KitchenTimerUi.access(this);expandedWidget.resumeAfterConfiguration();break;case GROCERY:configureGroceries(key);break;case CALENDAR:configureCalendar(key);break;case RAIN:rainFor(key).configure();break;case AIR:AirQualityCard air=airFor(key);air.configureWidget();if(!air.ready())expandedWidget.resumeAfterConfiguration();break;case FAVORITES:favoritesActions();break;case WORLD_CLOCK:configureClock(key);break;}},
            ()->{expandedWidget=null;allApps=originApps;homePage=origin;focused=HomeLayout.HEADER_CLOCK.equals(key)||layout.widgets.contains(key)?key:"";if(allApps)appsFocus=focused;else homeFocus=focused;render(0);});
        expandedWidget.open(saved==null||saved.getBoolean("fresh",false)?tiles.get(key):null,(saved==null||saved.getBoolean("fresh",false))&&!reducedMotion());
    }
    private void contextMenu(String key) {
        if(HomeLayout.HEADER_CLOCK.equals(key)){configureClock(key);return;}
        // Hold OK on Start screensaver (D1): Home settings · Screensaver on its own, so Back returns to the button.
        if (START_SCREENSAVER.equals(key)) { screensaverSettings(); return; }
        if (HOME_SETTINGS.equals(key)) { homeSettings(); return; }
        if (key.isEmpty() || SETTINGS.equals(key)) { homeSettings(); return; }
        if(HomeLayout.FAVORITES.equals(key)){favoriteContext();return;}
        if(AndroidWidgetSpec.key(key)){
            java.util.List<String> labels=new ArrayList<>(java.util.List.of(getString(R.string.hb_move),getString(R.string.hb_resize),getString(R.string.hb_delete_widget),getString(R.string.hb_aw_repair)));
            if(androidWidgets.reconfigurable(key))labels.add(getString(R.string.hb_fs_configure));
            AndroidWidgetSpec widget=layout.androidWidget(key);
            menu(widget==null?getString(R.string.hb_aw_title):widget.label,labels.toArray(new String[0]),i->{if(i==0)startMove(key);else if(i==1)startResize(key);else if(i==2)deleteTile(key);else if(i==3)androidWidgets.repair(key);else androidWidgets.configure(key);});return;
        }
        if(HomeLayout.TIMER.equals(key)){menu(getString(R.string.hb_timer_title),new String[]{getString(R.string.hb_fs_configure),getString(R.string.hb_move),getString(R.string.hb_resize),getString(R.string.hb_delete_widget)},i->{if(i==0)expandWidget(key,null);else if(i==1)startMove(key);else if(i==2)startResize(key);else deleteTile(key);});return;}
        if(HomeLayout.isGroceryWidget(key)){menu(getString(R.string.hb_grocery_title),new String[]{getString(R.string.hb_fs_configure),getString(R.string.hb_move),getString(R.string.hb_resize),getString(R.string.hb_delete_widget)},i->{if(i==0)configureGroceries(key);else if(i==1)startMove(key);else if(i==2)startResize(key);else deleteTile(key);});return;}
        if(HomeLayout.isWorldClockWidget(key)) {
            menu(getString(R.string.hb_wc_title),new String[]{getString(R.string.hb_wc_configure),getString(R.string.hb_move),getString(R.string.hb_resize),getString(R.string.hb_delete_widget)},i->{
                if(i==0)configureClock(key);else if(i==1)startMove(key);else if(i==2)startResize(key);else deleteTile(key);
            });return;
        }
        if (HomeLayout.isRainWidget(key)) {
            menu(getString(R.string.hb_rain_rain),new String[]{getString(R.string.hb_rain_configure),getString(R.string.hb_move),getString(R.string.hb_resize),getString(R.string.hb_delete_widget)}, i -> {
                if(i==0) rainFor(key).configure(); else if(i==1) startMove(key); else if(i==2) rainSize(key); else deleteTile(key);
            });return;
        }
        if (HomeLayout.isCalendarWidget(key)) {
            menu(getString(R.string.hb_cal_calendar), "", new String[]{getString(R.string.hb_cal_configure), getString(R.string.hb_move), getString(R.string.hb_resize), getString(R.string.hb_delete_widget)}, choice -> {
                if (choice == 0) configureCalendar(key);
                else if (choice == 1) startMove(key);
                else if (choice == 2) startResize(key);
                else deleteTile(key);
            }); return;
        }
        if (isAirWidgetKey(key)) {
            menu(getString(R.string.hb_air_quality_widget), getString(R.string.hb_choose_an_action_for_this_widget), new String[]{getString(R.string.hb_configure_location_and_source), getString(R.string.hb_move), getString(R.string.hb_resize), getString(R.string.hb_more)}, choice -> {
                if (choice == 0) airFor(key).configureWidget();
                if (choice == 1) startMove(key);
                if (choice == 2) startResize(key);
                if (choice == 3) menu(getString(R.string.hb_hint_more_air_quality), getString(R.string.hb_delete_removes_this_widget_panel_undo_restores_it),
                        new String[]{getString(R.string.hb_delete_widget), getString(R.string.hb_refresh_reading), getString(R.string.hb_homeboard_settings)}, more -> {
                            if (more == 0) deleteTile(key);
                            else if (more == 1) airFor(key).refreshNow();
                            else homeSettings();
                        });
            }); return;
        }
        if(key.startsWith("widget:")){menu(getString(R.string.hb_aw_unknown),new String[]{getString(R.string.hb_delete_widget)},i->deleteTile(key));return;}
        App app = apps.get(key);
        String deleteLabel = allApps ? getString(R.string.hb_hide_from_all_apps) : getString(R.string.hb_remove_from_home);
        menu(app == null ? getString(R.string.hb_unavailable_app) : app.label, getString(R.string.hb_choose_an_action_for_this_app),
                new String[]{getString(R.string.hb_move), layout.isFavorite(key) ? getString(R.string.hb_unfavorite) : getString(R.string.hb_favorite), deleteLabel, getString(R.string.hb_more)}, choice -> {
                    switch (choice) {
                        case 0: startMove(key); break;
                        case 1:
                            if (layout.isFavorite(key)) { layout.removeFavorite(key);layout.widgets.remove(key); layout.homePositions.remove(key); }
                            else layout.addFavorite(key);
                            save(layout.isFavorite(key) ? getString(R.string.hb_added_app_to_home) : getString(R.string.hb_removed_app_from_home));
                            render(0); break;
                        case 2: deleteTile(key); break;
                        default: menu(getString(R.string.hb_more_39) + (app == null ? getString(R.string.hb_hint_app) : app.label), getString(R.string.hb_android_owns_app_and_system_actions),
                                new String[]{getString(R.string.hb_uninstall_app), getString(R.string.hb_app_info), getString(R.string.hb_homeboard_settings)}, more -> {
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
        String label = HomeLayout.TIMER.equals(key) ? getString(R.string.hb_timer_title) : HomeLayout.FAVORITES.equals(key) ? getString(R.string.hb_fav_title) : HomeLayout.isRainWidget(key) ? getString(R.string.hb_rain_rain) : HomeLayout.isCalendarWidget(key) ? "calendar widget" : isAirWidgetKey(key) ? getString(R.string.hb_air_quality_panel) : HomeLayout.isWorldClockWidget(key) ? getString(R.string.hb_wc_title) : HomeLayout.isGroceryWidget(key) ? getString(R.string.hb_grocery_title) : app == null ? "app shortcut" : app.label;
        layout.delete(key, dashboard);
        save(dashboard ? getString(R.string.hb_hint_removed) + label + getString(R.string.hb_hint_from_home) : getString(R.string.hb_hint_hid) + label + getString(R.string.hb_hint_from_all_apps));
        if (dashboard && !layout.widgets.isEmpty()) {
            homePage = Math.min(homePage, homePageCount(layout.widgets) - 1);
            if (!layout.widgets.contains(focused)) focused = layout.widgets.isEmpty() ? "" : layout.widgets.get(0);
        }
        render(0);
    }
    private void uninstall(String key) {
        App app = apps.get(key);
        if (app == null) { toast(getString(R.string.hb_app_is_unavailable)); return; }
        if ((app.info.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
            menu(getString(R.string.hb_system_app), getString(R.string.hb_android_does_not_allow_uninstalling_this_app_here), new String[]{getString(R.string.hb_keep_app), getString(R.string.hb_open_app_info)}, choice -> {
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
        moving = false; preview = null; previewHomePositions = null; save(getString(R.string.hb_moved_item)); render(0);
    }
    private void cancelMove() {
        moving = false; preview = null; previewHomePositions = null;
        if (allApps) { int i = appItems().indexOf(focused); appsPage = Math.max(0, i / appPageSize()); }
        else homePage = homePageOf(focused, layout.widgets);
        render(0);
    }
    private PhoneEditor phoneEditor;
    private boolean phonePanelOpen;
    private TextView phoneIndicator;
    private HomeLayout phonePreviewBefore;
    private String phonePreviewFocus,phonePreviewHomeFocus;
    private int phonePreviewPage;
    private boolean phonePreviewAllApps;
    private JSONObject phonePreviewAir;
    private final Runnable phonePreviewTimeout=()->phoneCancelPreview();
    boolean phoneForeground(){return active&&!closed;}
    boolean phoneCanPair(){return phonePanelOpen||!phoneBusy();}
    private boolean phoneBusy(){for(AirQualityCard card:airWidgets.values())if(!card.ready())return true;return layoutReadOnly||(androidWidgets!=null&&androidWidgets.busy())||backUndo.down()||!active||closed||overlay!=null||clockEditor!=null||groceryEditor!=null||moving||resizing||firstPageBefore!=null||favoriteOrderPreview!=null||expandedWidget!=null||undoGroupStart!=null;}
    String phoneName(){String name=android.provider.Settings.Global.getString(getContentResolver(),"device_name");if(name==null||name.isBlank())name=android.os.Build.MODEL;String id=android.provider.Settings.Secure.getString(getContentResolver(),android.provider.Settings.Secure.ANDROID_ID);return name+" · "+(id==null?"TV":id.substring(Math.max(0,id.length()-4)));}
    void phoneMessage(int id){toast(getString(id));}
    void phoneDismiss(){dismissMenu();activeMenu=null;menuStack.clear();}
    void phonePanel(String title,String message,android.graphics.Bitmap bitmap,String[] choices,java.util.function.IntConsumer action){
        dismissMenu();cancelOk();activeMenu=null;menuStack.clear();phonePanelOpen=true;
        surface.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        overlay=new FrameLayout(this);overlay.setBackgroundColor(paletteColors.scrim);
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(24),dp(18),dp(24),dp(18));panel.setBackground(shape(paletteColors.raised,paletteColors.edge,1));
        panel.addView(text(title,23));TextView detail=text(message,17);detail.setPadding(0,dp(8),0,dp(8));panel.addView(detail);
        if(bitmap!=null){ImageView qr=new ImageView(this);qr.setImageBitmap(bitmap);qr.setContentDescription(getString(R.string.hb_phone_scan));panel.addView(qr,new LinearLayout.LayoutParams(dp(160),dp(160)));}
        LinearLayout buttons=new LinearLayout(this);View first=null;
        for(int i=0;i<choices.length;i++){final int index=i;View button=infoButton(choices[i],()->action.accept(index));buttons.addView(button,new LinearLayout.LayoutParams(0,dp(48),1));if(first==null)first=button;}
        panel.addView(buttons);overlay.addView(panel,new FrameLayout.LayoutParams(Math.min(dp(740),dashboardWidthPixels()-dp(48)),-2,Gravity.CENTER));root.addView(overlay,new FrameLayout.LayoutParams(-1,-1));updateUndoHint();if(first!=null)first.requestFocus();
    }
    void phoneConnectionChanged(boolean connected){
        if(root==null)return;if(phoneIndicator!=null){root.removeView(phoneIndicator);phoneIndicator=null;}
        if(connected){phoneIndicator=text("●",12);phoneIndicator.setContentDescription(getString(R.string.hb_phone_connected));phoneIndicator.setPadding(0,0,0,0);FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(14),dp(16),Gravity.TOP|Gravity.RIGHT);lp.topMargin=dp(16);lp.rightMargin=dp(110);root.addView(phoneIndicator,lp);}
        if(!connected&&phonePanelOpen)phoneDismiss();
    }
    JSONObject phoneIcon(JSONObject body)throws Exception {
        String key=PhoneConfiguration.bounded(body,"key",512);App app=apps.get(key);
        JSONObject result=new JSONObject();if(app==null)return result;
        try{
            Drawable icon=app.info.loadIcon(getPackageManager()).mutate();
            android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(64,64,android.graphics.Bitmap.Config.ARGB_8888);
            icon.setBounds(0,0,64,64);icon.draw(new android.graphics.Canvas(bitmap));
            java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,bytes);bitmap.recycle();
            result.put("icon","data:image/png;base64,"+android.util.Base64.encodeToString(bytes.toByteArray(),android.util.Base64.NO_WRAP));
        }catch(RuntimeException unavailable){/* Missing artwork keeps the readable monogram. */}
        return result;
    }
    JSONObject phoneState()throws Exception {
        HomeLayout saved=HomeLayout.parse(preferences.getString("layout",layout.encode()));
        for(String key:saved.widgets)if(isAirWidgetKey(key)){ AirQualityCard card=airFor(key); if(card.ready())card.configurationSnapshot(); }
        JSONObject air=PhoneConfigurationStore.air(preferences);
        int cols=saved.homeColumns==0?homeGridFitColumnsFor(saved.homeAppSize,saved.dashboardScale):saved.homeColumns,rows=saved.homeRows==0?homeGridFitRowsFor(saved.homeAppSize,saved.dashboardScale):saved.homeRows;
        JSONArray catalog=new JSONArray();for(App app:apps.values())catalog.put(new JSONObject().put("key",app.key).put("name",app.label));
        List<UndoEntry> undo=readUndoHistory();
        JSONObject state=new JSONObject().put("timers",KitchenTimerStore.snapshot(this)).put("tv",phoneName()).put("tvId",PhoneConfigurationStore.tvId(preferences)).put("instances",PhoneConfigurationStore.initializeInstances(preferences,saved)).put("revision",preferences.getLong(PhoneConfigurationStore.REVISION,0))
                .put("providers",phoneProviders()).put("draft",androidWidgets.presentation(PhoneConfiguration.snapshot(saved,air,cols,rows))).put("apps",catalog).put("busy",phoneBusy()||phonePreviewBefore!=null)
                .put("undo",undo.isEmpty()?"":undo.get(0).label).put("language",getResources().getConfiguration().getLocales().get(0).getLanguage());
        JSONArray stations=new JSONArray();String[] ids={"114","119","122","70","65"},names=getResources().getStringArray(R.array.hb_station_names);
        for(int i=0;i<ids.length;i++)stations.put(new JSONObject().put("id",ids[i]).put("name",names[i]));
        return state.put("worldClockDefaults",new JSONObject().put("settings",new WorldClockSettings().json()).put("width",WorldClockSettings.DEFAULT_WIDTH).put("height",WorldClockSettings.DEFAULT_HEIGHT)).put("stations",stations).put("worldClockCatalog",WorldClockCatalog.json()).put("clockNow",java.time.Instant.now().toString()).put("clockZone",java.time.ZoneId.systemDefault().getId()).put("clock24Hour",android.text.format.DateFormat.is24HourFormat(this));
    }
    private void phoneRequireRevision(JSONObject body)throws Exception{
        if(body.getLong("revision")!=preferences.getLong(PhoneConfigurationStore.REVISION,0))throw new PhoneConfiguration.Invalid("revision",getString(R.string.hb_phone_conflict));
    }
    private String phoneOperationId(JSONObject body)throws Exception{String id=body.getString("operation");if(!id.matches("[A-Za-z0-9_-]{16,80}"))throw new PhoneConfiguration.Invalid("operation","Invalid operation identity");return id;}
    JSONObject phoneTimers(JSONObject body){return KitchenTimerStore.command(this,body);}
    JSONObject phoneTimerState(){return KitchenTimerStore.snapshot(this);}
    JSONObject phoneOperation(JSONObject body)throws Exception{String id=phoneOperationId(body),result=PhoneConfigurationStore.result(preferences,id);return result.isEmpty()?new JSONObject().put("status","unknown"):new JSONObject(result).put("status","committed");}
    private String phoneHash(String value)throws Exception{return android.util.Base64.encodeToString(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)),android.util.Base64.NO_WRAP);}
    JSONObject phoneApply(JSONObject body)throws Exception{
        String id=phoneOperationId(body),hash=phoneHash(body.getJSONObject("draft").toString()),previous=PhoneConfigurationStore.result(preferences,id);
        if(!previous.isEmpty()){if(!new JSONObject(previous).getString("hash").equals(hash))throw new PhoneConfiguration.Invalid("operation","Operation already used for a different change");return phoneState().put("appliedRevision",new JSONObject(previous).getLong("revision"));}
        if(phonePreviewBefore!=null)phoneCancelPreview();
        phoneRequireRevision(body);if(phoneBusy())throw new PhoneConfiguration.Invalid("busy",getString(R.string.hb_phone_busy));
        PhoneConfiguration.Result next=PhoneConfiguration.validate(body.getJSONObject("draft"),layout,PhoneConfigurationStore.air(preferences),apps.keySet());
        androidWidgets.validateDraft(layout,next.layout);
        JSONObject before=PhoneConfigurationStore.aggregate(preferences);
        boolean changed=!layout.encode().equals(next.layout.encode())||!PhoneConfigurationStore.air(preferences).toString().equals(next.air.toString());
        long rev=preferences.getLong(PhoneConfigurationStore.REVISION,0)+(changed?1:0);
        JSONObject outcome=new JSONObject().put("revision",rev).put("hash",hash);
        if(changed){JSONArray undo=storedUndoEntries();if(undo.length()>=MAX_UNDO_ENTRIES){JSONArray bounded=new JSONArray();for(int i=undo.length()-MAX_UNDO_ENTRIES+1;i<undo.length();i++)bounded.put(undo.getJSONObject(i));undo=bounded;}
            undo.put(new JSONObject().put("kind","phone").put("key","").put("state",before.toString()).put("label",getString(R.string.hb_phone_change)));
            PhoneConfigurationStore.commit(preferences,next.layout,next.air,undo,id,outcome.toString());phoneReload();showUndoBanner(getString(R.string.hb_phone_change));
        }else{JSONObject operations=PhoneConfigurationStore.operations(preferences);if(operations.length()>=32)operations.remove(operations.keys().next());operations.put(id,outcome.toString());if(!preferences.edit().putString(PhoneConfigurationStore.OPERATIONS,operations.toString()).commit())throw new IllegalStateException("Save failed");}
        return phoneState().put("appliedRevision",rev);
    }
    JSONObject phoneUndo(JSONObject body)throws Exception{
        String id=phoneOperationId(body),previous=PhoneConfigurationStore.result(preferences,id);
        if(!previous.isEmpty())return phoneState();phoneRequireRevision(body);
        if(phoneBusy()||phonePreviewBefore!=null)throw new PhoneConfiguration.Invalid("busy",getString(R.string.hb_phone_busy));
        List<UndoEntry> entries=readUndoHistory();if(entries.isEmpty())return phoneState();UndoEntry entry=entries.get(0);
        HomeLayout restored=layout;JSONObject air=PhoneConfigurationStore.air(preferences);
        switch(entry.kind){
            case "phone":JSONObject aggregate=new JSONObject(entry.state);restored=HomeLayout.restore(aggregate.getJSONObject("layout").toString(),layout.headerClock);air=aggregate.getJSONObject("air");break;
            case "layout":restored=HomeLayout.restore(entry.state,layout.headerClock);break;
            case "home-app-size":restored=HomeLayout.restore(new JSONObject(entry.state).getString("layout"),layout.headerClock);break;
            case "air":air.put(entry.key,new JSONObject(entry.state));break;
            default:throw new PhoneConfiguration.Invalid("undo","Undo this earlier system change with the TV controls");
        }
        JSONArray stored=storedUndoEntries(),remaining=new JSONArray();for(int i=0;i<stored.length()-1;i++)remaining.put(stored.getJSONObject(i));
        JSONObject outcome=new JSONObject().put("revision",preferences.getLong(PhoneConfigurationStore.REVISION,0)+1).put("hash","undo");
        PhoneConfigurationStore.commit(preferences,restored,air,remaining,id,outcome.toString());phoneReload();showUndoBanner(getString(R.string.hb_undid)+entry.label,false);return phoneState();
    }
    private void phoneReload(){
        if(androidWidgets!=null)androidWidgets.reconcile();
        for(AirQualityCard a:airWidgets.values())a.close();airWidgets.clear();for(RainWidgetController r:rainWidgets.values())r.close();rainWidgets.clear();
        try{layout=HomeLayout.parse(preferences.getString("layout",layout.encode()));}catch(Exception e){throw new IllegalStateException(e);}
        lastSavedLayout=layout.encode();if(!layout.widgets.contains(homeFocus))homeFocus=layout.widgets.isEmpty()?"":layout.widgets.get(0);if(!allApps){focused=homeFocus;homePage=homeFocus.isEmpty()?0:homePageOf(homeFocus,layout.widgets);}render(0);
    }
    JSONObject phonePreview(JSONObject body)throws Exception{
        if(phonePreviewBefore!=null)phoneCancelPreview();phoneRequireRevision(body);
        if(phoneBusy())throw new PhoneConfiguration.Invalid("busy",getString(R.string.hb_phone_busy));
        PhoneConfiguration.Result draft=PhoneConfiguration.validate(body.getJSONObject("draft"),layout,PhoneConfigurationStore.air(preferences),apps.keySet());
        androidWidgets.validateDraft(layout,draft.layout);
        phonePreviewBefore=layout;phonePreviewAir=draft.air;phonePreviewFocus=focused;phonePreviewHomeFocus=homeFocus;phonePreviewPage=homePage;phonePreviewAllApps=allApps;
        layout=draft.layout;allApps=false;homePage=0;focused=layout.widgets.isEmpty()?"":layout.widgets.get(0);render(0);
        if(footer!=null)footer.setText(getString(R.string.hb_phone_preview));handler.postDelayed(phonePreviewTimeout,30_000);return new JSONObject().put("ok",true);
    }
    void phoneCancelPreview(){
        if(phonePreviewBefore==null)return;handler.removeCallbacks(phonePreviewTimeout);layout=phonePreviewBefore;phonePreviewBefore=null;phonePreviewAir=null;
        focused=phonePreviewFocus;homeFocus=phonePreviewHomeFocus;homePage=phonePreviewPage;allApps=phonePreviewAllApps;render(0);
    }
    private View phonePreviewTile(String key){
        String value=getString(R.string.hb_phone_preview_data)+"\n";
        if(HomeLayout.isRainWidget(key)){RainSettings r=layout.rainSettings.get(key);value+=getString(R.string.hb_rain_rain)+" · "+r.displayName()+"\n"+getString(R.string.hb_rain_window,r.hours)+" · "+getString(R.string.hb_rain_following,r.days);}
        else {JSONObject a=phonePreviewAir.optJSONObject(key);value+=getString(R.string.hb_air_quality_panel)+"\n"+(a==null?"":a.optString("source")+" · "+a.optString("location-name")+" · "+a.optString("station"));}
        TextView tile=text(value,16);tile.setGravity(Gravity.CENTER);tile.setBackground(shape(paletteColors.surface,paletteColors.edge,1));return tile;
    }

    private void homeSettings() {
        // A dashboard saved by a newer Homeboard: only Device settings and About, so Android's Settings, which has no
        // other on-screen route since the top row lost its gear, is never out of reach.
        List<HomeSettingsMenu.Entry> entries = HomeSettingsMenu.entries(layoutReadOnly);
        String[] labels = new String[entries.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = homeSettingsLabel(entries.get(i));
        menu(getString(R.string.hb_home_settings), getString(R.string.hb_choose_an_area_to_customize), labels, choice -> {
            if (choice < 0 || choice >= entries.size()) return;
            switch (entries.get(choice)) {
                case WIDGETS_AND_RESTORE: widgetsAndRestore(); break;
                case APPEARANCE: appearanceSettings(); break;
                case SCREENSAVER: screensaverSettings(); break;
                case DEVICE_SETTINGS: open(new Intent(Settings.ACTION_SETTINGS)); break;
                case HOME_BEHAVIOR: homeBehaviorSettings(); break;
                case LANGUAGE: languageSettings(); break;
                case UNDO_HISTORY: openUndoHistory(); break;
                case HOME_BUTTON: homeButtonSettings(); break;
                case ABOUT: aboutHomeboard(); break;
                case PHONE: if(phoneEditor==null)phoneEditor=new PhoneEditor(this);phoneEditor.open();break;
                case CLEANUP: cleanupInstructions(); break;
                default: break;
            }
        });
    }
    private String homeSettingsLabel(HomeSettingsMenu.Entry entry) {
        switch (entry) {
            case WIDGETS_AND_RESTORE: return getString(R.string.hb_widgets_restore);
            case APPEARANCE: return getString(R.string.hb_appearance);
            case SCREENSAVER: return getString(R.string.hb_screensaver);
            case DEVICE_SETTINGS: return getString(R.string.hb_device_settings);
            case HOME_BEHAVIOR: return getString(R.string.hb_home_behavior);
            case LANGUAGE: return getString(R.string.hb_language);
            case UNDO_HISTORY: return getString(R.string.hb_undo_history);
            case HOME_BUTTON: return homeButtonMenuLabel();
            case ABOUT: return getString(R.string.hb_about_homeboard_tv);
            case PHONE: return getString(R.string.hb_phone_title);
            case CLEANUP: return getString(R.string.hb_cleanup_title);
            default: throw new IllegalArgumentException(entry.name());
        }
    }

    // Screensaver (docs/plans/2026-11-20-screensavers.md, Stages 1 and 3): Home settings → Screensaver, its Options,
    // Preview through ScreensaverActivity, the Home idle timer, and Use everywhere (EverywhereScreensaver through
    // SystemScreensaver). Every change saves at once with Undo; a change to Android's setting carries its previous value.
    private static final int REQUEST_SCREENSAVER_PREVIEW = 7404, REQUEST_APP_SCREENSAVER = 7405;
    /** The request an app's preview screen was started with while it is up (Preview or REQUEST_APP_SCREENSAVER); 0 when none. */
    private int appScreenRequest;
    private String appScreenEntry, appScreenMode;
    /** A ringing timer closed an app's preview screen: Home opens the timer screen when it is back in front. */
    private boolean timersAfterAppScreen;
    /**
     * A timer or alarm started ringing (KitchenTimerSound's ACTION_END). Homeboard's own screensaver ends itself; an
     * app's preview screen that Home started is closed here, so Home comes back with the timer screen open.
     */
    private final BroadcastReceiver screensaverAlarm = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (appScreenRequest == 0) return;
            timersAfterAppScreen = true;
            try { finishActivity(appScreenRequest); }
            catch (RuntimeException refused) { android.util.Log.w(ScreensaverActivity.TAG, "could not close the app screensaver: " + refused); }
        }
    };
    /** The chooser's browsing state: browsed entry, scroll anchor and last action (ScreensaverPicker.State). */
    private final ScreensaverPicker.State screensaverPicker = new ScreensaverPicker.State();
    /** The chooser's own menu, so a key, a package change or a pause can tell it is the one showing. */
    private MenuSpec screensaverMenu;
    /** The chooser's views while it is drawn; replaced on every full draw. */
    private androidx.recyclerview.widget.RecyclerView screensaverList;
    private LinearLayout screensaverPane;
    private TextView screensaverCount;
    private View screensaverThumb;
    private String screensaverPaneEntry;
    /** The chooser state at the last pause while it was open, saved only if Android recreates the Activity. */
    private ScreensaverPicker.State screensaverPausedPicker;
    /** The control to focus after a redraw, by tag; null focuses the browsed row. */
    private String screensaverFocusTag;
    /** Set while a Preview is up: the entry, and whether its Options screen was open. */
    private String returningToScreensaver, screensaverAccepted;
    private boolean screensaverOptionsOpen;
    private View screensaverFirstAction;
    private EverywhereScreensaver everywhere;
    /** Built-ins plus the installed Android screensaver apps, read again each time the Screensaver screen opens. */
    private List<ScreensaverRegistry.Entry> screensaverEntries = ScreensaverRegistry.BUILT_IN;
    /** Read fresh by each screensaver screen draw: Android's current setting, timeout, permission and the kept original. */
    private EverywhereScreensaver.Status screensaverStatus;
    /** The recovery notice ("Android did not accept the change…") and what its Retry repeats; null when none. */
    private String screensaverFailure;
    private Runnable screensaverRetry;
    private boolean screensaverSetupChecked, screensaverShowQr;
    private static final String UNDO_SYSTEM_SCREENSAVER = "screensaver-system";
    /** The saved choice whose disappearance Home last announced; cleared when it is back or replaced. */
    private static final String PREF_SCREENSAVER_MISSING_NOTICED = "screensaver-missing-noticed";
    private long lastHomeInput = SystemClock.uptimeMillis();
    private final Runnable screensaverDue = this::scheduleScreensaver;

    private void noteHomeInput() {
        lastHomeInput = SystemClock.uptimeMillis();
        // Decide after the key is handled, so a key that closes a menu starts the timer from that moment.
        handler.removeCallbacks(screensaverDue); handler.post(screensaverDue);
    }
    /** The person is using Home: any menu, overlay, editor, expanded widget, move or resize holds the timer off. */
    private boolean homeBusy() {
        return overlay != null || expandedWidget != null || clockEditor != null || groceryEditor != null || moving || resizing
                || firstPageBefore != null || favoriteOrderPreview != null || gridSelector != null || adjustmentArm != null
                || adjustmentCancel != null || favoritesBrowser != null || phonePanelOpen || phonePreviewBefore != null || backUndo.down();
    }
    /** One delayed callback, rebased on the last input; "Android decides" posts nothing. */
    private void scheduleScreensaver() {
        handler.removeCallbacks(screensaverDue);
        if (layout == null || root == null) return;
        // One timer in charge: while Android starts the screensaver in every app, Homeboard's own Home timer is off.
        int idle = ScreensaverRules.homeIdleMinutes(everywhere().managed(), layout.screensaver.choice, layout.screensaver.idleMinutes);
        ScreensaverIdle.Decision next = ScreensaverIdle.decide(SystemClock.uptimeMillis(), lastHomeInput,
                idle, active && !closed && hasWindowFocus(), homeBusy());
        if (next.kind == ScreensaverIdle.Kind.START) {
            try { playScreensaver(ScreensaverActivity.MODE_IDLE); }
            catch (RuntimeException unavailable) { lastHomeInput = SystemClock.uptimeMillis(); }
        } else if (next.kind == ScreensaverIdle.Kind.WAIT) handler.postDelayed(screensaverDue, next.delayMs);
    }

    /**
     * Home idle (MODE_IDLE) and the remote's action (MODE_NOW): the chosen app's own preview screen when it declares
     * one (ScreensaverRules.homePlayer), otherwise Homeboard's ScreensaverActivity, which plays the chosen built-in or
     * the default slot. An app screen that cannot start falls back to Homeboard's. Throws only when that fails too.
     */
    private void playScreensaver(String mode) {
        if (layout == null) return;
        ScreensaverRegistry.Entry chosen = ScreensaverRegistry.resolve(layout.screensaver.choice, refreshScreensaverEntries());
        if (ScreensaverRules.homePlayer(chosen) == ScreensaverRules.Player.APP_SCREEN && startAppScreen(chosen, REQUEST_APP_SCREENSAVER, mode)) return;
        startActivity(ScreensaverActivity.intent(this, mode, null));
    }
    /**
     * Starts an app's preview screen for a result, so it opens in Home's own task, above Home: when it ends, Home is
     * underneath, and a ringing timer closes it with finishActivity (screensaverAlarm), which needs no permission to
     * start an activity from the background. False when it cannot start (missing, not exported, refused).
     */
    private boolean startAppScreen(ScreensaverRegistry.Entry entry, int request, String mode) {
        Intent intent = SystemScreensaver.appScreenIntent(entry.screen);
        if (intent == null) return false;
        try { startActivityForResult(intent, request); }
        catch (RuntimeException unavailable) {
            android.util.Log.w(ScreensaverActivity.TAG, "app screensaver did not start entry=" + entry.id + ": " + unavailable);
            return false;
        }
        appScreenRequest = request; appScreenEntry = entry.id; appScreenMode = mode;
        android.util.Log.i(ScreensaverActivity.TAG, "screensaver started mode=" + mode + " entry=" + entry.id + " player=app");
        return true;
    }
    private void appScreenEnded(String reason) {
        android.util.Log.i(ScreensaverActivity.TAG, "screensaver stopped mode=" + appScreenMode + " entry=" + appScreenEntry + " player=app reason=" + reason);
        appScreenRequest = 0; appScreenEntry = null; appScreenMode = null;
    }

    private EverywhereScreensaver everywhere() {
        if (everywhere == null) everywhere = SystemScreensaver.everywhere(this);
        return everywhere;
    }
    private List<ScreensaverRegistry.Entry> refreshScreensaverEntries() {
        ScreensaverGateway gateway = everywhere().gateway();
        List<ScreensaverRegistry.Entry> installed = ScreensaverRegistry.entries(gateway.installedDreams(), gateway.ownComponent());
        // A saved app that is no longer installed stays visible as an unavailable row (checkChosenScreensaverApp).
        screensaverEntries = layout == null ? installed : ScreensaverRegistry.withSaved(installed, layout.screensaver.choice, layout.screensaver.choiceName);
        return screensaverEntries;
    }
    /** The chosen entry as the list shows it: the default slot when the chosen app is no longer installed. */
    private ScreensaverRegistry.Entry chosenScreensaver() { return ScreensaverRegistry.resolve(layout.screensaver.choice, screensaverEntries); }
    /**
     * Home settings → Screensaver. Opened from Home settings it stacks on that menu; opened by holding OK on Start
     * screensaver (D1) it stands alone, so Back returns to Home with the button focused.
     */
    private void screensaverSettings() {
        refreshScreensaverEntries(); screensaverFailure = null; screensaverRetry = null;
        String chosen = chosenScreensaver().id;
        // Fresh openings start at the saved choice; activeMenu is Home settings when opened from there.
        screensaverPicker.reset(chosen, Math.max(0, ScreensaverPicker.indexOf(chosen, screensaverEntries)), activeMenu != null);
        screensaverFocusTag = null;
        openScreensaverMenu();
    }
    private void openScreensaverMenu() {
        screensaverMenu = new MenuSpec(getString(R.string.hb_screensaver), this::drawScreensaver);
        openMenu(screensaverMenu);
    }
    /**
     * After a Preview or an app's settings screen (both pause Home, which closes its menus): the chooser again from
     * where it was opened, on the same entry, scroll position and action (→ Options when the Preview came from
     * there), then save an accepted built-in Preview with Undo.
     */
    private void reopenScreensaver(String entry) {
        if (!active || layoutReadOnly) return;
        boolean options = screensaverOptionsOpen; String accepted = screensaverAccepted; screensaverAccepted = null;
        refreshScreensaverEntries();
        if (screensaverPicker.fromHomeSettings) homeSettings();
        invokingMenuAction = true;
        try {
            if (entry != null) screensaverPicker.browsed = entry;
            screensaverPicker.returning = screensaverPicker.action != null;
            openScreensaverMenu();
            if (options) openScreensaverOptions(entry, "opt:preview");
        } finally { invokingMenuAction = false; }
        if (accepted != null) useScreensaver(ScreensaverRegistry.entry(accepted), options ? "opt:preview" : "act:preview");
    }
    /** Use: on Home only before the one-time question; once Homeboard manages the TV, in every app with Undo. */
    private void useScreensaver(ScreensaverRegistry.Entry entry, String focusTag) {
        switch (ScreensaverRules.use(everywhere().managed(), entry.id, entry.playsOnHome())) {
            case APPLY_NOW: changeEverywhere(entry, false, focusTag); return;
            case EVERYWHERE_SCREEN: openEverywhere(entry.id); return;
            default:
                layout.screensaver.choice = entry.id; layout.screensaver.choiceName = entry.app() ? entry.name : "";
                save(getString(R.string.hb_ss_undo_choice, entry.title(this)));
                refreshScreensaverEntries();   // a replaced unavailable app's row goes away
                redrawScreensaver(focusTag);
        }
    }
    /**
     * Makes entry this TV's screensaver in every app. first: the asked-once Confirm, which also keeps the original.
     * Android's values change first and are read back; only then are the choice and the Home timer (off: one timer
     * in charge) saved, with an Undo entry carrying the previous Android values. A failure changes nothing here.
     */
    private void changeEverywhere(ScreensaverRegistry.Entry entry, boolean first, String focusTag) {
        if (layoutReadOnly) { toast(getString(R.string.hb_aw_newer_layout)); return; }
        EverywhereScreensaver system = everywhere();
        String layoutBefore = lastSavedLayout; boolean managedBefore = system.managed();
        String component = ScreensaverRules.componentFor(entry.id, system.gateway().ownComponent());
        EverywhereScreensaver.Outcome outcome = first ? system.manage(component) : system.apply(component);
        if (!outcome.ok()) { screensaverFailed(outcome, () -> changeEverywhere(entry, first, focusTag)); return; }
        screensaverFailure = null; screensaverRetry = null;
        layout.screensaver.choice = entry.id; layout.screensaver.choiceName = entry.app() ? entry.name : "";
        layout.screensaver.idleMinutes = ScreensaverSettings.ANDROID_DECIDES;
        if (!saveSystemScreensaver(getString(R.string.hb_ss_undo_tv, entry.title(this)), layoutBefore, outcome.before, managedBefore))
            system.undoTo(outcome.before, managedBefore, ScreensaverRegistry.installed(screensaverEntries));
        redrawScreensaver(focusTag);
    }
    /** Restore the TV's original, with Undo; Home keeps a Homeboard choice and falls back to the default slot from an app. */
    private void restoreEverywhere() {
        if (layoutReadOnly) { toast(getString(R.string.hb_aw_newer_layout)); return; }
        EverywhereScreensaver system = everywhere();
        ScreensaverValues original = system.original();
        String layoutBefore = lastSavedLayout; boolean managedBefore = system.managed();
        EverywhereScreensaver.Outcome outcome = system.restore();
        if (!outcome.ok()) { screensaverFailed(outcome, this::restoreEverywhere); return; }
        screensaverFailure = null; screensaverRetry = null;
        if (ScreensaverRules.isApp(layout.screensaver.choice)) { layout.screensaver.choice = ScreensaverRegistry.fallback().id; layout.screensaver.choiceName = ""; }
        String name = screensaverName(original == null ? null : original.component());
        if (!saveSystemScreensaver(getString(R.string.hb_ss_undo_restore, name), layoutBefore, outcome.before, managedBefore))
            system.undoTo(outcome.before, managedBefore, ScreensaverRegistry.installed(screensaverEntries));
        backMenu();
    }
    /** Saves the layout with an Undo entry that also carries Android's previous values and managed flag. */
    private boolean saveSystemScreensaver(String label, String layoutBefore, ScreensaverValues androidBefore, boolean managedBefore) {
        try {
            JSONObject state = new JSONObject().put("layout", layoutBefore).put("android", androidBefore.json()).put("managed", managedBefore);
            JSONArray undo = appendedUndo(storedUndoEntries(), UNDO_SYSTEM_SCREENSAVER, "", state.toString(), label);
            PhoneConfigurationStore.commit(preferences, layout, PhoneConfigurationStore.air(preferences), undo, null, null);
            lastSavedLayout = layout.encode();
            if (androidWidgets != null) androidWidgets.reconcile();
            showUndoBanner(label);
            return true;
        } catch (Exception failed) {
            try { layout = HomeLayout.parse(lastSavedLayout); } catch (Exception ignored) { }
            toast(getString(R.string.hb_configuration_save_failed));
            return false;
        }
    }
    /** The recovery notice: what Android refused and what is still in use. Its Retry repeats the change. */
    private void screensaverFailed(EverywhereScreensaver.Outcome outcome, Runnable retry) {
        screensaverFailure = outcome.result == EverywhereScreensaver.Result.UNREADABLE ? getString(R.string.hb_ss_unreadable)
                : getString(R.string.hb_ss_refused, screensaverName(outcome.now));
        screensaverRetry = retry;
        if (activeMenu != null && activeMenu.panelRenderer != null) redrawScreensaver("fail:retry");
        else showUndoBanner(screensaverFailure, false);
    }
    /**
     * On Home: is the chosen Android screensaver app still installed (ScreensaverRules.missing)?
     * Not managed: the saved choice is kept unchanged and nothing is saved; Home idle, the Start rule and Show now
     * play the default slot meanwhile, the Screensaver list shows the app as an unavailable row, and a reinstall
     * works again with no action. The notice is shown once per disappearance (the announced choice is kept in
     * preferences, outside the layout and its Undo). docs/plans/2026-11-26-screensaver-browsing.md, edge states.
     * Managed: unchanged from before. Android's setting is restored and the choice falls back to the default slot,
     * because what Android does with a screensaver component that is no longer installed has not been tested.
     */
    private void checkChosenScreensaverApp() {
        if (layout == null || layoutReadOnly) return;
        String choice = layout.screensaver.choice;
        boolean managed = everywhere().managed();
        boolean installed = !ScreensaverRules.isApp(choice) || ScreensaverRegistry.installed(refreshScreensaverEntries()).contains(choice);
        String noticed = preferences.getString(PREF_SCREENSAVER_MISSING_NOTICED, null);
        ScreensaverRules.Missing missing = ScreensaverRules.missing(choice, installed, managed, noticed);
        String nextNoticed = ScreensaverRules.noticedAfter(missing, choice);
        if (!java.util.Objects.equals(noticed, nextNoticed)) preferences.edit().putString(PREF_SCREENSAVER_MISSING_NOTICED, nextNoticed).apply();
        if (missing == ScreensaverRules.Missing.NONE || missing == ScreensaverRules.Missing.KEEP) return;
        String gone = choice, goneName = layout.screensaver.choiceName.isEmpty() ? packageOf(gone) : layout.screensaver.choiceName;
        if (missing == ScreensaverRules.Missing.KEEP_AND_NOTICE) {
            showUndoBanner(getString(R.string.hb_ss_missing_home, goneName, ScreensaverRegistry.fallback().title(this)), false);
            return;
        }
        ScreensaverValues original = everywhere().original();
        EverywhereScreensaver.Outcome outcome = everywhere().fallBackIfGone(gone, ScreensaverRegistry.installed(screensaverEntries));
        if (outcome == null) return;
        ScreensaverRegistry.Entry fallback = ScreensaverRegistry.fallback();
        layout.screensaver.choice = fallback.id; layout.screensaver.choiceName = "";
        saveWithoutUndo();
        String home = fallback.title(this);
        showUndoBanner(outcome.ok() ? getString(R.string.hb_ss_gone, goneName, screensaverName(original == null ? null : original.component()), home)
                : getString(R.string.hb_ss_gone_failed, goneName, screensaverName(outcome.now), home), false);
    }
    private static String packageOf(String component) { int slash = component.indexOf('/'); return slash > 0 ? component.substring(0, slash) : component; }
    /** A screensaver_components value by name: an app's own label, Homeboard with its choice, or Android's default. */
    private String screensaverName(String raw) {
        String first = ScreensaverRules.first(raw);
        if (first == null) return getString(R.string.hb_ss_android_default);
        if (first.equals(everywhere().gateway().ownComponent())) return getString(R.string.hb_ss_homeboard_named, chosenHomeboardEntry().title(this));
        String label = ScreensaverRegistry.appLabel(first, screensaverEntries);
        if (label != null) return label;
        if (first.equals(layout.screensaver.choice) && !layout.screensaver.choiceName.isEmpty()) return layout.screensaver.choiceName;
        return packageOf(first);
    }
    private String screensaverName(ScreensaverGateway.Reading reading) {
        return reading == null || !reading.readable ? getString(R.string.hb_ss_unknown) : screensaverName(reading.value);
    }
    /** What ScreensaverDream plays: the chosen built-in, or the default slot when an app is chosen. */
    private ScreensaverRegistry.Entry chosenHomeboardEntry() { return ScreensaverRegistry.entry(layout.screensaver.choice); }
    private String screensaverTimeout(ScreensaverGateway.Reading timeout) {
        int minutes = ScreensaverRules.timeoutMinutes(timeout);
        if (minutes == ScreensaverRules.UNKNOWN) return getString(R.string.hb_ss_timeout_unknown);
        if (minutes == ScreensaverRules.NEVER) return getString(R.string.hb_ss_timeout_never);
        return getString(R.string.hb_ss_timeout_after, minutes);
    }
    /** "In other apps": Android's real screensaver and timeout, read through the gateway; unknown when unreadable. */
    private String screensaverOtherApps(EverywhereScreensaver.Status status) {
        switch (ScreensaverRules.otherLine(status.managed, status.component.readable)) {
            case UNKNOWN: return getString(R.string.hb_ss_other_unknown);
            case THE_SAME: return getString(R.string.hb_ss_other_same, screensaverName(status.component), screensaverTimeout(status.timeout));
            default: return getString(R.string.hb_ss_other_unchanged, screensaverName(status.component), screensaverTimeout(status.timeout));
        }
    }
    private void previewScreensaver(ScreensaverRegistry.Entry entry, boolean fromOptions) {
        returningToScreensaver = entry.id; screensaverOptionsOpen = fromOptions; screensaverAccepted = null;
        if (entry.app()) {
            // An app's own preview screen: Back ends it and the Screensaver screen comes back; nothing is saved.
            if (!entry.playsOnHome() || !startAppScreen(entry, REQUEST_SCREENSAVER_PREVIEW, ScreensaverActivity.MODE_PREVIEW)) {
                returningToScreensaver = null; toast(getString(R.string.hb_android_could_not_open_that_action));
            }
            return;
        }
        try { startActivityForResult(ScreensaverActivity.intent(this, ScreensaverActivity.MODE_PREVIEW, entry.id), REQUEST_SCREENSAVER_PREVIEW); }
        catch (RuntimeException unavailable) { returningToScreensaver = null; toast(getString(R.string.hb_android_could_not_open_that_action)); }
    }
    /** Redraws whichever screensaver screen is showing, keeping focus on the control that changed. */
    private void redrawScreensaver(String focusTag) {
        if (activeMenu == null || activeMenu.panelRenderer == null) return;
        screensaverFocusTag = focusTag; activeMenu.panelRenderer.run();
    }

    private LinearLayout screensaverPanel(String title, LinearLayout body) { return screensaverPanel(title, body, false); }
    /**
     * The panel the screensaver screens share: heading and Back on top, then the body. fill: the chooser, which
     * takes the measured dashboard height so its list scrolls inside it and its detail pane scrolls on its own.
     */
    private LinearLayout screensaverPanel(String title, LinearLayout body, boolean fill) {
        if (overlay != null) root.removeView(overlay);
        hideAppFocusHighlight();
        surface.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        overlay = new FrameLayout(this); overlay.setBackgroundColor(paletteColors.scrim);
        LinearLayout panel = new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(22), dp(14), dp(22), dp(12));
        panel.setBackground(shape(paletteColors.raised, paletteColors.edge, 2));
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading = text(title, 24); heading.setTypeface(null, Typeface.BOLD); heading.setSingleLine(true);
        header.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        TextView back = infoButton(getString(R.string.hb_back), this::backMenu); back.setTag("back");
        back.setPadding(dp(20), 0, dp(20), 0);
        header.addView(back, new LinearLayout.LayoutParams(-2, dp(42)));
        panel.addView(header);
        if (screensaverFailure != null) {
            // The recovery notice: what Android refused, what is unchanged, and Retry.
            LinearLayout notice = new LinearLayout(this); notice.setGravity(Gravity.CENTER_VERTICAL);
            notice.setPadding(dp(16), dp(8), dp(10), dp(8));
            notice.setBackground(shape(paletteColors.surface, 0xFFE07A7A, 2));
            TextView message = text(screensaverFailure, 16); message.setMaxLines(3);
            notice.addView(message, new LinearLayout.LayoutParams(0, -2, 1));
            TextView retry = infoButton(getString(R.string.hb_ss_retry), () -> {
                Runnable again = screensaverRetry; screensaverFailure = null; screensaverRetry = null;
                if (again != null) again.run(); else redrawScreensaver(null);
            });
            retry.setTag("fail:retry"); retry.setPadding(dp(18), 0, dp(18), 0);
            notice.addView(retry, new LinearLayout.LayoutParams(-2, dp(42)));
            LinearLayout.LayoutParams noticeLp = new LinearLayout.LayoutParams(-1, -2); noticeLp.topMargin = dp(8);
            panel.addView(notice, noticeLp);
        }
        LinearLayout.LayoutParams bodyLp = fill ? new LinearLayout.LayoutParams(-1, 0, 1) : new LinearLayout.LayoutParams(-1, -2);
        bodyLp.topMargin = dp(8);
        panel.addView(body, bodyLp);
        int width = Math.min(dp(1180), dashboardWidthPixels() - dp(40));
        int height = fill ? Math.max(dp(320), Math.min(dp(760), dashboardHeightPixels() - dp(40))) : -2;
        overlay.addView(panel, new FrameLayout.LayoutParams(width, height, Gravity.CENTER));
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1)); updateUndoHint();
        return panel;
    }
    private TextView muted(String value, float size) { TextView v = text(value, size); v.setTextColor(paletteColors.muted); return v; }
    private TextView bold(String value, float size) { TextView v = text(value, size); v.setTypeface(null, Typeface.BOLD); return v; }
    private View divider() { View line = new View(this); line.setBackgroundColor(paletteColors.edge); return line; }
    /** A small rounded label: "In use", "Built in". */
    private TextView badge(String value, int fill, int ink) {
        TextView v = text(value, 13); v.setTypeface(null, Typeface.BOLD); v.setTextColor(ink);
        v.setPadding(dp(9), dp(1), dp(9), dp(2)); UiBackgroundDrawable d = new UiBackgroundDrawable(fill);
        d.setCornerRadius(dp(10)); d.setStroke(dp(1), paletteColors.edge); v.setBackground(d); return v;
    }
    /** One segment of a segmented choice: filled when chosen, ringed when focused. */
    private TextView segment(String label, boolean chosen, String tag, Runnable action) {
        TextView v = text(label, 16); v.setGravity(Gravity.CENTER); v.setFocusable(true); v.setClickable(true);
        v.setPadding(dp(12), 0, dp(12), 0); v.setTag(tag); v.setSingleLine(true);
        if (chosen) { v.setTextColor(contrastingText(paletteColors.focus)); v.setTypeface(null, Typeface.BOLD); }
        StateListDrawable states = new UiBackgroundDrawable.States();
        states.addState(new int[]{android.R.attr.state_focused}, shape(chosen ? paletteColors.focus : paletteColors.raised, paletteColors.text, 3));
        states.addState(new int[]{}, shape(chosen ? paletteColors.focus : paletteColors.surface, chosen ? paletteColors.focus : paletteColors.edge, 1));
        v.setBackground(states);
        v.setContentDescription(label + (chosen ? ", selected" : ""));
        v.setOnClickListener(x -> action.run());
        return v;
    }
    private LinearLayout optionRow(LinearLayout parent, String label, boolean enabled) {
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setMinimumHeight(dp(44));
        TextView name = bold(label, 17); if (!enabled) name.setAlpha(0.45f);
        row.addView(name, new LinearLayout.LayoutParams(dp(210), -2));
        parent.addView(row, new LinearLayout.LayoutParams(-1, -2));
        return row;
    }
    private void addSegment(LinearLayout row, TextView segment) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(38)); lp.rightMargin = dp(6); row.addView(segment, lp);
    }
    private void focusScreensaver(View container, View fallback) {
        View target = screensaverFocusTag == null ? null : container.findViewWithTag(screensaverFocusTag);
        if (target == null || !target.isFocusable()) target = fallback;
        screensaverFocusTag = null;
        if (target != null) target.requestFocus();
    }
    /** Whether the Options box says that Android's screensaver replaces Homeboard's at Android's idle time. */
    private boolean screensaverAndroidFollows(EverywhereScreensaver.Status status) {
        return ScreensaverRules.androidFollows(status.managed, status.component, everywhere().gateway().ownComponent());
    }
    /** The same note on the main pane, only while Homeboard's own Home timer is set. */
    private boolean screensaverAndroidFollowsOnPane(EverywhereScreensaver.Status status) {
        return ScreensaverRules.androidFollowsOnPane(status.managed, layout.screensaver.choice, layout.screensaver.idleMinutes,
                status.component, everywhere().gateway().ownComponent());
    }
    private String screensaverStartsOnHome(EverywhereScreensaver.Status status) {
        int minutes = layout.screensaver.idleMinutes;
        switch (ScreensaverRules.homeLine(status.managed, layout.screensaver.choice, minutes)) {
            case ANDROID_STARTS: return getString(R.string.hb_ss_android_starts, screensaverTimeout(status.timeout));
            case NO_TIMER: return getString(R.string.hb_ss_no_timer);
            default: return getString(R.string.hb_ss_after, minutes);
        }
    }

    /**
     * Home settings → Screensaver (docs/plans/2026-11-26-screensaver-browsing.md): one continuous list on the left,
     * the browsed entry's preview, actions and timing on the right. Up and Down browse rows and update only the
     * detail pane; Right or OK enters the entry's actions; Left and Back return to the same row. A full draw (open,
     * return, redraw after a change) restores the browsed row, scroll position and last action from screensaverPicker.
     */
    private void drawScreensaver() {
        captureScreensaverAnchor();
        screensaverStatus = everywhere().status();
        List<ScreensaverRegistry.Entry> entries = screensaverEntries;
        int browsed = ScreensaverPicker.browsedRow(screensaverPicker.browsed, screensaverPicker.browsedIndex, entries);
        screensaverPicker.browsed = entries.get(browsed).id; screensaverPicker.browsedIndex = browsed;
        String homeId = chosenScreensaver().id;
        java.util.Map<String, ScreensaverPicker.Badge> badges = ScreensaverPicker.badges(homeId,
                ScreensaverPicker.otherAppsEntry(screensaverStatus.component, everywhere().gateway().ownComponent(), layout.screensaver.choice, entries));
        boolean narrow = dashboardWidthPixels() < dp(720);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(narrow ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        LinearLayout column = new LinearLayout(this); column.setOrientation(LinearLayout.VERTICAL);
        column.addView(bold(getString(R.string.hb_ss_list_heading), 19));
        FrameLayout listFrame = new FrameLayout(this);
        androidx.recyclerview.widget.RecyclerView list = new androidx.recyclerview.widget.RecyclerView(this);
        androidx.recyclerview.widget.LinearLayoutManager rows = new androidx.recyclerview.widget.LinearLayoutManager(this);
        list.setLayoutManager(rows); list.setItemAnimator(null); list.setVerticalScrollBarEnabled(false);
        list.setPadding(0, 0, dp(12), 0); list.setClipToPadding(false);
        list.setAdapter(new ScreensaverRows(entries, badges));
        listFrame.addView(list, new FrameLayout.LayoutParams(-1, -1));
        // A subtle, non-focusable position indicator; hidden when every row fits.
        FrameLayout track = new FrameLayout(this); track.setFocusable(false);
        View thumb = new View(this); thumb.setBackground(shape(paletteColors.muted, paletteColors.muted, 0)); thumb.setVisibility(View.INVISIBLE);
        track.addView(thumb, new FrameLayout.LayoutParams(-1, 0));
        listFrame.addView(track, new FrameLayout.LayoutParams(dp(4), -1, Gravity.END));
        LinearLayout.LayoutParams listLp = new LinearLayout.LayoutParams(-1, 0, 1); listLp.topMargin = dp(8);
        column.addView(listFrame, listLp);
        LinearLayout foot = new LinearLayout(this); foot.setGravity(Gravity.CENTER_VERTICAL);
        TextView count = muted("", 14); count.setSingleLine(true);
        foot.addView(count, new LinearLayout.LayoutParams(-2, -2));
        TextView hint = muted(getString(R.string.hb_ss_hint), 13); hint.setPadding(dp(14), 0, 0, 0); hint.setMaxLines(2);
        foot.addView(hint, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams footLp = new LinearLayout.LayoutParams(-1, -2); footLp.topMargin = dp(8); column.addView(foot, footLp);

        LinearLayout pane = new LinearLayout(this); pane.setOrientation(LinearLayout.VERTICAL);
        pane.setPadding(dp(18), dp(14), dp(18), dp(12));
        android.widget.ScrollView detail = new android.widget.ScrollView(this);
        detail.setBackground(shape(paletteColors.surface, paletteColors.edge, 1)); detail.setFillViewport(true);
        detail.addView(pane, new FrameLayout.LayoutParams(-1, -2));
        if (narrow) {
            body.addView(column, new LinearLayout.LayoutParams(-1, 0, 0.45f));
            LinearLayout.LayoutParams detailLp = new LinearLayout.LayoutParams(-1, 0, 0.55f); detailLp.topMargin = dp(12);
            body.addView(detail, detailLp);
        } else {
            body.addView(column, new LinearLayout.LayoutParams(0, -1, 0.42f));
            LinearLayout.LayoutParams detailLp = new LinearLayout.LayoutParams(0, -1, 0.58f); detailLp.leftMargin = dp(18);
            body.addView(detail, detailLp);
        }
        LinearLayout panel = screensaverPanel(getString(R.string.hb_home_settings) + " · " + getString(R.string.hb_screensaver), body, true);
        screensaverList = list; screensaverPane = pane; screensaverCount = count; screensaverThumb = thumb; screensaverPaneEntry = null;
        list.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            @Override public void onScrolled(androidx.recyclerview.widget.RecyclerView view, int dx, int dy) { updateScreensaverThumb(view, thumb); }
        });
        list.addOnLayoutChangeListener((v, l, t, r, b2, ol, ot, or, ob) -> updateScreensaverThumb(list, thumb));

        // Header Back (and Retry when a failure is shown): Down returns to the browsed row.
        View back = panel.findViewWithTag("back"), retry = panel.findViewWithTag("fail:retry");
        if (back != null) back.setOnKeyListener((v, key, event) -> {
            if (key != KeyEvent.KEYCODE_DPAD_DOWN) return false;
            if (event.getAction() == KeyEvent.ACTION_DOWN) { if (retry != null) retry.requestFocus(); else focusScreensaverRow(screensaverPicker.browsedIndex); }
            return true;
        });
        if (retry != null) retry.setOnKeyListener((v, key, event) -> {
            if (key != KeyEvent.KEYCODE_DPAD_DOWN) return false;
            if (event.getAction() == KeyEvent.ACTION_DOWN) focusScreensaverRow(screensaverPicker.browsedIndex);
            return true;
        });

        drawScreensaverPane(pane, entries.get(browsed));
        updateScreensaverCount();
        int anchor = ScreensaverPicker.indexOf(screensaverPicker.anchor, entries);
        if (anchor >= 0) rows.scrollToPositionWithOffset(anchor, screensaverPicker.anchorOffset);
        else rows.scrollToPosition(browsed);
        String explicit = screensaverFocusTag; screensaverFocusTag = null;
        String returnTo = screensaverPicker.takeReturnFocus();
        String tag = explicit != null ? explicit : returnTo;
        list.post(() -> {
            if (screensaverList != list) return;
            View target = tag == null ? null : panel.findViewWithTag(tag);
            if (target != null && target.isFocusable()) target.requestFocus();
            else focusScreensaverRow(screensaverPicker.browsedIndex);
        });
    }

    /** One chooser row per entry, with stable ids so a redraw or package refresh keeps rows and focus by identity. */
    private final class ScreensaverRows extends androidx.recyclerview.widget.RecyclerView.Adapter<ScreensaverRows.Row> {
        private final List<ScreensaverRegistry.Entry> entries;
        private final java.util.Map<String, ScreensaverPicker.Badge> badges;
        ScreensaverRows(List<ScreensaverRegistry.Entry> entries, java.util.Map<String, ScreensaverPicker.Badge> badges) {
            this.entries = entries; this.badges = badges; setHasStableIds(true);
        }
        final class Row extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            final TextView glyph, title, status; final LinearLayout badgeBox;
            Row(LinearLayout card, TextView glyph, TextView title, LinearLayout badgeBox, TextView status) {
                super(card); this.glyph = glyph; this.title = title; this.badgeBox = badgeBox; this.status = status;
            }
        }
        @Override public int getItemCount() { return entries.size(); }
        @Override public long getItemId(int position) { return ScreensaverPicker.stableId(entries.get(position).id); }
        @Override public Row onCreateViewHolder(ViewGroup parent, int viewType) {
            LinearLayout card = new LinearLayout(MainActivity.this); card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(dp(14), dp(8), dp(14), dp(8)); card.setBackground(tileBackground());
            card.setFocusable(true); card.setClickable(true);
            TextView glyph = text("", 22); glyph.setGravity(Gravity.CENTER);
            glyph.setBackground(shape(paletteColors.raised, paletteColors.edge, 1));
            card.addView(glyph, new LinearLayout.LayoutParams(dp(44), dp(44)));
            LinearLayout copy = new LinearLayout(MainActivity.this); copy.setOrientation(LinearLayout.VERTICAL); copy.setPadding(dp(14), 0, 0, 0);
            LinearLayout nameRow = new LinearLayout(MainActivity.this); nameRow.setGravity(Gravity.CENTER_VERTICAL);
            TextView title = bold("", 19); title.setMaxLines(2); title.setEllipsize(android.text.TextUtils.TruncateAt.END);
            nameRow.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
            LinearLayout badgeBox = new LinearLayout(MainActivity.this); badgeBox.setGravity(Gravity.CENTER_VERTICAL);
            nameRow.addView(badgeBox, new LinearLayout.LayoutParams(-2, -2));
            copy.addView(nameRow);
            TextView status = muted("", 14); status.setSingleLine(true); status.setEllipsize(android.text.TextUtils.TruncateAt.END);
            copy.addView(status);
            card.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
            androidx.recyclerview.widget.RecyclerView.LayoutParams lp = new androidx.recyclerview.widget.RecyclerView.LayoutParams(-1, -2);
            lp.topMargin = dp(4); lp.bottomMargin = dp(4); card.setLayoutParams(lp);
            return new Row(card, glyph, title, badgeBox, status);
        }
        @Override public void onBindViewHolder(Row row, int position) {
            ScreensaverRegistry.Entry entry = entries.get(position);
            View card = row.itemView;
            card.setTag("card:" + entry.id);
            row.glyph.setText(entry.glyph);
            row.title.setText(entry.title(MainActivity.this));
            row.badgeBox.removeAllViews();
            ScreensaverPicker.Badge scope = badges.get(entry.id);
            StringBuilder spoken = new StringBuilder(entry.title(MainActivity.this));
            if (scope != null) {
                String label = getString(scope == ScreensaverPicker.Badge.ON_HOME ? R.string.hb_ss_on_home
                        : scope == ScreensaverPicker.Badge.IN_OTHER_APPS ? R.string.hb_ss_other_apps : R.string.hb_ss_in_use);
                addRowBadge(row.badgeBox, badge(label, paletteColors.focus, contrastingText(paletteColors.focus)));
                spoken.append(", ").append(label);
            } else if (ScreensaverRules.use(screensaverStatus.managed, entry.id, entry.playsOnHome()) == ScreensaverRules.Use.EVERYWHERE_SCREEN) {
                String label = getString(R.string.hb_ss_needs_everywhere);
                addRowBadge(row.badgeBox, badge(label, paletteColors.raised, paletteColors.text));
                spoken.append(", ").append(label);
            }
            // One short status line; descriptions live in the detail pane only.
            String status = entry.app() ? "☾ " + getString(entry.summary) : "◆ " + getString(R.string.hb_ss_built_in);
            row.status.setText(status);
            spoken.append(", ").append(status.substring(2));
            card.setContentDescription(spoken);
            card.setOnFocusChangeListener((v, has) -> {
                if (!has) return;
                int at = row.getBindingAdapterPosition();
                if (at == androidx.recyclerview.widget.RecyclerView.NO_POSITION) return;
                screensaverPicker.browsed = entry.id; screensaverPicker.browsedIndex = at;
                if (!entry.id.equals(screensaverPaneEntry) && screensaverPane != null) drawScreensaverPane(screensaverPane, entry);
                updateScreensaverCount();
            });
            card.setOnClickListener(v -> {
                screensaverPicker.browsed = entry.id; screensaverPicker.browsedIndex = row.getBindingAdapterPosition();
                // Touch: a tap inspects the row. Remote: short OK enters its actions, launching nothing.
                if (v.isInTouchMode()) { if (screensaverPane != null) drawScreensaverPane(screensaverPane, entry); updateScreensaverCount(); }
                else if (screensaverFirstAction != null) screensaverFirstAction.requestFocus();
            });
            card.setOnKeyListener((v, key, event) -> {
                int at = row.getBindingAdapterPosition();
                if (at == androidx.recyclerview.widget.RecyclerView.NO_POSITION) return false;
                if (key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_DOWN) {
                    if (event.getAction() != KeyEvent.ACTION_DOWN) return true;
                    int delta = key == KeyEvent.KEYCODE_DPAD_UP ? -1 : 1;
                    switch (ScreensaverPicker.move(at, entries.size(), delta)) {
                        case HEADER: {
                            View retry = overlay == null ? null : overlay.findViewWithTag("fail:retry");
                            View back = overlay == null ? null : overlay.findViewWithTag("back");
                            if (retry != null) retry.requestFocus(); else if (back != null) back.requestFocus();
                            break;
                        }
                        case ROW: focusScreensaverRow(ScreensaverPicker.target(at, entries.size(), delta)); break;
                        default: break;   // the last row: stay; the count shows the end
                    }
                    return true;
                }
                if (key == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN && screensaverFirstAction != null) screensaverFirstAction.requestFocus();
                    return true;
                }
                return key == KeyEvent.KEYCODE_DPAD_LEFT;   // nothing to the left of the list
            });
        }
    }
    private void addRowBadge(LinearLayout box, TextView badge) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2); lp.leftMargin = dp(8); box.addView(badge, lp);
    }
    /** Focuses row index, scrolling only enough to reveal it; a row not laid out yet is focused after the scroll. */
    private void focusScreensaverRow(int index) {
        androidx.recyclerview.widget.RecyclerView list = screensaverList;
        if (list == null || list.getAdapter() == null || index < 0 || index >= list.getAdapter().getItemCount()) return;
        androidx.recyclerview.widget.RecyclerView.LayoutManager rows = list.getLayoutManager();
        View row = rows == null ? null : rows.findViewByPosition(index);
        if (row != null) { row.requestFocus(); return; }
        list.scrollToPosition(index);
        list.post(() -> {
            View laid = list.getLayoutManager() == null ? null : list.getLayoutManager().findViewByPosition(index);
            if (laid != null) laid.requestFocus();
        });
    }
    /** Remembers the first visible row and its offset, so the next draw restores the scroll exactly. */
    private void captureScreensaverAnchor() {
        androidx.recyclerview.widget.RecyclerView list = screensaverList;
        if (list == null || !list.isAttachedToWindow() || !(list.getLayoutManager() instanceof androidx.recyclerview.widget.LinearLayoutManager)) return;
        androidx.recyclerview.widget.LinearLayoutManager rows = (androidx.recyclerview.widget.LinearLayoutManager) list.getLayoutManager();
        int first = rows.findFirstVisibleItemPosition();
        View view = first == androidx.recyclerview.widget.RecyclerView.NO_POSITION ? null : rows.findViewByPosition(first);
        if (view == null || first >= screensaverEntries.size()) return;
        screensaverPicker.anchor = screensaverEntries.get(first).id;
        screensaverPicker.anchorOffset = rows.getDecoratedTop(view) - list.getPaddingTop()
                - ((androidx.recyclerview.widget.RecyclerView.LayoutParams) view.getLayoutParams()).topMargin;
    }
    private void updateScreensaverCount() {
        if (screensaverCount != null)
            screensaverCount.setText(getString(R.string.hb_ss_position, screensaverPicker.browsedIndex + 1, screensaverEntries.size()));
    }
    private void updateScreensaverThumb(androidx.recyclerview.widget.RecyclerView list, View thumb) {
        View track = (View) thumb.getParent();
        int[] at = ScreensaverPicker.thumb(list.computeVerticalScrollOffset(), list.computeVerticalScrollExtent(),
                list.computeVerticalScrollRange(), track.getHeight(), dp(24));
        if (at == null) { thumb.setVisibility(View.INVISIBLE); return; }
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) thumb.getLayoutParams();
        if (lp.topMargin != at[0] || lp.height != at[1]) { lp.topMargin = at[0]; lp.height = at[1]; thumb.setLayoutParams(lp); }
        thumb.setVisibility(View.VISIBLE);
    }
    /** The chooser is the menu showing. */
    private boolean screensaverChooserShowing() { return activeMenu != null && activeMenu == screensaverMenu && screensaverList != null; }
    /**
     * Installed apps changed while the chooser is open: re-read them and redraw on the same browsed entry and scroll
     * position. If the browsed app went away, browse the nearest remaining entry and say why.
     */
    private void refreshScreensaverChooser() {
        captureScreensaverAnchor();
        View focused = getCurrentFocus();
        String focusTag = focused != null && screensaverPane != null && isDescendant(screensaverPane, focused) && focused.getTag() instanceof String ? (String) focused.getTag() : null;
        int before = ScreensaverPicker.indexOf(screensaverPicker.browsed, screensaverEntries);
        String goneName = before >= 0 ? screensaverEntries.get(before).title(this) : null;
        refreshScreensaverEntries();
        if (before >= 0 && ScreensaverPicker.indexOf(screensaverPicker.browsed, screensaverEntries) < 0) {
            focusTag = null; screensaverPicker.returning = false;
            toast(getString(R.string.hb_ss_removed, goneName));
        }
        redrawScreensaver(focusTag);
    }
    private static boolean isDescendant(View ancestor, View view) {
        for (android.view.ViewParent p = view.getParent(); p != null; p = p.getParent()) if (p == ancestor) return true;
        return false;
    }

    private void drawScreensaverPane(LinearLayout pane, ScreensaverRegistry.Entry entry) {
        pane.removeAllViews();
        if (pane == screensaverPane) screensaverPaneEntry = entry.id;
        ScreensaverSettings settings = layout.screensaver;
        boolean clock = ScreensaverRegistry.CLOCK.equals(entry.id);
        FrameLayout sample = new FrameLayout(this);
        String backdrop = clock ? settings.clockBackdrop : ScreensaverSettings.BACKDROP_HOME;
        if (entry.missing()) {
            // The saved app is not installed: its glyph only; the description says what plays meanwhile.
            sample.setBackground(shape(paletteColors.raised, paletteColors.edge, 1));
            TextView glyph = text(entry.glyph, 40); glyph.setAlpha(0.45f); glyph.setGravity(Gravity.CENTER);
            sample.addView(glyph, new FrameLayout.LayoutParams(-1, -1));
        } else if (entry.app()) {
            // Android gives apps no way to start another app's screensaver: an app without its own preview screen says
            // so instead of offering a Preview; one with it shows its glyph, and Preview opens that screen.
            sample.setBackground(shape(paletteColors.raised, paletteColors.edge, 1));
            LinearLayout none = new LinearLayout(this); none.setGravity(Gravity.CENTER_VERTICAL); none.setPadding(dp(24), 0, dp(24), 0);
            TextView glyph = text(entry.glyph, 40); none.addView(glyph);
            if (!entry.playsOnHome()) {
                TextView why = muted(getString(R.string.hb_ss_no_preview), 15); why.setPadding(dp(18), 0, 0, 0);
                none.addView(why, new LinearLayout.LayoutParams(0, -2, 1));
            }
            sample.addView(none, new FrameLayout.LayoutParams(-1, -1));
        } else if (ScreensaverSettings.BACKDROP_GLOW.equals(backdrop)) {
            GradientDrawable glow = new GradientDrawable(); glow.setGradientType(GradientDrawable.RADIAL_GRADIENT);
            glow.setColors(new int[]{0xFF4A2A8C, 0xFF1A1236, 0xFF050508}); glow.setGradientCenter(0.25f, 0.45f);
            glow.setGradientRadius(dp(320)); glow.setCornerRadius(dp(12)); sample.setBackground(glow);
        } else {
            int fill = ScreensaverSettings.BACKDROP_BLACK.equals(backdrop) ? Color.BLACK : paletteColorForId(layout.palette);
            UiBackgroundDrawable d = new UiBackgroundDrawable(fill); d.setCornerRadius(dp(12)); sample.setBackground(d);
        }
        if (!entry.app() && (clock || settings.backgroundShowsClock)) {
            LinearLayout time = new LinearLayout(this); time.setOrientation(LinearLayout.VERTICAL);
            android.widget.TextClock hours = new android.widget.TextClock(this);
            hours.setFormat12Hour("h:mm"); hours.setFormat24Hour("HH:mm"); hours.setTextSize(46); hours.setTextColor(0xE6FFFFFF);
            android.widget.TextClock date = new android.widget.TextClock(this);
            date.setFormat12Hour(getString(R.string.date_format)); date.setFormat24Hour(getString(R.string.date_format));
            date.setTextSize(15); date.setTextColor(0xB3FFFFFF);
            time.addView(hours); time.addView(date);
            ClockFormat.apply(time, Config.load(this).clockFormat);
            FrameLayout.LayoutParams timeLp = new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER_VERTICAL | Gravity.START);
            timeLp.leftMargin = dp(30); sample.addView(time, timeLp);
        }
        if (!entry.missing() && (!entry.app() || entry.playsOnHome())) {
            TextView tag = badge(getString(R.string.hb_ss_preview), 0xCC000000, Color.WHITE);
            FrameLayout.LayoutParams tagLp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.END);
            tagLp.setMargins(0, 0, dp(10), dp(10)); sample.addView(tag, tagLp);
        }
        sample.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        pane.addView(sample, new LinearLayout.LayoutParams(-1, dp(130)));
        TextView name = bold(entry.title(this), 21); name.setPadding(0, dp(8), 0, 0); pane.addView(name);
        TextView detail = muted(entry.missing() ? getString(R.string.hb_ss_missing_detail, ScreensaverRegistry.fallback().title(this))
                : getString(entry.description), 16); pane.addView(detail);
        // Wraps, so large text never pushes the actions off a fixed row; the pane itself scrolls.
        android.widget.GridLayout actions = new android.widget.GridLayout(this);
        actions.setColumnCount(Math.max(1, (dashboardWidthPixels() < dp(900)) ? 2 : 3));
        EverywhereScreensaver.Status status = screensaverStatus;
        boolean inUse = entry.id.equals(chosenScreensaver().id);
        screensaverFirstAction = null;
        for (ScreensaverPicker.Action action : ScreensaverPicker.actions(entry, status.managed, inUse)) {
            switch (action) {
                case EVERYWHERE: addScreensaverAction(actions, getString(R.string.hb_ss_everywhere), action.tag, () -> openEverywhere(entry.id)); break;
                case USE: addScreensaverAction(actions, getString(R.string.hb_ss_use), action.tag, () -> useScreensaver(entry, entry.app() ? "act:use" : "act:preview")); break;
                case PREVIEW: addScreensaverAction(actions, getString(R.string.hb_ss_preview), action.tag, () -> previewScreensaver(entry, false)); break;
                case OPTIONS: addScreensaverAction(actions, getString(R.string.hb_ss_options), action.tag, () -> openScreensaverOptions(entry.id, null)); break;
                case SETTINGS: addScreensaverAction(actions, getString(R.string.hb_ss_open_settings, entry.name), action.tag, () -> openScreensaverAppSettings(entry)); break;
            }
        }
        LinearLayout.LayoutParams actionsLp = new LinearLayout.LayoutParams(-1, -2); actionsLp.topMargin = dp(8); pane.addView(actions, actionsLp);
        LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(-1, dp(1)); lineLp.setMargins(0, dp(12), 0, dp(8));
        pane.addView(divider(), lineLp);
        pane.addView(bold(getString(R.string.hb_ss_when), 18));
        LinearLayout onHome = new LinearLayout(this); onHome.setGravity(Gravity.CENTER_VERTICAL); onHome.setMinimumHeight(dp(46));
        onHome.addView(bold(getString(R.string.hb_ss_on_home), 16), new LinearLayout.LayoutParams(dp(140), -2));
        onHome.addView(text(screensaverStartsOnHome(status), 16), new LinearLayout.LayoutParams(0, -2, 1));
        if (!status.managed) {
            // While Android is in charge Homeboard's own Home timer is off, so there is nothing to change here.
            // Home's choice is an app: Change opens When it starts alone, without a built-in's look options.
            ScreensaverRegistry.Entry chosen = chosenScreensaver();
            boolean whenOnly = ScreensaverPicker.changeOpens(chosen) == ScreensaverPicker.OptionsView.WHEN_ONLY;
            String optionsFor = whenOnly ? chosen.id : entry.app() ? chosenHomeboardEntry().id : entry.id;
            TextView change = infoButton(getString(R.string.hb_ss_change), chooserAction("act:change",
                    () -> openScreensaverOptions(optionsFor, "idle:" + settings.idleMinutes, whenOnly)));
            change.setTag("act:change"); change.setPadding(dp(18), 0, dp(18), 0); returnToRowOnLeft(change);
            onHome.addView(change, new LinearLayout.LayoutParams(-2, dp(40)));
        }
        pane.addView(onHome);
        LinearLayout other = new LinearLayout(this); other.setGravity(Gravity.CENTER_VERTICAL); other.setMinimumHeight(dp(46));
        other.addView(bold(getString(R.string.hb_ss_other_apps), 16), new LinearLayout.LayoutParams(dp(140), -2));
        other.addView(muted(screensaverOtherApps(status), 15), new LinearLayout.LayoutParams(0, -2, 1));
        TextView manage = infoButton(getString(status.managed ? R.string.hb_ss_this_tv_open : R.string.hb_ss_everywhere),
                chooserAction("act:everywhere", () -> openEverywhere(entry.id)));
        manage.setTag("act:everywhere"); manage.setPadding(dp(18), 0, dp(18), 0); returnToRowOnLeft(manage);
        other.addView(manage, new LinearLayout.LayoutParams(-2, dp(40)));
        pane.addView(other);
        if (screensaverAndroidFollowsOnPane(status)) pane.addView(muted(getString(R.string.hb_ss_android_follows), 14));
    }
    /**
     * Opens an app's own settings screen. Home pauses, which closes its menus, so the chooser comes back on resume on
     * the same entry, position and action (returningToScreensaver). A failed launch keeps everything for a retry.
     */
    private void openScreensaverAppSettings(ScreensaverRegistry.Entry entry) {
        ComponentName settings = entry.settings == null ? null : ComponentName.unflattenFromString(entry.settings);
        if (settings == null) { toast(getString(R.string.hb_android_could_not_open_that_action)); return; }
        boolean fromChooser = screensaverChooserShowing();
        try {
            startActivity(new Intent().setComponent(settings));
            if (fromChooser) { returningToScreensaver = entry.id; screensaverOptionsOpen = false; screensaverAccepted = null; }
        } catch (RuntimeException e) { toast(getString(R.string.hb_android_could_not_open_that_action)); }
    }
    /** An action that leaves or redraws the chooser: remembers the scroll and the action, so the return focuses it. */
    private Runnable chooserAction(String tag, Runnable action) {
        return () -> { if (screensaverChooserShowing()) { captureScreensaverAnchor(); screensaverPicker.used(tag); } action.run(); };
    }
    /** Left from the leftmost control of the detail pane returns to the browsed row, not to whichever row is nearest. */
    private void returnToRowOnLeft(View control) {
        control.setOnKeyListener((v, key, event) -> {
            if (key != KeyEvent.KEYCODE_DPAD_LEFT || screensaverPane == null || !screensaverChooserShowing()) return false;
            View next = v.focusSearch(View.FOCUS_LEFT);
            if (next != null && isDescendant(screensaverPane, next)) return false;
            if (event.getAction() == KeyEvent.ACTION_DOWN) focusScreensaverRow(screensaverPicker.browsedIndex);
            return true;
        });
    }
    private void addScreensaverAction(ViewGroup actions, String label, String tag, Runnable action) {
        TextView button = infoButton(label, chooserAction(tag, action)); button.setTag(tag); button.setPadding(dp(18), 0, dp(18), 0);
        ViewGroup.MarginLayoutParams lp = actions instanceof android.widget.GridLayout ? new android.widget.GridLayout.LayoutParams() : new LinearLayout.LayoutParams(-2, dp(42));
        lp.width = -2; lp.height = dp(42); lp.rightMargin = dp(8); lp.bottomMargin = dp(8);
        actions.addView(button, lp);
        returnToRowOnLeft(button);
        if (screensaverFirstAction == null) screensaverFirstAction = button;
    }

    private void openScreensaverOptions(String entryId, String focusTag) { openScreensaverOptions(entryId, focusTag, false); }
    /** whenOnly: When it starts alone, for Home's app choice (ScreensaverPicker.changeOpens). */
    private void openScreensaverOptions(String entryId, String focusTag, boolean whenOnly) {
        screensaverFocusTag = focusTag;
        openMenu(new MenuSpec(getString(whenOnly ? R.string.hb_ss_when : R.string.hb_ss_options_title), () -> drawScreensaverOptions(entryId, whenOnly)));
    }
    /**
     * An entry's Options and When it starts. Each choice saves at once with Undo; sound and screen care are
     * statements. whenOnly shows When it starts alone, titled for Home's app choice, with no built-in look rows.
     */
    private void drawScreensaverOptions(String entryId, boolean whenOnly) {
        java.util.Set<ScreensaverPicker.Group> groups = ScreensaverPicker.groups(whenOnly
                ? ScreensaverPicker.OptionsView.WHEN_ONLY : ScreensaverPicker.OptionsView.ENTRY_OPTIONS);
        ScreensaverRegistry.Entry entry = ScreensaverRegistry.entry(entryId);
        ScreensaverSettings settings = layout.screensaver;
        LinearLayout body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL);
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(10), dp(18), dp(10)); box.setBackground(shape(paletteColors.surface, paletteColors.edge, 1));
        if (groups.contains(ScreensaverPicker.Group.LOOK)) body.addView(box, new LinearLayout.LayoutParams(-1, -2));
        screensaverStatus = everywhere().status();
        String appName = whenOnly ? ScreensaverRegistry.resolve(entryId, screensaverEntries).title(this) : null;
        LinearLayout panel = screensaverPanel(whenOnly ? getString(R.string.hb_ss_when) + " · " + appName : getString(R.string.hb_ss_options_title), body);
        LinearLayout title = new LinearLayout(this); title.setGravity(Gravity.CENTER_VERTICAL);
        title.addView(bold(getString(entry.label), 21));
        TextView builtIn = badge(getString(R.string.hb_ss_built_in), paletteColors.raised, paletteColors.text);
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(-2, -2); badgeLp.leftMargin = dp(10); title.addView(builtIn, badgeLp);
        box.addView(title);
        View first = null;
        if (entry.options.contains(ScreensaverRegistry.Option.BACKDROP)) {
            LinearLayout row = optionRow(box, getString(R.string.hb_ss_backdrop), true);
            String[] values = ScreensaverSettings.BACKDROPS;
            int[] labels = {R.string.hb_ss_glow, R.string.hb_ss_background, R.string.hb_ss_black};
            for (int i = 0; i < values.length; i++) {
                String value = values[i], label = getString(labels[i]);
                TextView segment = segment(label, value.equals(settings.clockBackdrop), "bd:" + value, () -> {
                    settings.clockBackdrop = value; save(getString(R.string.hb_ss_undo_backdrop, label)); redrawScreensaver("bd:" + value);
                });
                addSegment(row, segment); if (first == null) first = segment;
            }
        }
        if (entry.options.contains(ScreensaverRegistry.Option.SHOW_CLOCK)) {
            LinearLayout row = optionRow(box, getString(R.string.hb_ss_show_clock), true);
            for (boolean on : new boolean[]{true, false}) {
                String label = getString(on ? R.string.hb_on : R.string.hb_off);
                TextView segment = segment(label, settings.backgroundShowsClock == on, "clock:" + on, () -> {
                    settings.backgroundShowsClock = on; save(getString(R.string.hb_ss_undo_clock, label)); redrawScreensaver("clock:" + on);
                });
                addSegment(row, segment); if (first == null) first = segment;
            }
        }
        boolean backdropDim = entry.options.contains(ScreensaverRegistry.Option.BACKDROP_DIM);
        if (backdropDim || entry.options.contains(ScreensaverRegistry.Option.DIM)) {
            boolean enabled = !backdropDim || ScreensaverSettings.BACKDROP_HOME.equals(settings.clockBackdrop);
            LinearLayout row = optionRow(box, getString(backdropDim ? R.string.hb_ss_dim_backdrop : R.string.hb_ss_dim), enabled);
            int current = backdropDim ? settings.clockDimPercent : settings.backgroundDimPercent;
            for (int step : new int[]{-ScreensaverSettings.DIM_STEP, ScreensaverSettings.DIM_STEP}) {
                String tag = "dim:" + step;
                TextView segment = segment((step < 0 ? "−" : "+") + ScreensaverSettings.DIM_STEP + "%", false, tag, () -> {
                    int value = ScreensaverSettings.dim(current + step);
                    if (backdropDim) settings.clockDimPercent = value; else settings.backgroundDimPercent = value;
                    save(getString(R.string.hb_ss_undo_dim, value)); redrawScreensaver(tag);
                });
                if (!enabled) { segment.setFocusable(false); segment.setAlpha(0.45f); }
                addSegment(row, segment);
                if (step < 0) { TextView value = text(current + "%", 17); value.setPadding(dp(4), 0, dp(10), 0); if (!enabled) value.setAlpha(0.45f); row.addView(value); }
            }
            if (backdropDim) { TextView note = muted(getString(R.string.hb_ss_dim_note), 14); note.setPadding(dp(8), 0, 0, 0); row.addView(note); }
        }
        if (ScreensaverRegistry.CLOCK.equals(entry.id) || settings.backgroundShowsClock) {
            LinearLayout row = optionRow(box, getString(R.string.hb_ss_time_format), true);
            row.addView(text(getString(ClockFormat.is24(this, Config.load(this).clockFormat) ? R.string.hb_ss_time_24 : R.string.hb_ss_time_12), 17));
            LinearLayout care = optionRow(box, getString(R.string.hb_ss_screen_care), true);
            care.addView(text(getString(R.string.hb_ss_screen_care_value), 17));
        }
        LinearLayout sound = optionRow(box, getString(R.string.hb_ss_sound), true);
        sound.addView(text(getString(R.string.hb_ss_sound_value), 17));
        LinearLayout tail = new LinearLayout(this); tail.setGravity(Gravity.CENTER_VERTICAL);
        TextView preview = infoButton(getString(R.string.hb_ss_preview), () -> previewScreensaver(entry, true));
        preview.setTag("opt:preview"); preview.setPadding(dp(18), 0, dp(18), 0);
        tail.addView(preview, new LinearLayout.LayoutParams(-2, dp(40)));
        TextView separate = muted(getString(R.string.hb_ss_separate), 14); separate.setPadding(dp(14), 0, 0, 0);
        tail.addView(separate, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams tailLp = new LinearLayout.LayoutParams(-1, -2); tailLp.topMargin = dp(4); box.addView(tail, tailLp);
        if (first == null) first = preview;
        if (!groups.contains(ScreensaverPicker.Group.LOOK)) first = null;   // the look box is not shown

        LinearLayout when = new LinearLayout(this); when.setOrientation(LinearLayout.VERTICAL);
        when.setPadding(dp(18), dp(8), dp(18), dp(8)); when.setBackground(shape(paletteColors.surface, paletteColors.edge, 1));
        LinearLayout.LayoutParams whenLp = new LinearLayout.LayoutParams(-1, -2); whenLp.topMargin = dp(10); body.addView(when, whenLp);
        when.addView(bold(getString(R.string.hb_ss_when), 19));
        boolean homeTimer = !screensaverStatus.managed;
        LinearLayout idle = optionRow(when, getString(R.string.hb_ss_start_after), true);
        if (!homeTimer) idle.addView(muted(getString(R.string.hb_ss_home_timer_off), 15), new LinearLayout.LayoutParams(0, -2, 1));
        else for (int minutes : ScreensaverSettings.IDLE_MINUTES) {
            String label = minutes == ScreensaverSettings.ANDROID_DECIDES ? getString(R.string.hb_ss_android_decides) : String.valueOf(minutes);
            String undo = minutes == ScreensaverSettings.ANDROID_DECIDES ? label : getString(R.string.hb_ss_minutes, minutes);
            TextView segment = segment(label, settings.idleMinutes == minutes, "idle:" + minutes, () -> {
                settings.idleMinutes = minutes; save(getString(R.string.hb_ss_undo_idle, undo)); redrawScreensaver("idle:" + minutes);
            });
            addSegment(idle, segment);
            if (first == null) first = segment;
        }
        if (homeTimer) { TextView unit = text(getString(R.string.hb_ss_min_without_input), 16); unit.setPadding(dp(6), 0, 0, 0); idle.addView(unit); }
        LinearLayout other = optionRow(when, getString(R.string.hb_ss_other_apps), true);
        other.addView(muted(screensaverOtherApps(screensaverStatus), 15), new LinearLayout.LayoutParams(0, -2, 1));
        if (screensaverAndroidFollows(screensaverStatus)) when.addView(muted(getString(R.string.hb_ss_android_follows), 14));
        TextView footer = muted(getString(R.string.hb_ss_footer), 13);
        LinearLayout.LayoutParams footerLp = new LinearLayout.LayoutParams(-1, -2); footerLp.topMargin = dp(8); body.addView(footer, footerLp);
        focusScreensaver(panel, first);
    }

    private void openEverywhere(String entryId) {
        screensaverSetupChecked = false; screensaverShowQr = false; screensaverFocusTag = null;
        openMenu(new MenuSpec(getString(R.string.hb_ss_everywhere_title), () -> drawEverywhere(entryId)));
    }
    /** One row of the Use everywhere card: a label on the left, its value beside it. */
    private void everywhereRow(LinearLayout box, String label, String value) {
        LinearLayout row = new LinearLayout(this); row.setMinimumHeight(dp(34)); row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(muted(label, 17), new LinearLayout.LayoutParams(dp(150), -2));
        row.addView(text(value, 17), new LinearLayout.LayoutParams(0, -2, 1));
        box.addView(row, new LinearLayout.LayoutParams(-1, -2));
    }
    private TextView everywhereButton(LinearLayout actions, String label, String tag, Runnable action) {
        TextView button = infoButton(label, action); button.setTag(tag); button.setPadding(dp(18), 0, dp(18), 0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(44)); lp.rightMargin = dp(10);
        actions.addView(button, lp);
        return button;
    }
    /**
     * Home settings → Screensaver → Use everywhere: the one-time setup (not permitted), the asked-once question
     * (permitted, not yet managed) or this TV's screensaver in use (managed), per ScreensaverRules.everywhereScreen.
     * Opening it, Retry and Show QR code change nothing; only Confirm, Restore and later choices write.
     */
    private void drawEverywhere(String entryId) {
        EverywhereScreensaver.Status status = screensaverStatus = everywhere().status();
        ScreensaverRegistry.Entry entry = ScreensaverRegistry.resolve(entryId, screensaverEntries);
        LinearLayout body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL);
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(14), dp(22), dp(14)); box.setBackground(shape(paletteColors.surface, paletteColors.edge, 1));
        body.addView(box, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout panel = screensaverPanel(getString(R.string.hb_ss_everywhere_title), body);
        LinearLayout actions = new LinearLayout(this);
        View first;
        switch (ScreensaverRules.everywhereScreen(status.managed, status.mayWrite)) {
            case SETUP: {
                box.addView(bold(getString(R.string.hb_ss_setup_title), 22));
                TextView why = text(getString(R.string.hb_ss_setup_body), 17); why.setPadding(0, dp(8), 0, dp(8)); box.addView(why);
                setupCommand(box, screensaverShowQr);
                TextView nothing = muted(getString(R.string.hb_ss_setup_nothing), 16); nothing.setPadding(0, dp(8), 0, 0); box.addView(nothing);
                if (screensaverSetupChecked) box.addView(muted(getString(R.string.hb_ss_setup_still), 16));
                first = everywhereButton(actions, getString(R.string.hb_ss_retry), "ev:retry", () -> { screensaverSetupChecked = true; redrawScreensaver("ev:retry"); });
                everywhereButton(actions, getString(screensaverShowQr ? R.string.hb_ss_hide_qr : R.string.hb_ss_show_qr), "ev:qr",
                        () -> { screensaverShowQr = !screensaverShowQr; redrawScreensaver("ev:qr"); });
                break;
            }
            case ASK_ONCE: {
                box.addView(bold(getString(R.string.hb_ss_ask_title), 22));
                LinearLayout rows = new LinearLayout(this); rows.setOrientation(LinearLayout.VERTICAL); rows.setPadding(0, dp(8), 0, dp(4));
                everywhereRow(rows, getString(R.string.hb_ss_now), screensaverName(status.component));
                everywhereRow(rows, getString(R.string.hb_ss_after_label), getString(R.string.hb_ss_after_value, entry.title(this)));
                everywhereRow(rows, getString(R.string.hb_ss_starts), getString(R.string.hb_ss_starts_unchanged, screensaverTimeout(status.timeout)));
                everywhereRow(rows, getString(R.string.hb_ss_later), getString(R.string.hb_ss_later_value));
                box.addView(rows);
                box.addView(muted(getString(R.string.hb_ss_ask_note), 16));
                first = everywhereButton(actions, getString(R.string.hb_ss_confirm), "ev:confirm", () -> changeEverywhere(entry, true, "ev:start"));
                everywhereButton(actions, getString(R.string.hb_cancel), "ev:cancel", this::backMenu);
                break;
            }
            default: {
                ScreensaverRegistry.Entry chosen = chosenScreensaver();
                boolean set = ScreensaverRules.inUseEverywhere(true, status.component, chosen.id, everywhere().gateway().ownComponent());
                LinearLayout title = new LinearLayout(this); title.setGravity(Gravity.CENTER_VERTICAL);
                title.addView(bold(screensaverName(status.component), 22));
                if (set) {
                    TextView badge = badge(getString(R.string.hb_ss_this_tv), paletteColors.focus, contrastingText(paletteColors.focus));
                    LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(-2, -2); badgeLp.leftMargin = dp(12); title.addView(badge, badgeLp);
                }
                box.addView(title);
                LinearLayout rows = new LinearLayout(this); rows.setOrientation(LinearLayout.VERTICAL); rows.setPadding(0, dp(8), 0, dp(4));
                everywhereRow(rows, getString(R.string.hb_ss_starts), getString(R.string.hb_ss_starts_any_app, screensaverTimeout(status.timeout)));
                if (status.original != null)
                    everywhereRow(rows, getString(R.string.hb_ss_original), getString(R.string.hb_ss_original_value, screensaverName(status.original.component())));
                box.addView(rows);
                box.addView(muted(!set ? getString(R.string.hb_ss_not_set) : chosen.app() ? getString(R.string.hb_ss_in_use_app, chosen.name)
                        : getString(R.string.hb_ss_in_use_homeboard), 16));
                first = everywhereButton(actions, getString(R.string.hb_ss_start_now), "ev:start", () -> startScreensaverNow(chosen));
                if (chosen.app() && chosen.settings != null)
                    everywhereButton(actions, getString(R.string.hb_ss_open_settings, chosen.name), "ev:settings", () -> openScreensaverAppSettings(chosen));
                if (status.original != null)
                    everywhereButton(actions, getString(R.string.hb_ss_restore, screensaverName(status.original.component())), "ev:restore", this::restoreEverywhere);
            }
        }
        LinearLayout.LayoutParams actionsLp = new LinearLayout.LayoutParams(-1, -2); actionsLp.topMargin = dp(14); box.addView(actions, actionsLp);
        TextView footer = muted(getString(R.string.hb_ss_everywhere_footer), 13);
        LinearLayout.LayoutParams footerLp = new LinearLayout.LayoutParams(-1, -2); footerLp.topMargin = dp(8); body.addView(footer, footerLp);
        focusScreensaver(panel, first);
    }
    /** Start now: Android's own shortcut, which plays this TV's screensaver; Homeboard's Show now for its own entries if the shortcut is missing. */
    private void startScreensaverNow(ScreensaverRegistry.Entry chosen) {
        if (everywhere().gateway().startNow()) return;
        if (!chosen.app()) {
            try { startActivity(ScreensaverActivity.intent(this, ScreensaverActivity.MODE_NOW, null)); return; }
            catch (RuntimeException unavailable) { /* falls through to the notice */ }
        }
        toast(getString(R.string.hb_android_could_not_open_that_action));
    }

    private void cleanupInstructions() {
        openMenu(new MenuSpec(getString(R.string.hb_cleanup_title), this::drawCleanupInstructions));
    }

    private void drawCleanupInstructions() {
        if (overlay != null) root.removeView(overlay);
        overlay = new FrameLayout(this); overlay.setBackgroundColor(paletteColors.scrim);
        LinearLayout panel = new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(26), dp(22), dp(26), dp(20));
        panel.setBackground(shape(paletteColors.raised, paletteColors.edge, 2));
        TextView heading = text(getString(R.string.hb_cleanup_title), 25); heading.setTypeface(null, Typeface.BOLD);
        panel.addView(heading);
        TextView description = text(getString(R.string.hb_cleanup_intro) + "\n\n" + getString(R.string.hb_cleanup_setup), 18);
        description.setPadding(0, dp(18), 0, dp(18)); description.setLineSpacing(0, 1.12f); panel.addView(description);
        View back = infoButton(getString(R.string.hb_hint_back), this::backMenu);
        panel.addView(back, new LinearLayout.LayoutParams(-1, dp(48)));
        int width = Math.min(dp(800), dashboardWidthPixels() - dp(80));
        overlay.addView(panel, new FrameLayout.LayoutParams(width, -2, Gravity.CENTER));
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1)); updateUndoHint(); back.requestFocus();
    }

    private void widgetsAndRestore() {
        menu(getString(R.string.hb_widgets_restore), getString(R.string.hb_add_a_widget_or_restore_an_app_shortcut),
                new String[]{getString(R.string.hb_add_widget), getString(R.string.hb_restore_hidden_app),getString(R.string.hb_dashboard_default)}, choice -> {
                    if (choice == 0) addWidgetMenu();
                    else if(choice==1) restoreHiddenApps();
                    else chooseDefaultDashboard();
                });
    }

    private void addWidgetMenu(){
        WidgetKind[] kinds={WidgetKind.ANDROID,WidgetKind.AIR,WidgetKind.CALENDAR,WidgetKind.RAIN,WidgetKind.FAVORITES,WidgetKind.GROCERY,WidgetKind.TIMER};String[] labels=new String[kinds.length+1];
        for(int i=0;i<kinds.length;i++)labels[i]=getString(widgetTitle(kinds[i]));
        labels[kinds.length]=getString(R.string.hb_favorite_an_app_from_all_apps);
        menu(getString(R.string.hb_add_to_home),getString(R.string.hb_choose_a_tile_to_add),labels,add->{
            if(add==kinds.length){allApps=false;showApps();return;}
            WidgetKind kind=kinds[add];if(kind==WidgetKind.ANDROID){androidWidgets.picker();return;}String key=kind==WidgetKind.GROCERY||kind==WidgetKind.AIR||kind==WidgetKind.RAIN||kind==WidgetKind.WORLD_CLOCK?newWidgetKey(kind.key):kind.key;
            if(!layout.widgets.contains(key)){layout.addPanel(key);if(kind==WidgetKind.RAIN)layout.setPanelSpan(key,Math.min(4,homeGridColumns()),Math.min(3,homeGridRows()));save(getString(R.string.hb_cal_added));}
            allApps=false;focused=key;homePage=homePageOf(key,layout.widgets);render(0);
        });
    }

    private String newWidgetKey(String base) {
        if (!layout.widgets.contains(base)) return base;
        int suffix = 2;
        while (layout.widgets.contains(base + ":" + suffix)) suffix++;
        return base + ":" + suffix;
    }

    private void appearanceSettings() {
        menu(getString(R.string.hb_appearance), getString(R.string.hb_customize_backgrounds_and_dashboard_elements),
                new String[]{getString(R.string.hb_icons), getString(R.string.hb_palette), getString(R.string.hb_background), getString(R.string.hb_ui_opacity) + layout.uiOpacityPercent + "%", getString(R.string.hb_dashboard_scale)}, choice -> {
                    if (choice == 0) appArtworkSettings();
                    else if (choice == 1) colorPaletteSettings();
                    else if (choice == 2) backgroundSettings();
                    else if (choice == 4) dashboardScaleSettings();
                    else startAdjustment(getString(R.string.hb_ui_opacity_70), layout.uiOpacityPercent,
                            this::applyUiBackgroundOpacity, value -> {
                                layout.uiOpacityPercent = value; save(getString(R.string.hb_hint_ui_opacity) + value + "%"); render(0);
                            });
                });
    }

    private void dashboardScaleSettings() {
        if (layoutReadOnly) { toast(getString(R.string.hb_aw_newer_layout)); return; }
        openMenu(new MenuSpec(getString(R.string.hb_dashboard_scale), this::showDashboardScale));
    }
    private void restoreScaleNavigation() {
        allApps = scaleOriginalApps; homePage = scaleOriginalPage; focused = scaleOriginalFocus;
    }
    private void showDashboardScale() {
        if (scalePreview == 0) {
            scaleOriginalApps = allApps; scaleOriginalPage = homePage; scaleOriginalFocus = focused;
            scaleOriginalColumns = homeGridColumns(); scaleOriginalRows = homeGridRows();
            scalePreview = layout.dashboardScale;
            allApps = false; focused = homeFocus;
        }
        try {
            scaleDraft = HomeLayout.parse(layout.encode());
            scaleDraft.setDashboardScale(scalePreview, scaleOriginalColumns, scaleOriginalRows);
        } catch (org.json.JSONException invalid) {
            toast(getString(R.string.hb_configuration_save_failed)); backMenu(); return;
        }
        render(0);
        surface.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        surface.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        if (overlay != null) root.removeView(overlay);
        overlay = new FrameLayout(this);
        overlay.setClickable(true); overlay.setFocusable(true);
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL); panel.setPadding(dp(20),dp(12),dp(20),dp(12));
        panel.setBackground(shape(paletteColors.raised, paletteColors.edge, 2));
        TextView title = text(getString(R.string.hb_dashboard_scale_preview, DashboardScale.nativePercent(scalePreview, native4kWorkspace()), homeGridColumns(), homeGridRows()), 20);
        title.setTypeface(null, Typeface.BOLD); panel.addView(title);
        TextView explanation = text(getString(native4kWorkspace() ? R.string.hb_dashboard_scale_help_native : R.string.hb_dashboard_scale_help), 15);
        explanation.setTextColor(paletteColors.muted); panel.addView(explanation);
        LinearLayout buttons = new LinearLayout(this); buttons.setOrientation(LinearLayout.HORIZONTAL);
        TextView selected = null;
        for (int value : DashboardScale.choices(native4kWorkspace())) {
            TextView button = infoButton((value == scalePreview ? "✓ " : "") + DashboardScale.nativePercent(value, native4kWorkspace()) + "%", () -> {
                scalePreview = value; showDashboardScale();
            });
            buttons.addView(button, new LinearLayout.LayoutParams(0,dp(48),1));
            if (value == scalePreview) selected = button;
        }
        buttons.addView(infoButton(getString(R.string.hb_dashboard_scale_save), () -> {
            int value = scalePreview;
            scalePreview = 0;
            layout.setDashboardScale(value, scaleOriginalColumns, scaleOriginalRows);
            scaleDraft = null;
            save(getString(R.string.hb_dashboard_scale));
            restoreScaleNavigation(); render(0); backMenu();
        }), new LinearLayout.LayoutParams(0,dp(48),1.4f));
        buttons.addView(infoButton(getString(R.string.hb_cancel), this::backMenu), new LinearLayout.LayoutParams(0,dp(48),1));
        panel.addView(buttons);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);
        lp.setMargins(dp(32),dp(16),dp(32),dp(12)); overlay.addView(panel,lp);
        if (selected != null) selected.requestFocus();
        hideAppFocusHighlight(); updateUndoHint();
    }

    private void colorPaletteSettings() {
        String[] ids = {"midnight", "forest", "ocean", "plum", "sand", "rose", "teal", "slate",
                "sunset", "black", "charcoal", "indigo", "lagoon", "moss", "cedar", "amber",
                "cobalt", "pine", "emerald", "copper"};
        String[] labels = {getString(R.string.hb_midnight), getString(R.string.hb_forest), getString(R.string.hb_ocean), getString(R.string.hb_plum), getString(R.string.hb_sand), getString(R.string.hb_rose), getString(R.string.hb_teal), getString(R.string.hb_slate),
                getString(R.string.hb_sunset), getString(R.string.hb_black), getString(R.string.hb_charcoal), getString(R.string.hb_indigo), getString(R.string.hb_lagoon), getString(R.string.hb_moss), getString(R.string.hb_cedar), getString(R.string.hb_amber),
                getString(R.string.hb_cobalt), getString(R.string.hb_pine), getString(R.string.hb_emerald), getString(R.string.hb_copper)};
        int selected = -1;
        for (int i = 0; i < ids.length; i++) if (ids[i].equals(layout.palette)) selected = i;
        if (selected < 0) {
            // Preserve an older selected palette while keeping the curated selector at twenty choices.
            selected = ids.length - 1;
            ids[selected] = layout.palette;
            labels[selected] = getString(R.string.hb_current_palette);
        }
        if ("theme".equals(layout.palette)) selected = 0;
        beginUndoGroup(getString(R.string.hb_changed_background_color));
        paletteMenu(getString(R.string.hb_palette), getString(R.string.hb_move_to_a_color_to_preview_it), labels, choice -> {
            layout.palette = ids[choice];
            applyBackgroundChoice();
        }, selected);
    }

    private void solidColorSettings() {
        cancelWallpaperPreview();
        beginUndoGroup("Changed solid background");
        layout.wallpaper.component = ""; layout.wallpaper.uuid = "";
        save(getString(R.string.hb_hint_background_solid_color)); syncBackground(); render(0);
        colorPaletteSettings();
    }

    private void applyBackgroundChoice() {
        background = paletteColor(); paletteColors = PaletteColors.from(background);
        setAirPalette();
        root.setBackgroundColor(background); save(getString(R.string.hb_changed_background)); render(0);
    }

    private void appArtworkSettings() {
        menu(getString(R.string.hb_icons), getString(R.string.hb_choose_artwork_and_icon_shape),
                new String[]{getString(R.string.hb_home_favorites) + homeAppSizeLabel(),
                        getString(R.string.hb_all_apps_97) + (layout.appBanners ? getString(R.string.hb_tv_banners) : getString(R.string.hb_dense_icons)),
                        getString(R.string.hb_hint_circular_icon_mask) + (layout.circularIcons ? getString(R.string.hb_on) : getString(R.string.hb_off))}, choice -> {
                    if (choice == 0) homeAppSizeSettings();
                    else if (choice == 1) allAppsArtworkSettings();
                    else menu(getString(R.string.hb_circular_icon_mask), getString(R.string.hb_the_icon_keeps_the_same_size),
                            new String[]{getString(R.string.hb_original_icon_shape), getString(R.string.hb_circular_mask)}, mask -> {
                                layout.circularIcons = mask == 1;
                                appsPage = Math.max(0, appItems().indexOf(focused) / appPageSize());
                                save(getString(R.string.hb_changed_app_icon_shape)); render(0);
                            }, layout.circularIcons ? 1 : 0);
                });
    }
    private String homeAppSizeLabel() {
        return HomeLayout.APP_BANNER.equals(layout.homeAppSize) ? getString(R.string.hb_banners) : getString(R.string.hb_small_icons);
    }
    private void homeAppSizeSettings() {
        menu(getString(R.string.hb_home_favorites_109), getString(R.string.hb_banners_use_wider_cells_and_taller_rows_tiles_may_move_to_another),
                new String[]{getString(R.string.hb_small_icons), getString(R.string.hb_banners)}, choice -> changeHomeAppSize(
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
        String label = getString(R.string.hb_home_favorites_111) + homeAppSizeLabel();
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
        menu(getString(R.string.hb_all_apps), getString(R.string.hb_hint_uses) + (layout.appBanners ? getString(R.string.hb_tv_banners_responsive_grid) : "launcher icons · dense grid")
                        + getString(R.string.hb_hint_home_favorites_use) + homeAppSizeLabel(),
                new String[]{getString(R.string.hb_launcher_icons_dense_grid), getString(R.string.hb_tv_banners_responsive_grid)}, artwork -> {
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
                    save(getString(R.string.hb_changed_all_apps_artwork)); render(0);
                }, layout.appBanners ? 1 : 0);
    }

    private void homeBehaviorSettings() {
        String columns = layout.homeColumns == 0 ? getString(R.string.hb_auto) : String.valueOf(layout.homeColumns);
        String rows = layout.homeRows == 0 ? getString(R.string.hb_auto) : String.valueOf(Math.min(layout.homeRows, homeGridFitRows()));
        menu(getString(R.string.hb_home_behavior), getString(R.string.hb_choose_grid_density_new_panel_size_or_screen_movement),
                new String[]{getString(R.string.hb_grid_columns) + columns,
                        getString(R.string.hb_grid_rows) + rows,
                        getString(R.string.hb_new_panel_size) + panelSizeLabel(layout.defaultPanelSize),
                        getString(R.string.hb_reduced_motion) + (layout.reducedMotion ? getString(R.string.hb_on) : getString(R.string.hb_off))}, choice -> {
                    if (choice == 0) showGridSelector(true);
                    else if (choice == 1) showGridSelector(false);
                    else if (choice == 2) panelSizeMenu(null, true);
                    else menu(getString(R.string.hb_reduced_motion_121), getString(R.string.hb_choose_how_home_and_all_apps_move),
                            new String[]{getString(R.string.hb_off), getString(R.string.hb_on)}, motion -> {
                                layout.reducedMotion = motion == 1; save(getString(R.string.hb_changed_reduced_motion)); render(0);
                            }, layout.reducedMotion ? 1 : 0);
                });
    }

    private String panelSizeLabel(int size) {
        return size == HomeLayout.PANEL_SMALL ? getString(R.string.hb_small) : size == HomeLayout.PANEL_LARGE ? getString(R.string.hb_large) : getString(R.string.hb_medium);
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
        HomeLayout.PanelSpan minimum = HomeLayout.minimumPanelSpan(resizingKey);
        if (key == KeyEvent.KEYCODE_DPAD_UP) resizeHeight = Math.max(minimum.height, resizeHeight - 1);
        else if (key == KeyEvent.KEYCODE_DPAD_DOWN) resizeHeight = Math.min(rows, resizeHeight + 1);
        else if (key == KeyEvent.KEYCODE_DPAD_LEFT) resizeWidth = Math.max(minimum.width, resizeWidth - 1);
        else if (key == KeyEvent.KEYCODE_DPAD_RIGHT) resizeWidth = Math.min(columns, resizeWidth + 1);
        layout.setPanelSpan(resizingKey, resizeWidth, resizeHeight);
        homePage = homePageOf(resizingKey, layout.widgets);
        render(0);
        // The dashboard footer itself is the resize help panel; render updates its live dimensions.
    }

    private String resizeHelpText() {
        return getString(R.string.hb_hint_resize) + resizeWidth + "×" + resizeHeight
                + getString(R.string.hb_narrower_shorter_wider_taller_ok_save_back_cancel);
    }

    private void finishResize(boolean commit) {
        if (!resizing) return;
        if(commit&&AndroidWidgetSpec.key(resizingKey)&&!androidWidgets.legalResize(resizingKey,resizeOriginalWidth,resizeOriginalHeight,resizeWidth,resizeHeight)){toast(getString(R.string.hb_aw_too_small));commit=false;}
        if (commit) { save(getString(R.string.hb_resized_panel)); }
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
        String[] labels = {getString(R.string.hb_small_about_one_third_width), getString(R.string.hb_medium_about_half_width), getString(R.string.hb_large_about_three_quarters_width)};
        labels[selected - 1] += getString(R.string.hb_current);
        menu(defaults ? getString(R.string.hb_new_panel_size_133) : getString(R.string.hb_panel_size),
                defaults ? getString(R.string.hb_default_size_for_panels_added_later_existing_panels_keep_their_si)
                        : getString(R.string.hb_resize_this_panel_height_grows_with_size_and_the_grid_repacks_aro),
                labels, choice -> {
                    int size = choice + 1;
                    if (defaults) layout.defaultPanelSize = size;
                    else layout.setPanelSize(key, size);
                    save(defaults ? getString(R.string.hb_changed_new_panel_size) : getString(R.string.hb_changed_panel_size));
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
            for (int n = 4; n <= fit; n++) supported.add(n);
        } else {
            for (int n = 2; n <= fit; n++) supported.add(n);
        }
        int[] values = new int[supported.size()];
        String[] labels = new String[supported.size()];
        for (int i = 0; i < supported.size(); i++) {
            values[i] = supported.get(i);
            labels[i] = values[i] == 0 ? getString(R.string.hb_auto) : columns ? values[i] + getString(R.string.hb_columns) : values[i] + getString(R.string.hb_rows);
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
        overlay.setContentDescription((columns ? getString(R.string.hb_grid_columns_141) : getString(R.string.hb_grid_rows_142)) + " selector");
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1));updateUndoHint();
        renderGridSelector();
    }
    private void renderGridSelector() {
        if (gridSelector == null || overlay == null) return;
        overlay.removeAllViews();
        GridSelector selector = gridSelector;
        boolean columns = selector.columns;
        int fit = columns ? homeGridFitColumns() : homeGridFitRows();
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL); panel.setGravity(Gravity.CENTER);
        panel.setPadding(dp(32), dp(24), dp(32), dp(22));
        panel.setBackground(shape(paletteColors.raised, paletteColors.edge, 2));
        TextView title = text(columns ? getString(R.string.hb_grid_columns_141) : getString(R.string.hb_grid_rows_142), 26);
        title.setTypeface(null, Typeface.BOLD); title.setGravity(Gravity.CENTER);
        panel.addView(title, new LinearLayout.LayoutParams(-1, dp(42)));
        TextView subtitle = text(getString(columns ? R.string.hb_grid_preview_fit_columns : R.string.hb_grid_preview_fit_rows, fit), 16);
        subtitle.setTextColor(paletteColors.muted); subtitle.setGravity(Gravity.CENTER);
        panel.addView(subtitle, new LinearLayout.LayoutParams(-1, dp(34)));

        LinearLayout choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.HORIZONTAL); choices.setGravity(Gravity.CENTER);
        for (int offset = -1; offset <= 1; offset++) {
            int index = selector.index + offset;
            LinearLayout choice = new LinearLayout(this); choice.setOrientation(LinearLayout.VERTICAL);
            choice.setGravity(Gravity.CENTER);
            boolean selected = offset == 0;
            if (selected) choice.setBackground(shape(paletteColors.surface, paletteColors.text, 2));
            if (index >= 0 && index < selector.values.length) {
                int value = selector.values[index] == 0 ? fit : selector.values[index];
                int previewColumns = columns ? value : homeGridColumns();
                int previewRows = columns ? homeGridRows() : value;
                GridSizePreview preview = new GridSizePreview(this, previewColumns, previewRows, columns, selected, paletteColors);
                choice.addView(preview, new LinearLayout.LayoutParams(-1, 0, 1f));
                String label = selector.values[index] == 0 ? getString(R.string.hb_grid_preview_auto, previewColumns, previewRows) : selector.labels[index];
                TextView number = text((offset < 0 ? "‹  " : "") + label + (offset > 0 ? "  ›" : ""), selected ? 22 : 17);
                number.setGravity(Gravity.CENTER); number.setTextColor(selected ? paletteColors.text : paletteColors.muted);
                number.setSingleLine(true); number.setEllipsize(android.text.TextUtils.TruncateAt.END);
                choice.addView(number, new LinearLayout.LayoutParams(-1, dp(44)));
                choice.setContentDescription(getString(R.string.hb_grid_preview_dimensions, previewColumns, previewRows));
            } else {
                TextView end = text(getString(R.string.hb_end), 17); end.setGravity(Gravity.CENTER); end.setTextColor(paletteColors.muted);
                choice.addView(end, new LinearLayout.LayoutParams(-1, -1));
            }
            LinearLayout.LayoutParams choiceLp = new LinearLayout.LayoutParams(0, -1, selected ? 1.3f : 1f);
            choiceLp.setMargins(dp(4), 0, dp(4), 0); choices.addView(choice, choiceLp);
        }
        LinearLayout.LayoutParams choicesLp = new LinearLayout.LayoutParams(-1, 0, 1f);
        choicesLp.topMargin = dp(12); panel.addView(choices, choicesLp);

        TextView hint = text(getString(R.string.hb_hint_change_ok_apply_back_cancel), 15);
        hint.setTextColor(paletteColors.muted); hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(-1, dp(34));
        hintLp.topMargin = dp(14); panel.addView(hint, hintLp);
        int width = Math.min(dp(780), dashboardWidthPixels() - dp(80));
        int height = Math.min(dp(420), dashboardHeightPixels() - dp(48));
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
            save(selector.columns ? getString(R.string.hb_changed_home_grid_columns) : getString(R.string.hb_changed_home_grid_rows));
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
        openMenu(new MenuSpec(getString(R.string.hb_about_homeboard_tv), this::showAboutDialog));
    }
    /** Null for an ordinary Activity; proof implementations return observed surface callbacks. */
    @Override protected int[] dashboardRenderingSurface() { return super.dashboardRenderingSurface(); }
    private RenderingStatus renderingStatus() {
        int outputWidth = 0, outputHeight = 0; float refresh = 0;
        try {
            android.view.Display display = getWindowManager().getDefaultDisplay();
            if (display != null) {
                android.view.Display.Mode mode = display.getMode();
                outputWidth = mode.getPhysicalWidth(); outputHeight = mode.getPhysicalHeight();
                refresh = mode.getRefreshRate();
            }
        } catch (RuntimeException unavailable) { /* Keep missing observations unknown. */ }
        boolean attached = root != null && root.isAttachedToWindow();
        return new RenderingStatus(attached ? root.getWidth() : 0, attached ? root.getHeight() : 0,
                dashboardRenderingSurface(), outputWidth, outputHeight, refresh);
    }
    private String renderingSize(RenderingStatus.Size size) {
        if (!size.known()) return getString(R.string.hb_render_unavailable);
        String pixels = getString(R.string.hb_render_size, size.width, size.height);
        return size.is4k() ? getString(R.string.hb_render_4k, pixels) : pixels;
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
        TextView version = text(getString(R.string.hb_version) + appVersion() + getString(R.string.hb_hint_your_apps_and_widgets_arranged_your_way), 16);
        version.setTextColor(paletteColors.muted);
        titleBlock.addView(title); titleBlock.addView(version);
        panel.addView(heading, new LinearLayout.LayoutParams(-1, dp(72)));

        RenderingStatus rendering = renderingStatus();
        panel.addView(text(getString(R.string.hb_render_dashboard, renderingSize(rendering.dashboard)), 16));
        if (rendering.surface != null)
            panel.addView(text(getString(R.string.hb_render_surface, renderingSize(rendering.surface)), 16));
        String output = renderingSize(rendering.output);
        if (rendering.output.known() && Float.isFinite(rendering.refreshRate) && rendering.refreshRate > 0)
            output = getString(R.string.hb_render_refresh, output,
                    new java.text.DecimalFormat("0.##").format(rendering.refreshRate));
        panel.addView(text(getString(R.string.hb_render_output, output), 16));

        TextView purpose = text(getString(R.string.hb_a_customizable_home_screen_for_your_tv_built_on_cleanyourtv), 16);
        purpose.setPadding(0, dp(8), 0, dp(8));
        panel.addView(purpose);
        TextView sourcesHeading = text(getString(R.string.hb_project_sources_licenses), 13);
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

        TextView closeText = text(getString(R.string.hb_hint_back), 15); closeText.setTypeface(null, Typeface.BOLD);
        closeText.setTextColor(paletteColors.text); closeText.setGravity(Gravity.CENTER);
        closeText.setFocusable(true); closeText.setClickable(true);
        closeText.setPadding(dp(14), 0, dp(14), 0);
        closeText.setBackground(tileBackground());
        closeText.setOnClickListener(v -> backMenu());
        aboutBackButton = closeText;
        LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(-2, dp(42));
        backLp.topMargin = dp(8); panel.addView(closeText, backLp);

        int width = Math.max(1, Math.min(dp(900), dashboardWidthPixels() - dp(48)));
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.setBackground(shape(paletteColors.raised, paletteColors.edge, 2));
        scroll.addView(panel, new android.widget.ScrollView.LayoutParams(-1, -2));
        overlay.addView(scroll, new FrameLayout.LayoutParams(width,
                Math.max(1, Math.min(dp(500), dashboardHeightPixels() - dp(32))), Gravity.CENTER));
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1));updateUndoHint();
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
        int width = Math.max(1, Math.min(dp(880), dashboardWidthPixels() - dp(64)));
        overlay.addView(panel, new FrameLayout.LayoutParams(width, -2, Gravity.CENTER));
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1));updateUndoHint(); aboutBackButton.requestFocus();
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
    /** Home settings → Home button → Make Homeboard the Home app, offered only where no launcher outranks the role. */
    private void chooseDefaultHomeApp() {
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            RoleManager roles = getSystemService(RoleManager.class);
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME)) {
                try {
                    returningToHomeButton = true;
                    startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_HOME), REQUEST_HOME_ROLE);
                    return;
                } catch (RuntimeException unavailable) {
                    // Some TV builds expose the role but only implement the system Home settings screen.
                    returningToHomeButton = false;
                }
            }
        }
        Intent chooser = new Intent(Settings.ACTION_HOME_SETTINGS);
        if (getPackageManager().resolveActivity(chooser, PackageManager.MATCH_DEFAULT_ONLY) != null) {
            try { returningToHomeButton = true; startActivityForResult(chooser, REQUEST_HOME_ROLE); }
            catch (RuntimeException unavailable) { returningToHomeButton = false; toast(getString(R.string.hb_android_could_not_open_home_app_selection)); }
        }
        else toast(getString(R.string.hb_android_does_not_provide_home_app_selection_on_this_device));
    }

    // Home button (docs/plans/2026-11-20-home-button.md): Home settings → Home button. The choice and the Android side
    // live in HomeButton; this screen shows what is true (HomeButtonRules.screen), records Undo and listens for the
    // helper connecting (HomeKeyService.stateListener), so "Turning on…" ends on the helper's own signal.
    private static final String UNDO_HOME_BUTTON = "home-button";
    /** Failure guard only: how long "Turning on…" waits for the helper's connect signal before showing Not running. */
    private static final long HOME_BUTTON_START_GUARD_MS = 8_000L;
    private MenuSpec homeButtonMenu;
    private boolean homeButtonWaiting, homeButtonShowQr, homeButtonChecked, returningToHomeButton, homeButtonDropNoticeShown;
    private String homeButtonFailure;
    private final Runnable homeButtonGiveUp = () -> { homeButtonWaiting = false; redrawHomeButton(null); };

    /** Android's label for the launcher Home opens, such as "Google TV Home"; a plain phrase when unknown. */
    private String homeButtonLauncherName(HomeTarget.Result home) {
        if (home.target == null) return getString(R.string.hb_hb_launcher_fallback);
        try {
            PackageManager pm = getPackageManager();
            return String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(home.target, 0)));
        } catch (Exception unknown) { return getString(R.string.hb_hb_launcher_fallback); }
    }
    private String homeButtonMenuLabel() {
        HomeTarget.Result home = HomeButton.home(this);
        HomeButtonRules.Screen screen = HomeButton.screen(this, home, homeButtonWaiting);
        if (screen == HomeButtonRules.Screen.CHOOSER) return getString(R.string.hb_hb_menu_choose);
        switch (HomeButtonRules.summary(screen)) {
            case HOMEBOARD: return getString(R.string.hb_hb_menu_homeboard);
            case OTHER_LAUNCHER: return getString(R.string.hb_hb_menu_other, homeButtonLauncherName(home));
            default: return getString(R.string.hb_hb_menu_attention);
        }
    }
    private void homeButtonSettings() {
        homeButtonShowQr = false; homeButtonChecked = false; homeButtonFailure = null; screensaverFocusTag = null;
        homeButtonMenu = new MenuSpec(getString(R.string.hb_hb_title), this::drawHomeButton);
        openMenu(homeButtonMenu);
    }
    private boolean homeButtonOpen() { return homeButtonMenu != null && activeMenu == homeButtonMenu && overlay != null; }
    private void redrawHomeButton(String focusTag) {
        if (!homeButtonOpen()) return;
        screensaverFocusTag = focusTag; activeMenu.panelRenderer.run();
    }
    /** HomeKeyService connected or went away; posted to the main thread. */
    private void homeButtonHelperChanged() {
        if (HomeButton.running()) { homeButtonWaiting = false; handler.removeCallbacks(homeButtonGiveUp); }
        redrawHomeButton(null);
    }
    /** onResume: put a dropped helper back where Homeboard may, reopen the screen after Android's own, or say once that it is off. */
    private void resumeHomeButton() {
        HomeButton.repair(this);
        if (returningToHomeButton) {
            returningToHomeButton = false;
            handler.post(() -> {
                if (!active || layoutReadOnly) return;
                homeSettings();
                invokingMenuAction = true;
                try { homeButtonSettings(); } finally { invokingMenuAction = false; }
            });
            return;
        }
        HomeTarget.Result home = HomeButton.home(this);
        boolean dropped = HomeButtonRules.droppedNotice(HomeButton.wanted(this), home, HomeButton.inList(this), HomeButton.mayWrite(this), HomeButton.running());
        if (dropped && !homeButtonDropNoticeShown) toast(getString(R.string.hb_hb_dropped, homeButtonLauncherName(home)));
        homeButtonDropNoticeShown = dropped;
    }
    /** On or Off: saved at once with Undo; Android's side follows when Homeboard may write it. */
    private void changeHomeButton(boolean on) {
        if (layoutReadOnly) { toast(getString(R.string.hb_aw_newer_layout)); return; }
        boolean before = HomeButton.wanted(this);
        if (before == on) { redrawHomeButton(on ? "hb:on" : "hb:off"); return; }
        HomeTarget.Result home = HomeButton.home(this);
        HomeButton.Outcome outcome = HomeButton.set(this, on);
        if (outcome == HomeButton.Outcome.SAVE_FAILED) { toast(getString(R.string.hb_configuration_save_failed)); return; }
        String label = on ? getString(R.string.hb_hb_undo_on) : getString(R.string.hb_hb_undo_off, homeButtonLauncherName(home));
        try { pushUndo(UNDO_HOME_BUTTON, "", new JSONObject().put("wanted", before).toString(), label); }
        catch (org.json.JSONException impossible) { throw new IllegalStateException(impossible); }
        showUndoBanner(label);
        homeButtonFailure = outcome == HomeButton.Outcome.REFUSED ? getString(R.string.hb_hb_refused) : null;
        homeButtonWaiting = on && outcome == HomeButton.Outcome.WROTE && !HomeButton.running();
        handler.removeCallbacks(homeButtonGiveUp);
        if (homeButtonWaiting) handler.postDelayed(homeButtonGiveUp, HOME_BUTTON_START_GUARD_MS);
        redrawHomeButton(on ? "hb:on" : "hb:off");
    }
    private void restartHomeButton() {
        HomeButton.restart(this);
        homeButtonWaiting = true; handler.removeCallbacks(homeButtonGiveUp);
        handler.postDelayed(homeButtonGiveUp, HOME_BUTTON_START_GUARD_MS);
        redrawHomeButton("hb:restart");
    }
    private void openAccessibilitySettings() {
        returningToHomeButton = true;
        try { startActivity(SystemSettings.intent(this, Settings.ACTION_ACCESSIBILITY_SETTINGS)); }
        catch (RuntimeException unavailable) { returningToHomeButton = false; toast(getString(R.string.hb_android_could_not_open_that_action)); }
    }
    /** The one-time setup command, shared with the screensaver's Use everywhere, and its QR code when asked. */
    private void setupCommand(LinearLayout box, boolean showQr) {
        String command = "adb shell pm grant " + getPackageName() + " android.permission.WRITE_SECURE_SETTINGS";
        TextView code = text(command, 17); code.setTypeface(Typeface.MONOSPACE); code.setPadding(dp(14), dp(10), dp(14), dp(10));
        code.setBackground(shape(0xFF0B0F17, paletteColors.edge, 1)); code.setTextColor(0xFFE6EDF3);
        box.addView(code, new LinearLayout.LayoutParams(-1, -2));
        if (!showQr) return;
        try {
            ImageView qr = new ImageView(this); qr.setImageBitmap(PhoneEditor.qr(command));
            qr.setContentDescription(getString(R.string.hb_ss_qr_note));
            LinearLayout qrRow = new LinearLayout(this); qrRow.setGravity(Gravity.CENTER_VERTICAL); qrRow.setPadding(0, dp(8), 0, 0);
            qrRow.addView(qr, new LinearLayout.LayoutParams(dp(150), dp(150)));
            TextView note = muted(getString(R.string.hb_ss_qr_note), 15); note.setPadding(dp(14), 0, 0, 0);
            qrRow.addView(note, new LinearLayout.LayoutParams(0, -2, 1));
            box.addView(qrRow);
        } catch (Exception unavailable) { /* No QR code; the command is still shown as text. */ }
    }
    private TextView homeButtonParagraph(LinearLayout box, String value, boolean quiet) {
        TextView v = quiet ? muted(value, 16) : text(value, 17); v.setPadding(0, dp(6), 0, dp(2)); box.addView(v); return v;
    }
    /** Home settings → Home button, in whichever HomeButtonRules.Screen is true now. */
    private void drawHomeButton() {
        HomeTarget.Result home = HomeButton.home(this);
        boolean wanted = HomeButton.wanted(this), mayWrite = HomeButton.mayWrite(this);
        HomeButtonRules.Screen screen = HomeButton.screen(this, home, homeButtonWaiting);
        String launcher = homeButtonLauncherName(home), helper = getString(R.string.home_key_name);
        LinearLayout body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL);
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(14), dp(22), dp(14)); box.setBackground(shape(paletteColors.surface, paletteColors.edge, 1));
        body.addView(box, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout panel = screensaverPanel(getString(R.string.hb_hb_title), body);
        LinearLayout actions = new LinearLayout(this); actions.setPadding(0, dp(10), 0, 0);
        View first = null;
        if (homeButtonFailure != null) { TextView fail = homeButtonParagraph(box, homeButtonFailure, false); fail.setTextColor(0xFFFFB4B4); }
        boolean helperRoute = screen != HomeButtonRules.Screen.NOT_NEEDED && screen != HomeButtonRules.Screen.CHOOSER;
        if (!helperRoute) {
            box.addView(bold(getString(screen == HomeButtonRules.Screen.NOT_NEEDED ? R.string.hb_hb_not_needed : R.string.hb_hb_role), 22));
            homeButtonParagraph(box, getString(screen == HomeButtonRules.Screen.NOT_NEEDED ? R.string.hb_hb_not_needed_body : R.string.hb_hb_chooser_body), false);
            if (screen == HomeButtonRules.Screen.CHOOSER) first = everywhereButton(actions, getString(R.string.hb_hb_role), "hb:role", this::chooseDefaultHomeApp);
        }
        // The switch, shown wherever the helper route applies, and also where it no longer does but is still on.
        if (helperRoute || wanted) {
            LinearLayout row = optionRow(box, getString(R.string.hb_hb_switch), true);
            TextView on = segment(getString(R.string.hb_hb_on), wanted, "hb:on", () -> changeHomeButton(true));
            TextView off = segment(getString(R.string.hb_hb_off), !wanted, "hb:off", () -> changeHomeButton(false));
            addSegment(row, on); addSegment(row, off);
            String status = null; int fill = paletteColors.focus;
            switch (screen) {
                case WORKING: status = getString(R.string.hb_hb_working); break;
                case STARTING: status = getString(R.string.hb_hb_starting); fill = 0xFF6F86A6; break;
                case NOT_RUNNING: status = getString(R.string.hb_hb_not_running); fill = 0xFFE07A84; break;
                case SETUP: status = getString(R.string.hb_hb_setup); fill = 0xFFE0B24A; break;
                default: break;
            }
            if (status != null) {
                TextView badge = badge(status, fill, contrastingText(fill));
                LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(-2, -2); badgeLp.leftMargin = dp(10); row.addView(badge, badgeLp);
            }
            if (first == null) first = wanted ? on : off;
        }
        switch (screen) {
            case OFF:
                homeButtonParagraph(box, getString(R.string.hb_hb_off_body, launcher), false);
                if (home.roleHonoured) {
                    homeButtonParagraph(box, getString(R.string.hb_hb_role_body, launcher), true);
                    everywhereButton(actions, getString(R.string.hb_hb_role), "hb:role", this::chooseDefaultHomeApp);
                } else homeButtonParagraph(box, getString(R.string.hb_hb_role_google, launcher), true);
                break;
            case WORKING: homeButtonParagraph(box, getString(R.string.hb_hb_working_body, launcher), false); break;
            case STARTING: homeButtonParagraph(box, getString(R.string.hb_hb_starting_body), false); break;
            case NOT_RUNNING:
                homeButtonParagraph(box, getString(R.string.hb_hb_not_running_body, launcher), false);
                if (HomeButton.inList(this)) homeButtonParagraph(box, getString(R.string.hb_hb_not_running_listed), true);
                if (mayWrite) everywhereButton(actions, getString(R.string.hb_hb_start_again), "hb:restart", this::restartHomeButton);
                everywhereButton(actions, getString(R.string.hb_hb_android_settings), "hb:android", this::openAccessibilitySettings);
                if (!mayWrite) homeButtonParagraph(box, getString(R.string.hb_hb_android_hint, helper), true);
                break;
            case SETUP:
                homeButtonParagraph(box, getString(R.string.hb_hb_setup_body), false);
                setupCommand(box, homeButtonShowQr);
                homeButtonParagraph(box, getString(R.string.hb_hb_android_hint, helper), true);
                homeButtonParagraph(box, getString(R.string.hb_hb_setup_nothing, launcher), true);
                if (homeButtonChecked) homeButtonParagraph(box, getString(R.string.hb_ss_setup_still), true);
                everywhereButton(actions, getString(R.string.hb_hb_android_settings), "hb:android", this::openAccessibilitySettings);
                everywhereButton(actions, getString(R.string.hb_ss_retry), "hb:retry", () -> { homeButtonChecked = true; retryHomeButton(); });
                everywhereButton(actions, getString(homeButtonShowQr ? R.string.hb_ss_hide_qr : R.string.hb_ss_show_qr), "hb:qr",
                        () -> { homeButtonShowQr = !homeButtonShowQr; redrawHomeButton("hb:qr"); });
                break;
            default: break;
        }
        if (actions.getChildCount() > 0) body.addView(actions, new LinearLayout.LayoutParams(-1, -2));
        if (helperRoute || wanted) {
            TextView how = muted(getString(R.string.hb_hb_how) + "\n" + getString(R.string.hb_hb_recovery, helper, launcher), 13);
            LinearLayout.LayoutParams howLp = new LinearLayout.LayoutParams(-1, -2); howLp.topMargin = dp(8); body.addView(how, howLp);
        }
        if (first == null) first = panel.findViewWithTag("back");
        focusScreensaver(panel, first);
    }
    /** Retry on the setup screen: if the one-time setup is now done, turn the helper on as the setting asks. */
    private void retryHomeButton() {
        if (HomeButton.mayWrite(this) && HomeButton.wanted(this)) {
            HomeButton.Outcome outcome = HomeButton.set(this, true);
            homeButtonFailure = outcome == HomeButton.Outcome.REFUSED ? getString(R.string.hb_hb_refused) : null;
            homeButtonWaiting = outcome == HomeButton.Outcome.WROTE && !HomeButton.running();
            handler.removeCallbacks(homeButtonGiveUp);
            if (homeButtonWaiting) handler.postDelayed(homeButtonGiveUp, HOME_BUTTON_START_GUARD_MS);
        }
        redrawHomeButton("hb:retry");
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
            menu(getString(R.string.hb_restore_hidden_app), getString(R.string.hb_no_hidden_app_shortcuts_apps_remain_installed_when_a_shortcut_is_),
                    new String[0], ignored -> {});
            return;
        }
        String[] labels = new String[keys.size()];
        for (int i = 0; i < keys.size(); i++) labels[i] = apps.containsKey(keys.get(i)) ? apps.get(keys.get(i)).label : keys.get(i).substring(4);
        menu(getString(R.string.hb_restore_hidden_app), getString(R.string.hb_restores_this_shortcut_to_all_apps_it_does_not_reinstall_the_app), labels, choice -> {
            String restored = labels[choice];
            layout.hidden.remove(keys.get(choice)); save(getString(R.string.hb_hint_restored) + restored + getString(R.string.hb_hint_to_all_apps)); render(0);
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
        for (RainWidgetController widget : rainWidgets.values()) widget.stop();
        for(WorldClockWidgetView widget:clockWidgets.values())widget.stop();
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
        int screenWidthDp = Math.round(dashboardWidthPixels() / density);
        int screenHeightDp = Math.round(dashboardHeightPixels() / density);
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
                    String cardLabel = spec.paletteGrid && isSelected ? label + getString(R.string.hb_current) : label;
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
            View previous = menuPagerControl(getString(R.string.hb_previous), MenuIconView.Kind.PREVIOUS, page > 0);
            previous.setOnClickListener(v -> {
                if (page > 0) showMenuPage(spec, page - 1,
                        Math.max((page - 1) * spec.pageSize, spec.focusedChoice - spec.pageSize));
            });
            TextView countLabel = text(getString(R.string.hb_page) + (page + 1) + getString(R.string.hb_of) + pageCount, 15);
            countLabel.setGravity(Gravity.CENTER);
            View next = menuPagerControl(getString(R.string.hb_next), MenuIconView.Kind.NEXT, page + 1 < pageCount);
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
            TextView hint = text(getString(R.string.hb_move_to_preview_a_color_back_returns), 14);
            hint.setTextColor(paletteColors.muted); hints.addView(hint);
        } else {
            hints.addView(new MenuIconView(this, MenuIconView.Kind.CONFIRM, paletteColors), new LinearLayout.LayoutParams(dp(20), dp(20)));
            TextView selectHint = text(getString(R.string.hb_ok_more_back), 13); selectHint.setTextColor(paletteColors.muted);
            LinearLayout.LayoutParams selectLp = new LinearLayout.LayoutParams(-2, dp(26)); selectLp.leftMargin = dp(5);
            hints.addView(selectHint, selectLp);
            hints.addView(new MenuIconView(this, MenuIconView.Kind.CANCEL, paletteColors), new LinearLayout.LayoutParams(dp(20), dp(20)));
            TextView backHint = text(getString(R.string.hb_back_parent), 13); backHint.setTextColor(paletteColors.muted);
            LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(-2, dp(26)); backLp.leftMargin = dp(5);
            hints.addView(backHint, backLp);
        }
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(-1, footerHeight);
        hintLp.topMargin = dp(3); panel.addView(hints, hintLp);

        FrameLayout.LayoutParams panelLp = new FrameLayout.LayoutParams(panelWidth, panelHeight, Gravity.CENTER);
        overlay.addView(panel, panelLp);
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1));updateUndoHint();
        if (firstFocus != null) firstFocus.requestFocus();
    }

    private void showPieMenuPage(MenuSpec spec, int page, int focusChoice) {
        spec.page = page;
        surface.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        surface.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        if (overlay != null) root.removeView(overlay);
        hideAppFocusHighlight();
        overlay = new FrameLayout(this);
        boolean preview = wallpaperPreviewActive();
        overlay.setBackgroundColor(preview ? Color.TRANSPARENT : paletteColors.scrim);
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
        String centerAction = spec.labels.length == 0 ? getString(R.string.hb_back) : spec.pageCount > 1
                ? page + 1 < spec.pageCount ? getString(R.string.hb_more_164) : getString(R.string.hb_first_page)
                : menuStack.isEmpty() ? getString(R.string.hb_close) : getString(R.string.hb_back);
        String pageHint = spec.pageCount > 1 ? getString(R.string.hb_page) + (page + 1) + getString(R.string.hb_of) + spec.pageCount : "";
        String breadcrumb = getString(R.string.hb_provider_background).equals(spec.title) || spec.labels.length == 0 ? spec.subtitle : spec.labels.length == 0 && getString(R.string.hb_restore_hidden_app).equals(spec.title)
                ? getString(R.string.hb_no_hidden_app_shortcuts)
                : menuStack.isEmpty() ? spec.subtitle
                : menuStack.get(menuStack.size() - 1).title + " › " + spec.title;
        String interactionHint = spec.pageCount > 1
                ? getString(R.string.hb_page) + (page + 1) + getString(R.string.hb_of) + spec.pageCount + getString(R.string.hb_ok_changes_page)
                : getString(R.string.hb_directions_choose_back_returns);
        if (spec.labels.length == 0 && getString(R.string.hb_restore_hidden_app).equals(spec.title))
            interactionHint = getString(R.string.hb_hint_apps_stay_installed_back_returns);
        PieMenuView pie = new PieMenuView(this, paletteColors);
        if (getString(R.string.hb_provider_background).equals(spec.title) && wallpaperProvider != null) interactionHint = wallpaperProvider.readiness();
        pie.setContent(spec.title, breadcrumb, centerAction, interactionHint, labels, kinds, selected,
                new PieMenuView.Listener() {
                    @Override public void onArm(int arm) {
                        int choice = spec.page * spec.pageSize + arm;
                        if (choice < spec.labels.length) selectMenuChoice(spec, spec.page, choice);
                    }
                    @Override public void onCenter() { menuCenter(spec); }
                });
        int width = Math.round(dashboardWidthPixels() - dp(56));
        int height = Math.round(dashboardHeightPixels() - dp(40));
        int diameter = Math.max(dp(360), Math.min(width, height));
        diameter = Math.min(diameter, dp(760));
        if (preview) diameter = Math.min(diameter, dashboardWidthPixels() / 2 - dp(32));
        FrameLayout.LayoutParams pieLp = new FrameLayout.LayoutParams(diameter, diameter,
                preview ? Gravity.RIGHT | Gravity.CENTER_VERTICAL : Gravity.CENTER);
        if (preview) pieLp.rightMargin = dp(16);
        overlay.addView(pie, pieLp);
        if (preview) {
            String hint = getString(R.string.hb_provider_background).equals(spec.title)
                    ? "\n" + getString(R.string.hb_wallpaper_preview_dashboard_hidden) : "";
            TextView caption = text(wallpaperStatus + hint, 17);
            caption.setPadding(dp(14), dp(10), dp(14), dp(10));
            UiBackgroundDrawable fill = shape(paletteColors.raised, paletteColors.edge, 1);
            fill.setFillOpacity(WallpaperSettings.PIE_BACKGROUND_OPACITY);
            caption.setBackground(fill);
            FrameLayout.LayoutParams captionLp = new FrameLayout.LayoutParams(
                    dashboardWidthPixels() / 2 - dp(40), -2, Gravity.TOP | Gravity.LEFT);
            captionLp.leftMargin = dp(16); captionLp.topMargin = dp(16);
            overlay.addView(caption, captionLp);
        }
        if(!spec.explanation.isEmpty()&&spec.labels.length<=2){
            TextView explanation=text(spec.explanation,18);explanation.setPadding(dp(12),dp(12),dp(12),dp(12));
            explanation.setAutoSizeTextTypeUniformWithConfiguration(12,18,1,android.util.TypedValue.COMPLEX_UNIT_SP);
            explanation.setGravity(Gravity.CENTER_VERTICAL);UiBackgroundDrawable helpBackground=shape(paletteColors.raised,paletteColors.edge,1);helpBackground.setFillOpacity(WallpaperSettings.PIE_BACKGROUND_OPACITY);explanation.setBackground(helpBackground);
            FrameLayout.LayoutParams help=new FrameLayout.LayoutParams(Math.min(dp(250),dashboardWidthPixels()/3),dashboardHeightPixels()-dp(32),Gravity.LEFT|Gravity.TOP);
            help.leftMargin=dp(16);help.topMargin=dp(16);overlay.addView(explanation,help);
        }
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1));updateUndoHint();
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
            if(androidWidgets!=null)androidWidgets.menuDismissed();
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
    private boolean matchesMenuLabel(String label, int... resources) {
        for (int id : resources) {
            String text = getString(id).trim();
            if (label.equals(text) || label.startsWith(text + " · ")) return true;
        }
        return false;
    }
    private MenuIconView.Kind menuIconFor(String title, String label) {
        if(matchesMenuLabel(label,R.string.hb_wc_title,R.string.hb_wc_configure,R.string.hb_screensaver))return MenuIconView.Kind.CLOCK;
        if (paletteIdForLabel(label) != null) return MenuIconView.Kind.PALETTE;
        if (matchesMenuLabel(label, R.string.hb_saved_layout_could_not_be_read_starting_with_empty_home, R.string.hb_homeboard_change, R.string.hb_homeboard_settings, R.string.hb_added_app_to_home, R.string.hb_home_settings, R.string.hb_hb_title, R.string.hb_a_customizable_home_screen_for_your_tv_built_on_cleanyourtv, R.string.hb_homeboard_tv_source, R.string.hb_android_could_not_open_home_app_selection, R.string.hb_android_does_not_provide_home_app_selection_on_this_device)) return MenuIconView.Kind.HOME;
        if (matchesMenuLabel(label, R.string.hb_undo, R.string.hb_undo_5, R.string.hb_undo_history, R.string.hb_no_recent_homeboard_changes_can_be_undone, R.string.hb_undo_9, R.string.hb_newest_first_undoing_an_older_change_also_reverses_newer_changes, R.string.hb_could_not_undo_that_change, R.string.hb_delete_removes_this_widget_panel_undo_restores_it)) return MenuIconView.Kind.UNDO;
        if (matchesMenuLabel(label, R.string.hb_could_not_refresh_installed_apps, R.string.hb_air_quality_widget, R.string.hb_refresh_reading, R.string.hb_air_quality_panel, R.string.hb_air_quality, R.string.hb_air_quality_location_each_widget_is_independent, R.string.hb_air_quality_location, R.string.hb_refresh_failed_saved_reading, R.string.hb_nok_refresh_or_configure)) return MenuIconView.Kind.AIR;
        if (matchesMenuLabel(label, R.string.hb_device_settings, R.string.hb_plugin_settings, R.string.hb_no_accessible_plugin_settings, R.string.hb_plugin_settings_are_unavailable, R.string.hb_host_display_options_provider_content_stays_in_plugin_settings, R.string.hb_language)) return MenuIconView.Kind.SETTINGS;
        if (matchesMenuLabel(label, R.string.hb_move_preview_arrows_place_anywhere_in_the_grid_ok_saves_back_canc, R.string.hb_move_preview_arrows_reposition_ok_saves_back_cancels, R.string.hb_keep_app, R.string.hb_narrower_shorter_wider_taller_ok_save_back_cancel, R.string.hb_back_cancels, R.string.hb_cancel)) return MenuIconView.Kind.CANCEL;
        if (matchesMenuLabel(label, R.string.hb_app_unavailable_hold_ok_to_remove_this_tile, R.string.hb_delete_widget, R.string.hb_remove_from_home, R.string.hb_removed_app_from_home)) return MenuIconView.Kind.DELETE;
        if (matchesMenuLabel(label, R.string.hb_configure_location_and_source, R.string.hb_bangkok_monitoring_station, R.string.hb_location_unavailable, R.string.hb_bangkok_monitoring_station_228, R.string.hb_getting_device_location, R.string.hb_configure_location)) return MenuIconView.Kind.LOCATION;
        if (matchesMenuLabel(label, R.string.hb_move, R.string.hb_moved_item, R.string.hb_move_to_a_color_to_preview_it, R.string.hb_banners_use_wider_cells_and_taller_rows_tiles_may_move_to_another, R.string.hb_choose_grid_density_new_panel_size_or_screen_movement, R.string.hb_choose_how_home_and_all_apps_move, R.string.hb_move_to_preview_a_color_back_returns)) return MenuIconView.Kind.MOVE;
        if (matchesMenuLabel(label, R.string.hb_resize, R.string.hb_resized_panel, R.string.hb_resize_this_panel_height_grows_with_size_and_the_grid_repacks_aro)) return MenuIconView.Kind.RESIZE;
        if (matchesMenuLabel(label, R.string.hb_unfavorite, R.string.hb_favorite, R.string.hb_favorite_an_app_from_all_apps, R.string.hb_home_favorites, R.string.hb_home_favorites_109, R.string.hb_home_favorites_111)) return MenuIconView.Kind.FAVORITE;
        if (matchesMenuLabel(label, R.string.hb_uninstall_app, R.string.hb_android_does_not_allow_uninstalling_this_app_here)) return MenuIconView.Kind.UNINSTALL;
        if (matchesMenuLabel(label, R.string.hb_widgets_restore)) return MenuIconView.Kind.WIDGET;
        if (matchesMenuLabel(label, R.string.hb_appearance)) return MenuIconView.Kind.APPEARANCE;
        if (matchesMenuLabel(label, R.string.hb_home_behavior)) return MenuIconView.Kind.BEHAVIOR;
        if (matchesMenuLabel(label, R.string.hb_about_homeboard_tv, R.string.hb_small_about_one_third_width, R.string.hb_medium_about_half_width, R.string.hb_large_about_three_quarters_width)) return MenuIconView.Kind.INFO;
        if (matchesMenuLabel(label, R.string.hb_add_a_widget_or_restore_an_app_shortcut, R.string.hb_restore_hidden_app, R.string.hb_restores_this_shortcut_to_all_apps_it_does_not_reinstall_the_app)) return MenuIconView.Kind.RESTORE;
        if (matchesMenuLabel(label, R.string.hb_add_widget, R.string.hb_add_to_home)) return MenuIconView.Kind.ADD;
        if (matchesMenuLabel(label, R.string.hb_customize_backgrounds_and_dashboard_elements, R.string.hb_background, R.string.hb_changed_background_color, R.string.hb_changed_background, R.string.hb_provider_background, R.string.hb_projectivy_wallpaper_provider_selected, R.string.hb_background_details, R.string.hb_no_providers_installed_install_a_wallpaper_provider_app_then_retu, R.string.hb_use_background, R.string.hb_use_background_preview_first, R.string.hb_loading_wallpaper, R.string.hb_preview_a_usable_wallpaper_first, R.string.hb_background_189, R.string.hb_wallpaper_options, R.string.hb_wallpaper_rotation, R.string.hb_wallpaper_dimming, R.string.hb_open_wallpaper_link, R.string.hb_choose_background, R.string.hb_this_wallpaper_link_type_is_unsupported, R.string.hb_backgrounds)) return MenuIconView.Kind.WALLPAPER;
        if (matchesMenuLabel(label, R.string.hb_palette, R.string.hb_current_palette)) return MenuIconView.Kind.PALETTE;
        if (matchesMenuLabel(label, R.string.hb_icons, R.string.hb_choose_artwork_and_icon_shape, R.string.hb_dense_icons, R.string.hb_circular_icon_mask, R.string.hb_the_icon_keeps_the_same_size, R.string.hb_original_icon_shape, R.string.hb_changed_app_icon_shape, R.string.hb_small_icons)) return MenuIconView.Kind.ARTWORK;
        if (matchesMenuLabel(label, R.string.hb_tv_banners, R.string.hb_banners)) return MenuIconView.Kind.BANNER;
        if (matchesMenuLabel(label, R.string.hb_circular_mask)) return MenuIconView.Kind.CIRCLE;
        if (matchesMenuLabel(label, R.string.hb_tv_banners_responsive_grid, R.string.hb_launcher_icons_dense_grid, R.string.hb_grid_columns, R.string.hb_grid_rows, R.string.hb_grid_columns_141, R.string.hb_grid_rows_142, R.string.hb_changed_home_grid_columns, R.string.hb_changed_home_grid_rows, R.string.hb_nearest_model_grid_use_this_device, R.string.hb_enter_coordinates_for_a_model_grid, R.string.hb_choose_the_forecast_grid_near_this_location, R.string.hb_model_grid_location)) return MenuIconView.Kind.GRID;
        if (matchesMenuLabel(label, R.string.hb_reduced_motion, R.string.hb_reduced_motion_121, R.string.hb_changed_reduced_motion)) return MenuIconView.Kind.MOTION;
        if (matchesMenuLabel(label, R.string.hb_project_sources_licenses)) return MenuIconView.Kind.LICENSE;
        if (matchesMenuLabel(label, R.string.hb_previous, R.string.hb_back)) return MenuIconView.Kind.PREVIOUS;
        return MenuIconView.Kind.APP;
    }
    private String paletteIdForLabel(String label) {
        String normalized = label.replace(getString(R.string.hb_current_171), "").trim();
        if (normalized.equals(getString(R.string.hb_midnight))) return "midnight";
        if (normalized.equals(getString(R.string.hb_forest))) return "forest";
        if (normalized.equals(getString(R.string.hb_ocean))) return "ocean";
        if (normalized.equals(getString(R.string.hb_plum))) return "plum";
        if (normalized.equals(getString(R.string.hb_sand))) return "sand";
        if (normalized.equals(getString(R.string.hb_rose))) return "rose";
        if (normalized.equals(getString(R.string.hb_teal))) return "teal";
        if (normalized.equals(getString(R.string.hb_slate))) return "slate";
        if (normalized.equals(getString(R.string.hb_sunset))) return "sunset";
        if (normalized.equals(getString(R.string.hb_black))) return "black";
        if (normalized.equals(getString(R.string.hb_charcoal))) return "charcoal";
        if (normalized.equals(getString(R.string.hb_indigo))) return "indigo";
        if (normalized.equals(getString(R.string.hb_cobalt))) return "cobalt";
        if (normalized.equals(getString(R.string.hb_lagoon))) return "lagoon";
        if (normalized.equals(getString(R.string.hb_pine))) return "pine";
        if (normalized.equals(getString(R.string.hb_moss))) return "moss";
        if (normalized.equals(getString(R.string.hb_amber))) return "amber";
        if (normalized.equals(getString(R.string.hb_copper))) return "copper";
        if (normalized.equals(getString(R.string.hb_cedar))) return "cedar";
        if (normalized.equals(getString(R.string.hb_emerald))) return "emerald";
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
        phonePanelOpen=false;
        favoritesBrowser=null;
        calendarDayEditor = false;
        backgroundInfo = false;
        backgroundStatusText = null;
        if (overlay == null) return;
        if (activeMenu != null && activeMenu.paletteGrid) finishUndoGroup();
        root.removeView(overlay); overlay = null;
        if (scalePreview != 0) {
            scalePreview = 0;
            scaleDraft = null;
            restoreScaleNavigation();
            render(0);
        }
        aboutBackButton = null;
        surface.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        surface.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
        restoreFocus();
        if (undoBanner != null) undoBanner.bringToFront();
        updateAir();
        handler.post(()->{if(active&&hasWindowFocus()&&overlay==null&&expandedWidget!=null&&expandedWidget.configuring())expandedWidget.resumeAfterConfiguration();});
    }
    private void backMenu() {
        if(androidWidgets!=null)androidWidgets.menuDismissed();
        if (overlay == null) return;
        if (adjustmentCancel != null) { backAdjustment(); return; }
        if (activeMenu != null && getString(R.string.hb_provider_background).equals(activeMenu.title)) cancelWallpaperPreview();
        if (activeMenu != null && activeMenu.page > 0) {
            int page = activeMenu.page - 1;
            int focus = Math.min(page * activeMenu.pageSize, activeMenu.labels.length - 1);
            showMenuPage(activeMenu, page, focus);
            return;
        }
        if (!menuStack.isEmpty()) {
            dismissMenu();
            activeMenu = menuStack.remove(menuStack.size() - 1);
            if (getString(R.string.hb_background).equals(activeMenu.title)) activeMenu = backgroundMenuSpec();
            showMenuPage(activeMenu, activeMenu.page, activeMenu.focusedChoice);
        } else {
            dismissMenu(); activeMenu = null;
        }
    }
    private void open(Intent intent) {
        try { startActivity(intent); }
        catch (RuntimeException e) { toast(getString(R.string.hb_android_could_not_open_that_action)); }
    }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }

    @Override public void onUserInteraction() {
        super.onUserInteraction();
        if (wallpaperHost != null) wallpaperHost.userActivity();
        noteHomeInput();
    }
    private void syncBackground() {
        if (wallpaperHost == null) return;
        // A preview must not be hidden by the user's saved dashboard opacity or widgets.
        // Only view visibility changes; apply/cancel restore the exact saved appearance.
        boolean preview = wallpaperPreviewActive();
        if (surface != null) surface.setVisibility(preview ? View.INVISIBLE : View.VISIBLE);
        WallpaperSettings selected = wallpaperDraft == null || !wallpaperPreviewStarted ? layout.wallpaper : wallpaperDraft;
        wallpaperSurface.options(selected.solid() ? 0 : selected.dimPercent, reducedMotion());
        wallpaperHost.select(selected);
    }
    private boolean wallpaperPreviewActive() {
        return wallpaperDraft != null && wallpaperPreviewStarted;
    }
    private void applyUiBackgroundOpacity(int percent) {
        uiBackgroundOpacity = WallpaperSettings.percent(percent);
        for (AirQualityCard widget : airWidgets.values()) widget.setBackgroundOpacity(uiBackgroundOpacity);
        for (CalendarWidgetView widget : calendarWidgets.values()) widget.setFillOpacity(uiBackgroundOpacity);
        for(WorldClockWidgetView widget:clockWidgets.values())widget.setFillOpacity(uiBackgroundOpacity);
        for (RainWidgetController widget : rainWidgets.values()) widget.style(paletteColors, uiBackgroundOpacity, reducedMotion());
        for(String key:layout.widgets)if(key.startsWith("widget:"))UiBackgroundDrawable.apply(tiles.get(key),uiBackgroundOpacity);
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
    private int providerDirectoryPage, providerDirectoryFocus;
    private ProviderCatalog.Entry returningToProvider;
    private LinearLayout providerPanel(String title) {
        if (overlay != null) root.removeView(overlay);
        surface.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        overlay = new FrameLayout(this); overlay.setBackgroundColor(paletteColors.scrim);
        LinearLayout panel = new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(22),dp(18),dp(22),dp(18));
        panel.setBackground(shape(paletteColors.raised,paletteColors.edge,2));
        panel.addView(text(title,24));
        int width=Math.min(dp(900),dashboardWidthPixels()-dp(48));
        overlay.addView(panel,new FrameLayout.LayoutParams(width,-2,Gravity.CENTER));
        root.addView(overlay,new FrameLayout.LayoutParams(-1,-1));updateUndoHint();
        return panel;
    }
    private String providerInstalledVersion(ProviderCatalog.Entry entry) {
        try { android.content.pm.PackageInfo info=getPackageManager().getPackageInfo(entry.packageName,0); return info.versionName==null?String.valueOf(info.versionCode):info.versionName; }
        catch(PackageManager.NameNotFoundException missing) { return null; }
    }
    private String providerRoute(ProviderCatalog.Entry entry) {
        return getString(entry.excluded?R.string.hb_provider_excluded:entry.play?R.string.hb_provider_play:R.string.hb_provider_community);
    }
    private void providerDirectory() {
        openMenu(new MenuSpec(getString(R.string.hb_get_providers),this::drawProviderDirectory));
    }
    private void drawProviderDirectory() {
        LinearLayout panel=providerPanel(getString(R.string.hb_get_providers));
        panel.addView(text(getString(R.string.hb_provider_directory_hint),14));
        LinearLayout cards=new LinearLayout(this);cards.setPadding(0,dp(12),0,dp(12));
        panel.addView(cards,new LinearLayout.LayoutParams(-1,dp(230)));
        int first=providerDirectoryPage*3;
        View focus=null;
        for(int i=first;i<Math.min(first+3,ProviderCatalog.ENTRIES.size());i++) {
            ProviderCatalog.Entry entry=ProviderCatalog.ENTRIES.get(i);
            int index=i;
            LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(14),dp(12),dp(14),dp(10));card.setBackground(tileBackground());card.setFocusable(true);card.setClickable(true);
            card.addView(text(entry.name,20));card.addView(text(providerRoute(entry),13));
            TextView description=text(getString(entry.detail),14);card.addView(description,new LinearLayout.LayoutParams(-1,0,1));
            String version=providerInstalledVersion(entry);
            card.addView(text(version==null?getString(R.string.hb_provider_not_installed):getString(R.string.hb_provider_installed,version),13));
            card.setOnFocusChangeListener((v,hasFocus)->{if(hasFocus)providerDirectoryFocus=index;});
            card.setOnClickListener(v->openMenu(new MenuSpec(entry.name,()->drawProviderEntry(entry))));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);lp.setMargins(dp(4),0,dp(4),0);cards.addView(card,lp);
            if(i==Math.max(first,providerDirectoryFocus))focus=card;
        }
        panel.addView(text(getString(R.string.hb_provider_scope),13));
        LinearLayout nav=new LinearLayout(this);panel.addView(nav);
        if(first>0)nav.addView(infoButton("‹ "+getString(R.string.hb_previous),()->{providerDirectoryPage--;providerDirectoryFocus=providerDirectoryPage*3;drawProviderDirectory();}),new LinearLayout.LayoutParams(0,dp(48),1));
        nav.addView(infoButton(getString(R.string.hb_back),this::backMenu),new LinearLayout.LayoutParams(0,dp(48),1));
        if(first+3<ProviderCatalog.ENTRIES.size())nav.addView(infoButton(getString(R.string.hb_next)+" ›",()->{providerDirectoryPage++;providerDirectoryFocus=providerDirectoryPage*3;drawProviderDirectory();}),new LinearLayout.LayoutParams(0,dp(48),1));
        if(focus!=null)focus.requestFocus();
    }
    private void drawProviderEntry(ProviderCatalog.Entry entry) {
        LinearLayout panel=providerPanel(entry.name);
        panel.addView(text(providerRoute(entry)+" · "+getString(entry.detail),15));
        String version=providerInstalledVersion(entry);
        panel.addView(text(version==null?getString(R.string.hb_provider_not_installed):getString(R.string.hb_provider_installed,version),14));
        if(!entry.excluded && version==null) {
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
            ImageView qr=new ImageView(this);qr.setScaleType(ImageView.ScaleType.FIT_CENTER);
            try { qr.setImageBitmap(PhoneEditor.qr(entry.url)); } catch(Exception unavailable) { /* Address remains available. */ }
            qr.setContentDescription(entry.url);row.addView(qr,new LinearLayout.LayoutParams(dp(180),dp(180)));
            TextView address=text(getString(R.string.hb_provider_phone_hint)+"\n\n"+entry.url,14);address.setPadding(dp(16),0,0,0);
            row.addView(address,new LinearLayout.LayoutParams(0,-2,1));panel.addView(row);
        }
        LinearLayout actions=new LinearLayout(this);panel.addView(actions);
        if(!entry.excluded) {
            if(version!=null) {
                for(ProjectivyProviderRegistry.Provider p:new ProjectivyProviderRegistry(this).discover()) {
                    if(!entry.packageName.equals(p.component.getPackageName()))continue;
                    Intent settings=new ProjectivyProviderRegistry(this).settings(p);
                    if(settings==null)settings=getPackageManager().getLeanbackLaunchIntentForPackage(entry.packageName);
                    if(settings!=null) {
                        Intent launch=settings;
                        actions.addView(infoButton(getString(R.string.hb_provider_configure),()->{
                            returningToProvider=entry;
                            try{startActivity(launch);}catch(RuntimeException unavailable){returningToProvider=null;toast(getString(R.string.hb_no_accessible_plugin_settings));}
                        }),new LinearLayout.LayoutParams(0,dp(52),1));
                    }
                    actions.addView(infoButton(getString(R.string.hb_provider_preview),()->selectProvider(p)),new LinearLayout.LayoutParams(0,dp(52),1));
                    break;
                }
            } else if(entry.play) {
                actions.addView(infoButton(getString(R.string.hb_provider_open_play),()->{
                    returningToProvider=entry;
                    Intent intent=new Intent(Intent.ACTION_VIEW,Uri.parse(entry.url)).setPackage("com.android.vending");
                    try{startActivity(intent);}catch(RuntimeException unavailable){returningToProvider=null;toast(getString(R.string.hb_provider_no_store));}
                }),new LinearLayout.LayoutParams(0,dp(52),1));
            }
        }
        actions.addView(infoButton(getString(R.string.hb_provider_refresh),()->drawProviderEntry(entry)),new LinearLayout.LayoutParams(0,dp(52),1));
        TextView back=infoButton(getString(R.string.hb_back),this::backMenu);panel.addView(back,new LinearLayout.LayoutParams(-1,dp(48)));
        if(actions.getChildCount()>0)actions.getChildAt(0).requestFocus();else back.requestFocus();
    }
    private void selectProvider(ProjectivyProviderRegistry.Provider p) {
        if(!p.usable()){toast(p.problem);return;}
        wallpaperProvider=p;wallpaperDraft=layout.wallpaper.copy();
        wallpaperDraft.component=p.component.flattenToString();wallpaperDraft.uuid=p.uuid;
        wallpaperReady=false;wallpaperPreviewStarted=false;wallpaperStatus=getString(R.string.hb_choose_preview_to_load_this_provider);
        providerDetails(p,false);
    }
    private JSONArray phoneProviders() throws Exception {
        JSONArray result=new JSONArray();
        for(ProviderCatalog.Entry entry:ProviderCatalog.ENTRIES) {
            String version=providerInstalledVersion(entry);
            result.put(new JSONObject().put("id",entry.id).put("name",entry.name).put("route",providerRoute(entry))
                    .put("detail",getString(entry.detail)).put("url",entry.url).put("play",entry.play).put("excluded",entry.excluded)
                    .put("installed",version!=null).put("version",version==null?"":version));
        }
        return result;
    }

    private void backgroundSettings() {
        openMenu(backgroundMenuSpec());
    }
    private MenuSpec backgroundMenuSpec() {
        String[] choices = layout.wallpaper.solid()
                ? new String[]{getString(R.string.hb_status_solid_color), getString(R.string.hb_projectivy_plugins), getString(R.string.hb_background_details), getString(R.string.hb_get_providers)}
                : new String[]{getString(R.string.hb_status_solid_color), getString(R.string.hb_projectivy_plugins), getString(R.string.hb_background_details), getString(R.string.hb_get_providers), getString(R.string.hb_retry_provider)};
        return new MenuSpec(getString(R.string.hb_background), layout.wallpaper.solid() ? getString(R.string.hb_status_solid_color) : getString(R.string.hb_projectivy_wallpaper_provider_selected),
                choices, choice -> {
                    if (choice == 0) solidColorSettings();
                    else if (choice == 1) providerList();
                    else if (choice == 2) backgroundDetails();
                    else if (choice == 3) providerDirectory();
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
        menu(getString(R.string.hb_projectivy_plugins), providers.isEmpty() ? getString(R.string.hb_no_providers_installed_install_a_wallpaper_provider_app_then_retu)
                : getString(R.string.hb_choose_an_installed_provider_to_preview), labels, choice -> {
                    ProjectivyProviderRegistry.Provider p = providers.get(choice);
                    selectProvider(p);
                });
    }
    private void providerDetails(ProjectivyProviderRegistry.Provider p, boolean replace) {
        MenuSpec spec = new MenuSpec(getString(R.string.hb_provider_background), wallpaperStatus + " · " + p.name + " · " + p.readiness(),
                new String[]{getString(R.string.hb_preview), getString(R.string.hb_plugin_settings), wallpaperReady ? getString(R.string.hb_use_background) : getString(R.string.hb_use_background_preview_first), getString(R.string.hb_more_options)}, choice -> {
                    if (choice == 0) {
                        if (wallpaperDraft == null) { wallpaperDraft = layout.wallpaper.copy(); wallpaperDraft.component = p.component.flattenToString(); wallpaperDraft.uuid = p.uuid; }
                        wallpaperPreviewStarted = true; wallpaperReady = false; wallpaperStatus = getString(R.string.hb_loading_wallpaper);
                        syncBackground(); wallpaperHost.refresh(); providerDetails(p, true);
                    } else if (choice == 1) {
                        Intent settings = new ProjectivyProviderRegistry(this).settings(p);
                        if (settings == null) { toast(getString(R.string.hb_no_accessible_plugin_settings)); providerDetails(p, true); }
                        else {
                            returningFromProviderSettings = true;
                            try { startActivity(settings); }
                            catch (RuntimeException invalid) { returningFromProviderSettings = false; toast(getString(R.string.hb_plugin_settings_are_unavailable)); providerDetails(p, true); }
                        }
                    } else if (choice == 2) {
                        if (!wallpaperReady || wallpaperDraft == null) { toast(getString(R.string.hb_preview_a_usable_wallpaper_first)); providerDetails(p, true); return; }
                        layout.wallpaper = wallpaperDraft.copy(); wallpaperDraft = null;
                        save(getString(R.string.hb_background_189) + p.name); syncBackground(); render(0);
                    } else providerOptions(p);
                }, -1);
        if (replace) {
            // Returning from provider options replaces that subtree. Back must reach the
            // provider list, rather than a stale options callback after discarding the draft.
            for (int i = menuStack.size() - 1; i >= 0; i--) {
                if (getString(R.string.hb_provider_background).equals(menuStack.get(i).title)) {
                    while (menuStack.size() > i) menuStack.remove(menuStack.size() - 1);
                    break;
                }
            }
            activeMenu = spec; menuGeneration++; showMenuPage(spec, 0, 0);
        } else openMenu(spec);
    }
    private void refreshProviderDetails() {
        if (backgroundInfo && backgroundStatusText != null) backgroundStatusText.setText(backgroundInfoText());
        if (!closed && activeMenu != null && getString(R.string.hb_provider_background).equals(activeMenu.title)
                && overlay != null && wallpaperProvider != null) providerDetails(wallpaperProvider, true);
    }
    private void providerOptions(ProjectivyProviderRegistry.Provider p) {
        if (wallpaperDraft == null) wallpaperDraft = layout.wallpaper.copy();
        menu(getString(R.string.hb_wallpaper_options), getString(R.string.hb_host_display_options_provider_content_stays_in_plugin_settings),
                new String[]{getString(R.string.hb_rotation) + wallpaperDraft.rotationMinutes + getString(R.string.hb_min), getString(R.string.hb_dimming) + wallpaperDraft.dimPercent + "%", getString(R.string.hb_background_details), getString(R.string.hb_back_to_provider)}, choice -> {
                    if (choice == 0) menu(getString(R.string.hb_wallpaper_rotation), getString(R.string.hb_batch_cache_expiry_is_controlled_by_the_provider),
                            new String[]{getString(R.string.hb_1_minute), getString(R.string.hb_5_minutes), getString(R.string.hb_15_minutes), getString(R.string.hb_60_minutes)}, n -> {
                                wallpaperDraft.rotationMinutes = new int[]{1, 5, 15, 60}[n]; syncBackground(); providerDetails(p, true);
                            });
                    else if (choice == 1) startAdjustment(getString(R.string.hb_wallpaper_dimming), wallpaperDraft.dimPercent,
                            value -> wallpaperSurface.options(value, reducedMotion()), value -> {
                                wallpaperDraft.dimPercent = value; syncBackground(); providerDetails(p, true);
                            });
                    else if (choice == 2) backgroundDetails();
                    else providerDetails(p, true);
                });
    }
    private void backgroundDetails() {
        openMenu(new MenuSpec(getString(R.string.hb_background_details), this::drawBackgroundDetails));
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
        info += "\n"+getString(R.string.hb_ui_opacity_70)+": " + layout.uiOpacityPercent + "%";
        tv.projectivy.plugin.wallpaperprovider.api.Wallpaper item = displayedWallpaper;
        if (item != null && !selected.solid()) info += getString(R.string.hb_hint_ntitle) + (item.getTitle() == null ? getString(R.string.hb_untitled) : item.getTitle())
                + getString(R.string.hb_hint_nauthor) + (item.getAuthor() == null ? getString(R.string.hb_not_provided) : item.getAuthor())
                + getString(R.string.hb_hint_nsource) + (item.getSource() == null ? getString(R.string.hb_not_provided) : item.getSource());
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
        panel.addView(text(getString(R.string.hb_background_details), 24), new LinearLayout.LayoutParams(-1, -2));
        TextView details = text(backgroundInfoText(), 18); details.setMaxLines(8); details.setEllipsize(android.text.TextUtils.TruncateAt.END);
        backgroundStatusText = details;
        LinearLayout.LayoutParams detailLp = new LinearLayout.LayoutParams(-1, -2); detailLp.topMargin = dp(16); panel.addView(details, detailLp);
        LinearLayout buttons = new LinearLayout(this); buttons.setGravity(Gravity.CENTER_VERTICAL);
        if (item != null && item.getActionUri() != null) {
            TextView link = infoButton(getString(R.string.hb_open_wallpaper_link), () -> openWallpaperLink(item.getActionUri()));
            buttons.addView(link, new LinearLayout.LayoutParams(0, dp(60), 1));
        }
        buttons.addView(infoButton("Choose color", this::solidColorSettings), new LinearLayout.LayoutParams(0, dp(60), 1));
        buttons.addView(infoButton("Choose provider", this::providerList), new LinearLayout.LayoutParams(0, dp(60), 1));
        aboutBackButton = infoButton(getString(R.string.hb_back), this::backMenu);
        buttons.addView(aboutBackButton, new LinearLayout.LayoutParams(0, dp(60), 1));
        LinearLayout.LayoutParams buttonsLp = new LinearLayout.LayoutParams(-1, dp(64)); buttonsLp.topMargin = dp(16); panel.addView(buttons, buttonsLp);
        int width = Math.min(dp(840), dashboardWidthPixels() - dp(64));
        overlay.addView(panel, new FrameLayout.LayoutParams(width, -2, Gravity.CENTER));
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1));updateUndoHint(); aboutBackButton.requestFocus();
    }
    private TextView infoButton(String label, Runnable action) {
        TextView button = text(label, 17); button.setGravity(Gravity.CENTER); button.setFocusable(true); button.setClickable(true);
        button.setBackground(tileBackground()); button.setOnClickListener(v -> action.run()); return button;
    }
    private void openWallpaperLink(String value) {
        Uri uri = Uri.parse(value); String scheme = uri.getScheme();
        if (scheme == null || java.util.Arrays.asList("file", "content", "android.resource", "data", "javascript", "intent").contains(scheme.toLowerCase(java.util.Locale.ROOT))) {
            toast(getString(R.string.hb_this_wallpaper_link_type_is_unsupported)); return;
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
            adjustmentValue = getString(R.string.hb_ui_opacity_70).equals(title) ? WallpaperSettings.opacityStep(adjustmentValue, arm)
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
                (getString(R.string.hb_ui_opacity_70).equals(adjustmentTitle) ? getString(R.string.hb_backgrounds) : "") + adjustmentValue + "%", getString(R.string.hb_done), getString(R.string.hb_back_cancels),
                new String[]{"+5%", "100%", "−5%", getString(R.string.hb_reset) + (getString(R.string.hb_ui_opacity_70).equals(adjustmentTitle) ? WallpaperSettings.DEFAULT_WIDGET_OPACITY + "%" : "35%")},
                new MenuIconView.Kind[]{MenuIconView.Kind.ADD, MenuIconView.Kind.PALETTE, MenuIconView.Kind.DELETE, MenuIconView.Kind.UNDO}, new boolean[4],
                new PieMenuView.Listener() {
                    @Override public void onArm(int arm) { if (adjustmentArm != null) adjustmentArm.accept(arm); }
                    @Override public void onCenter() { if (adjustmentDone != null) adjustmentDone.run(); }
                });
        int diameter = Math.min(dp(760), Math.min(dashboardWidthPixels() - dp(56), dashboardHeightPixels() - dp(40)));
        overlay.addView(pie, new FrameLayout.LayoutParams(diameter, diameter, Gravity.CENTER));
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1));updateUndoHint(); pie.requestFocus();
    }
    private void clearAdjustment() { adjustmentArm = null; adjustmentDone = null; adjustmentCancel = null; adjustmentPreview = null; adjustmentCommit = null; }
    private void backAdjustment() {
        String title = adjustmentTitle;
        cancelAdjustment();
        if (getString(R.string.hb_ui_opacity_70).equals(title)) appearanceSettings();
        else if (wallpaperProvider != null) providerDetails(wallpaperProvider, true);
    }
    private void cancelAdjustment() {
        Runnable cancel = adjustmentCancel; clearAdjustment(); dismissMenu(); if (cancel != null) cancel.run();
    }
}
