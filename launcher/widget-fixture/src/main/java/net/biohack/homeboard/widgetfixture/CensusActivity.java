package net.biohack.homeboard.widgetfixture;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Read-only stage-0 gate. Never allocates IDs, binds, grants permissions or starts setup. */
public final class CensusActivity extends Activity {
    public static final String TAG = "HB_WIDGET_CENSUS";
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        JSONObject report = new JSONObject();
        StringBuilder screen = new StringBuilder("Homeboard · Android widget census\n\n");
        try {
            PackageManager pm = getPackageManager();
            boolean feature = pm.hasSystemFeature(PackageManager.FEATURE_APP_WIDGETS);
            report.put("schema",1).put("probeVersion",1).put("observedAt",System.currentTimeMillis())
                .put("feature",feature).put("sdk",android.os.Build.VERSION.SDK_INT);
            AppWidgetManager manager = getSystemService(AppWidgetManager.class);
            report.put("service",manager != null);
            Intent bind = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
            ResolveInfo resolved = pm.resolveActivity(bind, PackageManager.MATCH_DEFAULT_ONLY);
            String resolver = resolved == null || resolved.activityInfo == null ? "" :
                resolved.activityInfo.packageName + "/" + resolved.activityInfo.name;
            JSONArray candidates = new JSONArray();
            for (ResolveInfo r : pm.queryIntentActivities(bind, PackageManager.MATCH_DEFAULT_ONLY))
                candidates.put(r.activityInfo.packageName + "/" + r.activityInfo.name);
            report.put("bindResolver",resolver).put("bindActivities",candidates);
            screen.append("Platform feature: ").append(feature).append("\nWidget service: ").append(manager != null)
                .append("\nBinding approval: ").append(resolver.isEmpty() ? "NO ACTIVITY" : resolver).append("\n");
            List<AppWidgetProviderInfo> providers = manager == null ? new ArrayList<>() : new ArrayList<>(manager.getInstalledProviders());
            providers.sort(Comparator.comparing(p -> p.provider.flattenToString()));
            JSONArray records = new JSONArray();
            screen.append("Installed providers: ").append(providers.size()).append("\n\n");
            for (AppWidgetProviderInfo p : providers) {
                JSONObject item = new JSONObject().put("component",p.provider.flattenToString())
                    .put("label",p.loadLabel(pm)).put("minWidthPx",p.minWidth).put("minHeightPx",p.minHeight)
                    .put("resizeMode",p.resizeMode).put("configure",p.configure == null ? "" : p.configure.flattenToString());
                if (android.os.Build.VERSION.SDK_INT >= 28) item.put("widgetFeatures",p.widgetFeatures);
                android.content.pm.PackageInfo pkg = pm.getPackageInfo(p.provider.getPackageName(),0);
                item.put("versionCode",android.os.Build.VERSION.SDK_INT >= 28 ? pkg.getLongVersionCode() : pkg.versionCode);
                records.put(item);
                screen.append(p.loadLabel(pm)).append("\n").append(p.provider.flattenToShortString()).append("\n\n");
                Log.i(TAG,"provider="+item);
            }
            String outcome = !feature || manager == null ? "platform-unavailable" : resolver.isEmpty() ? "new-binding-unavailable" : "consent-ui-resolves";
            report.put("providers",records).put("providerCount",providers.size()).put("outcome",outcome);
            screen.append("Gate: ").append(outcome).append("\n\nRead-only probe · no widget IDs allocated\nBack returns to previous screen.");
            // Small summary remains easy to recover from coordinator logs even with many providers.
            JSONObject summary = new JSONObject(report.toString()); summary.remove("providers");
            Log.i(TAG,"summary="+summary);
            getFilesDir().mkdirs();
            try (java.io.FileOutputStream out = openFileOutput("widget-census.json",MODE_PRIVATE)) {
                out.write(report.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        } catch (Exception error) {
            Log.e(TAG,"census-failed",error);
            screen.append("Census failed: ").append(error.getClass().getSimpleName()).append(" · ").append(error.getMessage());
        }
        TextView text = new TextView(this);
        text.setText(screen); text.setTextSize(21); text.setTextColor(Color.WHITE);
        text.setPadding(48,36,48,36);
        ScrollView scroll = new ScrollView(this); scroll.setBackgroundColor(0xff182332); scroll.addView(text);
        scroll.setFocusable(true); setContentView(scroll);
    }
}
