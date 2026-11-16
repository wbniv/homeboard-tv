package com.gathin.tvhome;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import org.json.JSONArray;

/** Real remote/menu regressions with isolated launcher preferences and the offline provider. */
public final class BackgroundMenuProbeActivity extends MainActivity {
    interface Check { void run() throws Exception; }
    static final class Case {
        final String name; final Check action, assertion; final BooleanSupplier ready;
        Case(String name, Check action, Check assertion, BooleanSupplier ready) {
            this.name = name; this.action = action; this.assertion = assertion; this.ready = ready;
        }
    }
    private final Handler runner = new Handler();
    private final List<Case> cases = new ArrayList<>();
    private final Set<String> temporaryPreferences = new HashSet<>();
    private final List<String> passed = new ArrayList<>();
    private final List<Bitmap> shots = new ArrayList<>();
    private Map<String, ?> original;
    private ProjectivyProviderRegistry.Provider fixture;
    private int index, undoBeforeRetry, undoBeforeSolid;
    private long deadline;
    private boolean finished;

    @Override public SharedPreferences getSharedPreferences(String name, int mode) {
        if ("widget-home".equals(name) || name.startsWith("air-quality-widget-")) {
            name = "background-menu-probe-" + name; temporaryPreferences.add(name);
        }
        return super.getSharedPreferences(name, mode);
    }
    @Override protected void onCreate(Bundle state) {
        try {
            original = new LinkedHashMap<>(super.getSharedPreferences("widget-home", MODE_PRIVATE).getAll());
            String saved = (String) original.get("layout");
            HomeLayout seed = saved == null ? HomeLayout.firstRun() : HomeLayout.parse(saved);
            seed.wallpaper = new WallpaperSettings(); seed.palette = "forest";
            getSharedPreferences("widget-home", MODE_PRIVATE).edit().clear()
                    .putString("layout", seed.encode()).putString("undo_history", "[]").apply();
        } catch (Exception invalid) { throw new IllegalStateException(invalid); }
        super.onCreate(state);
        runner.postDelayed(() -> {
            try {
                fixture = new ProjectivyProviderRegistry(this).discover().stream()
                        .filter(p -> p.component.getPackageName().equals(getPackageName())).findFirst()
                        .orElseThrow(() -> new IllegalStateException("Offline provider missing"));
                for (boolean apps : new boolean[]{false, true}) addSurfaceCases(apps);
                addProviderCases(); next();
            } catch (Exception error) { fail(error); }
        }, 12_000);
    }
    private Object get(Object target, String name) throws Exception {
        Class<?> type = target == this ? MainActivity.class : target.getClass();
        Field field = type.getDeclaredField(name); field.setAccessible(true); return field.get(target);
    }
    private Object get(String name) throws Exception { return get(this, name); }
    private void call(String name) throws Exception {
        Method method = MainActivity.class.getDeclaredMethod(name); method.setAccessible(true); method.invoke(this);
    }
    private HomeLayout layout() throws Exception { return (HomeLayout) get("layout"); }
    private Object menu() throws Exception { return get("activeMenu"); }
    private void expectMenu(String title) throws Exception {
        require(menu() != null && title.equals(get(menu(), "title")), "Expected " + title + ", got " + (menu() == null ? "no menu" : get(menu(), "title")));
    }
    private void key(int key) {
        boolean handled = dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, key));
        // ViewRoot normally performs focus navigation after an unhandled Activity key.
        // Dispatching in-process needs that framework focus-search step too.
        if (!handled && key >= KeyEvent.KEYCODE_DPAD_UP && key <= KeyEvent.KEYCODE_DPAD_RIGHT) {
            int direction = key == KeyEvent.KEYCODE_DPAD_UP ? View.FOCUS_UP
                    : key == KeyEvent.KEYCODE_DPAD_RIGHT ? View.FOCUS_RIGHT
                    : key == KeyEvent.KEYCODE_DPAD_DOWN ? View.FOCUS_DOWN : View.FOCUS_LEFT;
            View focused = getCurrentFocus();
            View next = focused == null ? null : focused.focusSearch(direction);
            if (next != null) next.requestFocus(direction);
        }
        dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, key));
    }
    private void choose(int choice) throws Exception {
        int size = (Integer) get(menu(), "pageSize");
        while ((Integer) get(menu(), "page") < choice / size) key(KeyEvent.KEYCODE_DPAD_CENTER);
        int[] arms = {KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT};
        key(arms[choice % size]);
    }
    private void closeMenus() throws Exception {
        for (int i = 0; get("overlay") != null && i < 15; i++) key(KeyEvent.KEYCODE_BACK);
        require(get("overlay") == null, "Back did not close menus");
    }
    private void openBackground() throws Exception {
        closeMenus(); key(KeyEvent.KEYCODE_MENU); choose(1); choose(0); expectMenu("Background");
    }
    private void undo() throws Exception {
        closeMenus(); key(KeyEvent.KEYCODE_MENU); choose(3); expectMenu("Undo history"); choose(0);
    }
    private int undoCount() throws Exception {
        return new JSONArray(getSharedPreferences("widget-home", MODE_PRIVATE).getString("undo_history", "[]")).length();
    }
    private String details() throws Exception {
        expectMenu("Background details"); require((Boolean) get("backgroundInfo"), "Native details input mode missing");
        return ((TextView) get("backgroundStatusText")).getText().toString();
    }
    private TextView button(View view, String text) {
        if (view instanceof TextView && text.contentEquals(((TextView) view).getText()) && view.isClickable()) return (TextView) view;
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            TextView found = button(((ViewGroup) view).getChildAt(i), text); if (found != null) return found;
        }
        return null;
    }
    private void click(String label) throws Exception {
        TextView target = button((View) get("overlay"), label); require(target != null, "Missing button " + label);
        require(target.requestFocus(), "Could not focus " + label); key(KeyEvent.KEYCODE_DPAD_CENTER);
    }
    private void snapshot() throws Exception {
        View root = (View) get("root"); Bitmap full = Bitmap.createBitmap(root.getWidth(), root.getHeight(), Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(full)); shots.add(Bitmap.createScaledBitmap(full, 900, 506, true)); full.recycle();
    }
    private void add(String name, Check action, Check assertion) { cases.add(new Case(name, action, assertion, () -> true)); }
    private void addReady(String name, Check action, Check assertion) {
        cases.add(new Case(name, action, assertion, () -> {
            try { return (Boolean) get("wallpaperReady") && "Preview ready".equals(get("wallpaperStatus")); }
            catch (Exception e) { return false; }
        }));
    }
    private void addSurfaceCases(boolean apps) {
        String prefix = apps ? "All Apps: " : "Home: ";
        add(prefix + "root source actions", () -> {
            closeMenus(); Field field = MainActivity.class.getDeclaredField("allApps"); field.setAccessible(true); field.setBoolean(this, apps);
            Method render = MainActivity.class.getDeclaredMethod("render", int.class); render.setAccessible(true); render.invoke(this, 0); openBackground();
        }, () -> require(((String[]) get(menu(), "labels")).length == 3, "Solid source offers a no-op Retry"));
        add(prefix + "Solid opens color picker", () -> choose(0), () -> {
            expectMenu("Palette"); require((Boolean) get(menu(), "paletteGrid"), "No palette grid");
            if (!apps) snapshot();
        });
        add(prefix + "color preview saves", () -> key(KeyEvent.KEYCODE_DPAD_RIGHT), () -> {
            require(!"forest".equals(layout().palette) && layout().wallpaper.solid(), "Color selection had no effect");
            require(undoCount() == 0, "Preview split into separate Undo entries");
        });
        add(prefix + "Back returns to Background", () -> key(KeyEvent.KEYCODE_BACK), () -> { expectMenu("Background"); require(undoCount() == 1, "Missing grouped Undo"); });
        add(prefix + "Undo restores color", this::undo, () -> require("forest".equals(layout().palette) && undoCount() == 0, "Undo did not restore palette"));
        add(prefix + "Details shows real settings", () -> { openBackground(); choose(2); }, () -> {
            String info = details(); require(info.contains("Source: Solid color") && info.contains("Palette: forest") && info.contains("UI background opacity:"), "Details lacks source or settings");
            if (!apps) snapshot();
        });
        add(prefix + "Details color child returns", () -> {
            click("Choose color"); expectMenu("Palette"); key(apps ? KeyEvent.KEYCODE_BACK : KeyEvent.KEYCODE_DPAD_CENTER);
        }, () -> expectMenu("Background details"));
        add(prefix + "Details providers child returns", () -> {
            key(KeyEvent.KEYCODE_DPAD_LEFT);
            require(getCurrentFocus() instanceof TextView && "Choose provider".contentEquals(((TextView) getCurrentFocus()).getText()), "Details button focus navigation failed");
            key(KeyEvent.KEYCODE_DPAD_CENTER); expectMenu("Projectivy plugins"); key(KeyEvent.KEYCODE_BACK);
        }, () -> expectMenu("Background details"));
        add(prefix + "Details Back button works", () -> click("Back"), () -> expectMenu("Background"));
    }
    private void openFixture() throws Exception {
        openBackground(); choose(1); expectMenu("Projectivy plugins");
        List<ProjectivyProviderRegistry.Provider> providers = new ProjectivyProviderRegistry(this).discover();
        int selected = -1;
        for (int i = 0; i < providers.size(); i++) if (providers.get(i).component.equals(fixture.component)) selected = i;
        require(selected >= 0, "Offline fixture missing"); choose(selected); expectMenu("Provider background");
    }
    private void addProviderCases() {
        add("Provider browsing preserves source", this::openFixture, () -> require(layout().wallpaper.solid(), "Browsing applied a provider"));
        addReady("Preview renders offline provider", () -> choose(0), () -> require(get("displayedWallpaper") != null && layout().wallpaper.solid(), "Preview changed saved source"));
        add("Rotation returns to provider", () -> { choose(3); choose(0); choose(1); }, () -> {
            expectMenu("Provider background"); require(((WallpaperSettings) get("wallpaperDraft")).rotationMinutes == 5, "Rotation choice ignored");
        });
        add("Dimming cancel retains value", () -> { choose(3); choose(1); key(KeyEvent.KEYCODE_DPAD_UP); key(KeyEvent.KEYCODE_BACK); }, () -> {
            expectMenu("Provider background"); require(((WallpaperSettings) get("wallpaperDraft")).dimPercent == 35, "Dimming cancel saved draft");
        });
        add("Dimming Back retains provider parent", () -> key(KeyEvent.KEYCODE_BACK), () -> { expectMenu("Projectivy plugins"); require(get("wallpaperDraft") == null, "Cancelled preview leaked"); });
        addReady("Reopened provider previews again", () -> { openFixture(); choose(0); }, () -> expectMenu("Provider background"));
        add("Use background applies provider", () -> choose(2), () -> {
            require(fixture.component.flattenToString().equals(layout().wallpaper.component), "Use background did not save provider");
            require(get("overlay") == null && get("wallpaperDraft") == null, "Apply did not return to dashboard");
        });
        add("Retry exists for saved provider", () -> {
            openBackground(); require(((String[]) get(menu(), "labels")).length == 4, "Provider has no Retry");
            undoBeforeRetry = undoCount(); choose(3);
        }, () -> expectMenu("Background details"));
        addReady("Retry updates Details when ready", () -> {}, () -> {
            require(details().contains("Preview ready") && details().contains("Fixture color"), "Retry status or credits stayed stale");
            require(undoCount() == undoBeforeRetry, "Retry altered saved settings");
        });
        add("Provider to Solid opens picker", () -> { undoBeforeSolid = undoCount(); click("Choose color"); }, () -> { expectMenu("Palette"); require(layout().wallpaper.solid(), "Solid did not stop provider"); });
        add("Source and color create one Undo", () -> {
            key(KeyEvent.KEYCODE_DPAD_RIGHT); key(KeyEvent.KEYCODE_BACK); expectMenu("Background details"); click("Back");
        }, () -> {
            expectMenu("Background"); require(((String[]) get(menu(), "labels")).length == 3, "Background retained stale Retry");
            require(undoCount() == undoBeforeSolid + 1, "Source and color were split into multiple changes");
        });
        add("Undo restores provider plus color", this::undo, () -> require(fixture.component.flattenToString().equals(layout().wallpaper.component)
                && "forest".equals(layout().palette), "Undo did not restore provider and palette together"));
        addReady("Provider Details has live credits", () -> { openBackground(); choose(2); }, () -> require(details().contains("Homeboard test wallpapers") && details().contains("Title: Fixture color"), "Saved provider details unavailable"));
        add("Real user settings remain unchanged", this::closeMenus, () -> require(original.equals(super.getSharedPreferences("widget-home", MODE_PRIVATE).getAll()), "Tests changed Will's settings"));
    }
    private void next() {
        if (finished) return;
        if (index == cases.size()) { finished = true; report("ALL CHECKS PASSED · " + passed.size() + " menu regressions"); return; }
        Case test = cases.get(index);
        try { test.action.run(); deadline = SystemClock.uptimeMillis() + 15_000; runner.postDelayed(this::check, 300); }
        catch (Exception error) { fail(error); }
    }
    private void check() {
        Case test = cases.get(index);
        try {
            if (!test.ready.getAsBoolean()) {
                require(SystemClock.uptimeMillis() < deadline, "Timed out waiting for UI"); runner.postDelayed(this::check, 200); return;
            }
            test.assertion.run(); passed.add("PASS " + test.name); index++; runner.postDelayed(this::next, 100);
        } catch (Exception error) { fail(error); }
    }
    private void fail(Exception error) { finished = true; report("FAIL " + (index < cases.size() ? cases.get(index).name : "setup") + ": " + error); }
    private void report(String result) {
        LinearLayout row = new LinearLayout(this); row.setBackgroundColor(0xff102333);
        TextView text = new TextView(this); text.setTextColor(Color.WHITE); text.setTextSize(12.5f); text.setPadding(16, 12, 8, 8);
        text.setText(result + "\n\n" + String.join("\n", passed)); row.addView(text, new LinearLayout.LayoutParams(0, -1, 1));
        LinearLayout images = new LinearLayout(this); images.setOrientation(LinearLayout.VERTICAL);
        for (Bitmap bitmap : shots) {
            ImageView image = new ImageView(this); image.setImageBitmap(bitmap); image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            images.addView(image, new LinearLayout.LayoutParams(-1, 0, 1));
        }
        row.addView(images, new LinearLayout.LayoutParams(0, -1, 1));
        addContentView(row, new ViewGroup.LayoutParams(-1, -1));
    }
    private void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    @Override protected void onDestroy() {
        runner.removeCallbacksAndMessages(null); super.onDestroy();
        for (String name : temporaryPreferences) super.getSharedPreferences(name, MODE_PRIVATE).edit().clear().apply();
        for (Bitmap bitmap : shots) bitmap.recycle();
    }
}
