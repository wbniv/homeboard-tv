package net.biohack.homeboard.widgetprovider;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Disposable public-API configuration target; has no household data or network. */
public class ConfigureActivity extends Activity {
    private int widgetId;
    private boolean saved;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        widgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        setResult(RESULT_CANCELED, result());
        Log.i(ConfiguredProvider.TAG, "LAUNCH id=" + widgetId);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(32), dp(24), dp(32), dp(24));
        content.setBackgroundColor(Color.rgb(24, 35, 50));
        TextView description = new TextView(this);
        description.setTextColor(Color.WHITE);
        description.setTextSize(24);
        description.setText("Configure test widget\nWidget ID: " + widgetId
                + " · received from Homeboard\n\nSave returns this instance to Homeboard's Add review."
                + "\nCancel or Back discards this pending setup.");
        content.addView(description, new LinearLayout.LayoutParams(-1, 0, 1));
        Button save = button(content, "Save");
        save.setEnabled(widgetId != AppWidgetManager.INVALID_APPWIDGET_ID);
        save.setOnClickListener(v -> {
            if (!getSharedPreferences("configured", 0).edit().putBoolean("saved-" + widgetId, true).commit()) return;
            ConfiguredProvider.update(this, widgetId);
            saved = true;
            setResult(RESULT_OK, result());
            Log.i(ConfiguredProvider.TAG, "SAVE id=" + widgetId);
            finish();
        });
        Button cancel = button(content, "Cancel");
        cancel.setOnClickListener(v -> finish());
        setContentView(content);
        (save.isEnabled() ? save : cancel).requestFocus();
    }
    private Intent result() { return new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private Button button(LinearLayout parent, String label) {
        Button button = new Button(this);
        button.setId(View.generateViewId());
        button.setText(label);
        button.setTextSize(22);
        parent.addView(button, new LinearLayout.LayoutParams(dp(340), dp(64)));
        return button;
    }
    @Override public void finish() {
        if (!saved) Log.i(ConfiguredProvider.TAG, "CANCEL id=" + widgetId);
        super.finish();
    }
}
