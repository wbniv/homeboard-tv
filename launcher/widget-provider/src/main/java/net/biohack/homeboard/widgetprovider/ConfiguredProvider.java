package net.biohack.homeboard.widgetprovider;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.util.Log;
import android.widget.RemoteViews;

public class ConfiguredProvider extends AppWidgetProvider {
    static final String TAG = "WidgetConfigTest";
    static void update(Context context, int id) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.probe_widget);
        boolean saved = context.getSharedPreferences("configured", 0).getBoolean("saved-" + id, false);
        views.setTextViewText(R.id.probe_value, (saved ? "Configuration saved" : "Waiting for configuration") + " · id " + id);
        AppWidgetManager.getInstance(context).updateAppWidget(id, views);
        Log.i(TAG, "UPDATE id=" + id + " saved=" + saved);
    }
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) update(context, id);
    }
    @Override public void onDeleted(Context context, int[] ids) {
        android.content.SharedPreferences.Editor edit = context.getSharedPreferences("configured", 0).edit();
        for (int id : ids) { edit.remove("saved-" + id); Log.i(TAG, "DELETE id=" + id); }
        edit.apply();
    }
}
