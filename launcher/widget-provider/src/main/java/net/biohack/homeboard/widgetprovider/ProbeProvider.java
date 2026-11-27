package net.biohack.homeboard.widgetprovider;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * Disposable no-configuration widget. Every render counts itself, shows the size the host last
 * reported, and offers a Press button, so a host can prove updates, size response and interaction.
 */
public final class ProbeProvider extends AppWidgetProvider {
    static final String TAG = "WidgetProbeProvider";
    static final String PRESS = "net.biohack.homeboard.widgetprovider.PRESS";

    static void render(Context context, int id, Bundle options) {
        SharedPreferences state = context.getSharedPreferences("probe", 0);
        int updates = state.getInt("updates-" + id, 0) + 1, presses = state.getInt("presses-" + id, 0);
        state.edit().putInt("updates-" + id, updates).apply();
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        if (options == null) options = manager.getAppWidgetOptions(id);
        int width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH), height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.interactive_widget);
        views.setTextViewText(R.id.probe_value, "Widget bound across apps · id " + id);
        views.setTextViewText(R.id.probe_detail, "Updates " + updates + " · " + width + "×" + height + " dp · Presses " + presses);
        Intent press = new Intent(context, ProbeProvider.class).setAction(PRESS).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        views.setOnClickPendingIntent(R.id.probe_press, PendingIntent.getBroadcast(context, id, press, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        manager.updateAppWidget(id, views);
        Log.i(TAG, "RENDER id=" + id + " updates=" + updates + " size=" + width + "x" + height + " presses=" + presses);
    }

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) render(context, id, null);
    }

    @Override public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager, int id, Bundle options) {
        render(context, id, options);
    }

    @Override public void onReceive(Context context, Intent intent) {
        if (PRESS.equals(intent.getAction())) {
            int id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return;
            SharedPreferences state = context.getSharedPreferences("probe", 0);
            state.edit().putInt("presses-" + id, state.getInt("presses-" + id, 0) + 1).commit();
            Log.i(TAG, "PRESS id=" + id);
            render(context, id, null);
            return;
        }
        super.onReceive(context, intent);
    }

    @Override public void onDeleted(Context context, int[] ids) {
        SharedPreferences.Editor edit = context.getSharedPreferences("probe", 0).edit();
        for (int id : ids) edit.remove("updates-" + id).remove("presses-" + id);
        edit.apply();
    }
}
