package com.gathin.tvhome;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Screensavers Stage 0 probe screen (docs/plans/2026-11-20-screensavers-stage0-probe.md), debug builds only.
 * Answers what Homeboard itself may do, which the coordinator's ADB-shell workflows cannot: Q2, Q3, Q4, Q7, Q10,
 * Q11 read-only, and Q5 only when the extra {@link #EXTRA_WRITE_TEST} is true (default false). Q8 is not built.
 *
 * Output follows the existing probes: JSON in the app's external files dir as wf.log, which coordinator check jobs
 * export, plus one logcat line per write under tag HB_SCREENSAVER_PROBE. The shape is ScreensaverProbeReport's.
 *
 * Every run does the reads and resolves every intent. It then starts at most one intent, named by the extra or
 * the alias meta-data {@link #EXTRA_START} (dream_settings, screensaver_now, market; default none), so each
 * coordinator capture shows one screen. Lifecycle callbacks are recorded for the whole run (Q7) and the report is
 * rewritten after each, so a screensaver started over this screen by screensaver-trigger, and the wake, are seen.
 */
public final class ScreensaverProbeActivity extends Activity {
    static final String TAG = "HB_SCREENSAVER_PROBE";
    /** Boolean, default false: run the Q5 write test. Only an explicit true runs it. */
    static final String EXTRA_WRITE_TEST = "write_test";
    /** String: which intent to start after the reads; none by default. */
    static final String EXTRA_START = "start";
    /** String: the Play listing package for Q10. */
    static final String EXTRA_PLAY_PACKAGE = "play_package";
    static final String START_DREAM_SETTINGS = "dream_settings", START_SCREENSAVER_NOW = "screensaver_now",
            START_MARKET = "market";
    /** A listing present on every Google TV device; any listed package answers Q10. */
    static final String DEFAULT_PLAY_PACKAGE = "com.google.android.youtube.tv";
    private static final String DREAM_SETTINGS = "android.settings.DREAM_SETTINGS";
    private static final String[] SECURE_KEYS = {"screensaver_components", "screensaver_enabled",
            "screensaver_activate_on_sleep", "screensaver_activate_on_dock", "screensaver_default_component",
            "sleep_timeout"};
    private static final String[] SYSTEM_KEYS = {Settings.System.SCREEN_OFF_TIMEOUT};

    private final ScreensaverProbeReport report = new ScreensaverProbeReport();
    private final JSONArray lifecycle = new JSONArray();
    private final long startedAt = System.currentTimeMillis();
    private TextView text;
    private boolean ran;
    private String start = "";
    private ScreensaverProbeReport.Result q2, q3, q10;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        text = new TextView(this);
        text.setTextColor(-1); text.setTextSize(20); text.setPadding(40, 40, 40, 40);
        text.setBackgroundColor(0xff101a23);
        text.setText("Screensaver probe (Stage 0)…");
        setContentView(text);
        event("onCreate");
    }

    @Override protected void onStart() { super.onStart(); event("onStart"); }
    @Override protected void onRestart() { super.onRestart(); event("onRestart"); }
    @Override protected void onPause() { event("onPause"); super.onPause(); }
    @Override protected void onStop() { event("onStop"); super.onStop(); }
    @Override protected void onDestroy() { event("onDestroy"); super.onDestroy(); }
    @Override public void onWindowFocusChanged(boolean focus) {
        super.onWindowFocusChanged(focus); event(focus ? "focusGained" : "focusLost");
    }

    @Override protected void onResume() {
        super.onResume();
        event("onResume");
        if (ran) return;   // a return from a started screen or the screensaver must not run the checks again
        ran = true;
        start = option(EXTRA_START);
        try { runChecks(); }
        catch (RuntimeException e) { report.add(ScreensaverProbeReport.Result.fail("probe", null, "checks complete", e.toString())); }
        write(true);
        startRequested();
    }

    private void runChecks() {
        identify();
        if (getIntent().getBooleanExtra(EXTRA_WRITE_TEST, false)) {
            report.addAll(ScreensaverWriteTest.run(new AndroidSettings(), writeCandidates()));
        } else {
            report.add(ScreensaverProbeReport.Result.notRun("q5", "explicit write_test extra",
                    "write_test extra not set (default off)"));
        }
        report.add(q2 = resolveCheck("q2", new Intent(DREAM_SETTINGS), START_DREAM_SETTINGS, null));
        report.add(q3 = resolveCheck("q3", SystemScreensaver.startNowIntent(), START_SCREENSAVER_NOW, null));
        report.add(q4());
        report.add(q7());
        report.add(ScreensaverProbeReport.Result.notRun("q8", "a video provider plays in a throwaway dream service",
                "not implemented in this probe (throwaway dream service not built)"));
        report.add(q10 = resolveCheck("q10", marketIntent(), START_MARKET, "com.android.vending"));
        report.addAll(q11());
    }

    private void identify() {
        report.device("manufacturer", Build.MANUFACTURER).device("model", Build.MODEL).device("device", Build.DEVICE)
                .device("sdk", Build.VERSION.SDK_INT).device("release", Build.VERSION.RELEASE)
                .device("fingerprint", Build.FINGERPRINT);
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            report.build("package", getPackageName()).build("versionName", info.versionName)
                    .build("versionCode", Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode)
                    .build("writeTest", getIntent().getBooleanExtra(EXTRA_WRITE_TEST, false))
                    .build("start", start.isEmpty() ? JSONObject.NULL : start);
        } catch (PackageManager.NameNotFoundException impossible) { report.build("package", getPackageName()); }
    }

    /** Resolve, and start it too when this run's start option names it. pass: resolves (or started) as expected. */
    private ScreensaverProbeReport.Result resolveCheck(String id, Intent intent, String startName, String expectedPackage) {
        boolean starts = startName != null && startName.equals(start);
        String expected = (expectedPackage == null ? "intent resolves to an activity" : "resolves to " + expectedPackage)
                + (starts ? " and starts" : "");
        String target = null, error = null;
        try {
            ResolveInfo r = getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            if (r != null && r.activityInfo != null) target = r.activityInfo.packageName + "/" + r.activityInfo.name;
        } catch (RuntimeException e) { error = e.toString(); }
        boolean resolves = target != null && (expectedPackage == null || target.startsWith(expectedPackage + "/"));
        JSONObject observed = ScreensaverWriteTest.obj("intent", intent.toUri(0), "resolvesTo", target,
                "started", starts ? "pending" : "not requested");
        if (error == null && target == null) error = "resolveActivity returned null (absent, or hidden by package visibility)";
        else if (error == null && !resolves) error = "resolves to " + target + ", not " + expectedPackage;
        return ScreensaverProbeReport.Result.of(id, resolves, observed, expected, error);
    }

    private void startRequested() {
        ScreensaverProbeReport.Result check = START_DREAM_SETTINGS.equals(start) ? q2
                : START_SCREENSAVER_NOW.equals(start) ? q3 : START_MARKET.equals(start) ? q10 : null;
        if (check == null) return;
        Intent intent = check == q2 ? new Intent(DREAM_SETTINGS) : check == q3 ? SystemScreensaver.startNowIntent() : marketIntent();
        String outcome;
        try { startActivity(intent); outcome = "started"; }
        catch (RuntimeException e) { outcome = "start failed: " + e; }
        Log.i(TAG, "start " + start + " " + outcome);
        try { ((JSONObject) check.observed).put("started", outcome); } catch (Exception ignored) { }
        // A started check passes when Android accepted the start, even if package visibility hid the resolve.
        boolean ok = "started".equals(outcome);
        String error = ok ? (check.pass ? null : check.error + "; started anyway")
                : (check.error == null ? "" : check.error + "; ") + outcome;
        report.put(ScreensaverProbeReport.Result.of(check.id, ok, check.observed, check.expected, error));
        write(true);
    }

    private ScreensaverProbeReport.Result q4() {
        JSONObject secure = new JSONObject(), system = new JSONObject();
        List<String> failed = new ArrayList<>();
        AndroidSettings s = new AndroidSettings();
        for (String key : SECURE_KEYS) put(secure, key, s.readSecure(key), failed);
        for (String key : SYSTEM_KEYS) put(system, key, s.readSystem(key), failed);
        return ScreensaverProbeReport.Result.of("q4", failed.isEmpty(),
                ScreensaverWriteTest.obj("secure", secure, "system", system,
                        "writeSecureSettingsGranted", s.mayWriteSecure()),
                "every key readable with no extra permission (null means not set)",
                failed.isEmpty() ? null : "unreadable: " + String.join(", ", failed));
    }

    private static void put(JSONObject into, String key, ScreensaverWriteTest.Value v, List<String> failed) {
        try { into.put(key, v.readable ? (v.value == null ? JSONObject.NULL : v.value) : "unreadable: " + v.error); }
        catch (Exception ignored) { }
        if (!v.readable) failed.add(key);
    }

    /** Lifecycle as seen so far; pass once the screen was covered (pause or focus lost) and came back (resume). */
    private ScreensaverProbeReport.Result q7() {
        boolean covered = false, back = false;
        for (int i = 0; i < lifecycle.length(); i++) {
            String name = lifecycle.optJSONObject(i).optString("event");
            if (name.equals("onPause") || name.equals("focusLost")) covered = true;
            else if (covered && (name.equals("onResume") || name.equals("focusGained"))) back = true;
        }
        return ScreensaverProbeReport.Result.of("q7", covered && back,
                ScreensaverWriteTest.obj("events", lifecycle, "foregroundAfterWake", back ? getComponentName().flattenToString() : null),
                "covered (onPause/onStop or focus lost) when Android's screensaver starts, then onResume/focus on wake",
                covered && back ? null : "screensaver start and wake not yet seen in this run");
    }

    private List<ScreensaverProbeReport.Result> q11() {
        List<ScreensaverProbeReport.Result> out = new ArrayList<>();
        boolean declared = false;
        try {
            String[] requested = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS).requestedPermissions;
            declared = requested != null && Arrays.asList(requested).contains(Manifest.permission.REQUEST_INSTALL_PACKAGES);
        } catch (Exception ignored) { }
        try {
            boolean can = getPackageManager().canRequestPackageInstalls();
            out.add(ScreensaverProbeReport.Result.pass("q11-can-request",
                    ScreensaverWriteTest.obj("canRequestPackageInstalls", can, "permissionDeclared", declared),
                    "canRequestPackageInstalls reads without error"));
        } catch (RuntimeException e) {
            out.add(ScreensaverProbeReport.Result.fail("q11-can-request",
                    ScreensaverWriteTest.obj("permissionDeclared", declared), "canRequestPackageInstalls reads without error", e.toString()));
        }
        Intent manage = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName()));
        ScreensaverProbeReport.Result resolved = resolveCheck("q11-manage-sources", manage, null, null);
        out.add(resolved);
        out.add(ScreensaverProbeReport.Result.notRun("q11-offer",
                "a frozen harmless test APK is offered to the installer and cancelled",
                "no frozen test APK exists in the repo"));
        return out;
    }

    private Intent marketIntent() {
        String pkg = option(EXTRA_PLAY_PACKAGE);
        return new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + (pkg.isEmpty() ? DEFAULT_PLAY_PACKAGE : pkg)));
    }

    /** Candidates for Q5's test component: this install's dream, then other installed dreams. */
    private List<String> writeCandidates() {
        List<String> out = new ArrayList<>();
        SystemScreensaver gateway = new SystemScreensaver(this);
        out.add(gateway.ownComponent());
        for (ScreensaverGateway.InstalledDream d : gateway.installedDreams()) out.add(d.component);
        return out;
    }

    /** An intent extra wins; otherwise the activity-alias meta-data (lets app-register select a variant). */
    private String option(String name) {
        String v = getIntent().getStringExtra(name);
        if (v != null) return v;
        try {
            ActivityInfo info = getPackageManager().getActivityInfo(getComponentName(), PackageManager.GET_META_DATA);
            if (info.metaData != null && info.metaData.getString(name) != null) return info.metaData.getString(name);
        } catch (PackageManager.NameNotFoundException ignored) { }
        return "";
    }

    private void event(String name) {
        try { lifecycle.put(new JSONObject().put("event", name).put("ms", System.currentTimeMillis() - startedAt)); }
        catch (Exception ignored) { }
        Log.i(TAG, "lifecycle " + name);
        if (ran) {   // keep Q7 current after the first report
            report.put(q7());
            write(true);
        }
    }

    private void write(boolean finished) {
        try {
            JSONObject json = report.toJson(Instant.now().toString(), finished);
            Files.write(new File(getExternalFilesDir(null), "wf.log").toPath(), json.toString().getBytes(StandardCharsets.UTF_8));
            Log.i(TAG, "result=" + json);
            if (text != null) text.setText(summary(json));
        } catch (Exception e) { Log.w(TAG, "report not written", e); }
    }

    private static String summary(JSONObject json) {
        StringBuilder b = new StringBuilder("Screensaver probe · ").append(json.optString("status")).append('\n');
        JSONArray results = json.optJSONArray("results");
        for (int i = 0; results != null && i < results.length(); i++) {
            JSONObject r = results.optJSONObject(i);
            b.append(r.optBoolean("pass") ? "✓ " : "✗ ").append(r.optString("id"));
            if (r.has("error")) b.append(" · ").append(r.optString("error"));
            b.append('\n');
        }
        return b.toString();
    }

    /** The real settings calls, every exception caught and reported as text. */
    private final class AndroidSettings implements ScreensaverWriteTest.Settings {
        @Override public boolean mayWriteSecure() {
            return checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED;
        }
        @Override public ScreensaverWriteTest.Value readSecure(String key) {
            try { return ScreensaverWriteTest.Value.of(Settings.Secure.getString(getContentResolver(), key)); }
            catch (RuntimeException e) { return ScreensaverWriteTest.Value.unreadable(e.toString()); }
        }
        @Override public ScreensaverWriteTest.Value readSystem(String key) {
            try { return ScreensaverWriteTest.Value.of(Settings.System.getString(getContentResolver(), key)); }
            catch (RuntimeException e) { return ScreensaverWriteTest.Value.unreadable(e.toString()); }
        }
        @Override public String writeSecure(String key, String value) {
            try { return Settings.Secure.putString(getContentResolver(), key, value) ? null : "putString returned false"; }
            catch (RuntimeException e) { return e.toString(); }
        }
        @Override public String writeSystem(String key, String value) {
            try { return Settings.System.putString(getContentResolver(), key, value) ? null : "putString returned false"; }
            catch (RuntimeException e) { return e.toString(); }
        }
    }
}
