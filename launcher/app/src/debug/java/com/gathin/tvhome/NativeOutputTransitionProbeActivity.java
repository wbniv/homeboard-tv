package com.gathin.tvhome;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Live HDMI output transition acceptance: UHD native, then HD ordinary, then UHD native,
 * driven by this window's own display-mode preference (never a global setting).
 * Android may refuse the preference; that is reported as a refusal, not a pass.
 */
public final class NativeOutputTransitionProbeActivity extends MainActivity {
    private static final String TAG = "HomeboardOutputProbe";
    private static final long SETTLE_MS = 3000, TRANSITION_DEADLINE_MS = 15000;
    private static int stage, recreations, uhdMode, hdMode;
    private static String originalLayout, originalFocus, refusal;
    private static Integer originalPage;
    private static final JSONArray observed = new JSONArray();
    private final Handler probe = new Handler(Looper.getMainLooper());

    @Override protected void onCreate(Bundle state) {
        if (stage > 0) recreations++;
        requestMode(stage == 1 ? hdMode : stage == 2 ? uhdMode : 0);
        super.onCreate(state);
    }
    @Override protected void onResume() {
        super.onResume();
        probe.postDelayed(this::observe, SETTLE_MS);
    }
    @Override protected void onPause() { probe.removeCallbacksAndMessages(null); super.onPause(); }

    private void observe() {
        try {
            Display.Mode mode = getWindowManager().getDefaultDisplay().getMode();
            int[] surface = dashboardRenderingSurface();
            JSONObject row = new JSONObject().put("stage", stage).put("recreations", recreations)
                    .put("output", mode.getPhysicalWidth() + "x" + mode.getPhysicalHeight() + "@" + Math.round(mode.getRefreshRate()))
                    .put("renderer", surface == null ? "ordinary" : "native " + surface[0] + "x" + surface[1])
                    .put("dashboard", getResources().getDisplayMetrics().widthPixels + "x" + getResources().getDisplayMetrics().heightPixels)
                    .put("uhdWorkspace", native4kWorkspace());
            observed.put(row); Log.i(TAG, row.toString());
            if (stage == 0) {
                require(isUhd(mode) && surface != null && surface[0] == 3840 && surface[1] == 2160, "Native UHD rendering was not active at start");
                originalLayout = layoutText(); originalFocus = String.valueOf(field("focused")); originalPage = (Integer)field("homePage");
                uhdMode = mode.getModeId(); hdMode = closest(1920, 1080, mode.getRefreshRate());
                require(hdMode != 0, "No 1920x1080 output mode is advertised");
                advance(1, hdMode);
            } else if (stage == 1) {
                require(mode.getPhysicalWidth() == 1920 && mode.getPhysicalHeight() == 1080, "HD output not active after recreation");
                require(surface == null, "Native rendering stayed active on HD output");
                require(native4kWorkspace(), "HD output relabelled the UHD workspace baseline");
                retained();
                advance(2, uhdMode);
            } else if (stage == 2) {
                require(isUhd(mode) && surface != null && surface[0] == 3840 && surface[1] == 2160, "Native UHD rendering did not return");
                retained();
                stage = 3; requestMode(0);
                report("pass", "UHD native -> HD ordinary -> UHD native by live output change; layout, page and focus retained; UHD baseline kept");
            }
        } catch (Exception failure) {
            stage = 3; requestMode(0);
            report("fail", refusal != null ? refusal : failure.toString());
        }
    }
    /** Request the next output on this window; Android recreates the dashboard when the mode actually changes. */
    private void advance(int next, int modeId) {
        int before = recreations; stage = next; requestMode(modeId);
        probe.postDelayed(() -> {
            if (recreations == before && stage == next) {
                refusal = "Output mode request " + modeId + " was not applied within " + TRANSITION_DEADLINE_MS + " ms (stage " + next + ")";
                stage = 3; requestMode(0); report("fail", refusal);
            }
        }, TRANSITION_DEADLINE_MS);
    }
    private void retained() {
        require(originalLayout.equals(layoutText()), "Saved layout changed across the output transition");
        require(originalFocus.equals(String.valueOf(field("focused"))) && originalPage.equals(field("homePage")), "Page or focus changed across the output transition");
    }
    private void requestMode(int modeId) {
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.preferredDisplayModeId = modeId; getWindow().setAttributes(params);
    }
    private int closest(int width, int height, float refresh) {
        int best = 0; float distance = Float.MAX_VALUE;
        for (Display.Mode m : getWindowManager().getDefaultDisplay().getSupportedModes())
            if (m.getPhysicalWidth() == width && m.getPhysicalHeight() == height && Math.abs(m.getRefreshRate() - refresh) < distance) {
                best = m.getModeId(); distance = Math.abs(m.getRefreshRate() - refresh);
            }
        return best;
    }
    private static boolean isUhd(Display.Mode mode) { return mode.getPhysicalWidth() >= 3840 && mode.getPhysicalHeight() >= 2160; }
    private String layoutText() { return getSharedPreferences("widget-home", 0).getString("layout", ""); }
    private Object field(String name) {
        try { java.lang.reflect.Field f = MainActivity.class.getDeclaredField(name); f.setAccessible(true); return f.get(this); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    private static void require(boolean value, String reason) { if (!value) throw new IllegalStateException(reason); }
    private void report(String status, String detail) {
        try {
            JSONObject result = new JSONObject().put("status", status).put("detail", detail).put("observed", observed)
                    .put("homeboardVersion", getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
            Log.i(TAG, result.toString());
            Files.write(new File(getExternalFilesDir(null), "wf.log").toPath(), result.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception failure) { Log.e(TAG, "Report failed", failure); }
    }
}
