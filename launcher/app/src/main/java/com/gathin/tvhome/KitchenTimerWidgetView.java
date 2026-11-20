package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Glance at several dishes; OK consistently opens the full timer collection. */
final class KitchenTimerWidgetView extends LinearLayout {
    private final PaletteColors palette;
    private final List<TextView> values = new ArrayList<>();
    private String arrangement = "";
    private final TimerTicker.Listener listener = this::refresh;

    KitchenTimerWidgetView(Context c, PaletteColors palette, int opacity) {
        super(c);
        this.palette = palette;
        setOrientation(VERTICAL);
        int pad = dp(10);
        setPadding(pad, pad, pad, pad);
        UiBackgroundDrawable bg = new UiBackgroundDrawable(palette.surface);
        bg.setFillOpacity(opacity);
        bg.setCornerRadius(pad);
        setBackground(bg);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
    }

    private int dp(float n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private int sp(float n) { return Math.round(n * getResources().getDisplayMetrics().scaledDensity); }

    private TextView label(String text, int size, int color) {
        TextView view = new TextView(getContext());
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setIncludeFontPadding(false);
        view.setMaxLines(1);
        view.setEllipsize(TextUtils.TruncateAt.END);
        return view;
    }

    private void refresh(TimerTickerSchedule.Snapshot tick) {
        int width = getWidth() - getPaddingLeft() - getPaddingRight();
        int height = getHeight() - getPaddingTop() - getPaddingBottom();
        if (width <= 0 || height <= 0) return;
        List<JSONObject> items = KitchenTimerUi.ordered(tick.data.optJSONArray("items"));
        // Allocate whole timer cells, not text lines: a wide one-row card can
        // show two running timers without needing extra lines for a heading.
        int columns = Math.max(1, Math.min(4, width / Math.max(dp(160), sp(160))));
        columns = Math.min(columns, Math.max(1, items.size()));
        boolean heading = height >= sp(150);
        int bodyHeight = height - (heading ? sp(26) : 0);
        int rows = Math.max(1, bodyHeight / sp(72));
        if (items.size() > columns * rows) rows = Math.max(1, (bodyHeight - sp(20)) / sp(72));
        int count = Math.min(items.size(), columns * rows);
        int hidden = items.size() - count;
        StringBuilder key = new StringBuilder().append(tick.revision).append(':')
                .append(columns).append(':').append(rows).append(':').append(heading);
        for (int i = 0; i < count; i++) key.append(':').append(items.get(i).optString("id"));
        if (!arrangement.contentEquals(key)) {
            arrangement = key.toString();
            removeAllViews();
            values.clear();
            if (heading || items.isEmpty()) {
                addView(label(getContext().getString(R.string.hb_timer_title), 18, palette.muted));
            }
            if (items.isEmpty()) {
                TextView empty = label(getContext().getString(R.string.hb_timer_empty), 18, palette.text);
                empty.setMaxLines(3);
                empty.setGravity(Gravity.CENTER_VERTICAL);
                empty.setAutoSizeTextTypeUniformWithConfiguration(12, 20, 1, TypedValue.COMPLEX_UNIT_SP);
                addView(empty, new LayoutParams(-1, 0, 1));
            } else {
                LinearLayout grid = new LinearLayout(getContext());
                grid.setOrientation(VERTICAL);
                addView(grid, new LayoutParams(-1, 0, 1));
                for (int offset = 0; offset < count; offset += columns) {
                    LinearLayout row = new LinearLayout(getContext());
                    grid.addView(row, new LayoutParams(-1, 0, 1));
                    for (int col = 0; col < columns; col++) {
                        LinearLayout cell = new LinearLayout(getContext());
                        cell.setOrientation(VERTICAL);
                        LayoutParams cellSize = new LayoutParams(0, -1, 1);
                        if (col > 0) cellSize.leftMargin = dp(12);
                        row.addView(cell, cellSize);
                        int index = offset + col;
                        if (index >= count) continue;
                        JSONObject item = items.get(index);
                        String name = KitchenTimerUi.name(getContext(), item);
                        if ("paused".equals(item.optString("state"))) {
                            name = getContext().getString(R.string.hb_timer_paused) + " · " + name;
                        }
                        TextView nameView = label(name, 18, palette.muted);
                        nameView.setPadding(0, dp(2), 0, dp(2));
                        cell.addView(nameView);
                        TextView value = label("", 48, palette.text);
                        value.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                        value.setFontFeatureSettings("tnum");
                        value.setGravity(Gravity.CENTER_VERTICAL);
                        value.setAutoSizeTextTypeUniformWithConfiguration(12, 56, 1, TypedValue.COMPLEX_UNIT_SP);
                        cell.addView(value, new LayoutParams(-1, 0, 1));
                        values.add(value);
                    }
                }
                // Never silently hide more entries, including in a shallow 1x1 card.
                if (hidden > 0) addView(label(getContext().getString(R.string.hb_timer_more, hidden), 14, palette.muted));
            }
        }
        for (int i = 0; i < count; i++) values.get(i).setText(glance(items.get(i)));
        StringBuilder description = new StringBuilder(getContext().getString(R.string.hb_timer_title));
        for (JSONObject item : items) description.append(" · ")
                .append(KitchenTimerUi.name(getContext(), item)).append(" · ")
                .append(KitchenTimerUi.detail(getContext(), item));
        setContentDescription(description);
    }

    private String glance(JSONObject item) {
        String state = item.optString("state");
        if ("done".equals(state)) return getContext().getString(R.string.hb_timer_finished);
        if ("timer".equals(item.optString("kind")) && ("running".equals(state) || "paused".equals(state))) {
            return KitchenTimers.clock(KitchenTimers.remaining(item, SystemClock.elapsedRealtime(), System.currentTimeMillis()));
        }
        if ("event".equals(item.optString("kind")) && "running".equals(state)) {
            return DisplayTime.timestamp(getContext(), java.time.Instant.ofEpochMilli(item.optLong("at")),
                    java.time.ZoneId.systemDefault(), "HH:mm");
        }
        return getContext().getString(KitchenTimerUi.state(state));
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        post(() -> refresh(TimerTicker.get(getContext()).current()));
    }
    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        TimerTicker.get(getContext()).listen(listener);
    }
    @Override protected void onDetachedFromWindow() {
        TimerTicker.get(getContext()).unlisten(listener);
        super.onDetachedFromWindow();
    }
}
