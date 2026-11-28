package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;
import java.util.Calendar;

/**
 * Debug-only launcher entry for the Fossify real-event check (docs/plans/2026-11-19-android-app-widgets.md):
 * asks Fossify Calendar to open its new-event screen with a clearly labelled test title for today,
 * so the remote only has to Save. Homeboard reads and writes no calendar data itself.
 */
public final class FossifyEventProbeActivity extends Activity {
    static final String FOSSIFY = "org.fossify.calendar", TITLE = "Homeboard test event";
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        Calendar begin = Calendar.getInstance();
        int day = begin.get(Calendar.DAY_OF_YEAR);
        begin.add(Calendar.HOUR_OF_DAY, 1); begin.set(Calendar.MINUTE, 0); begin.set(Calendar.SECOND, 0); begin.set(Calendar.MILLISECOND, 0);
        if (begin.get(Calendar.DAY_OF_YEAR) != day) begin = Calendar.getInstance();
        long start = begin.getTimeInMillis(), end = Math.min(start + 30 * 60_000L, endOfDay());
        Intent insert = new Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI).setPackage(FOSSIFY)
                .putExtra(CalendarContract.Events.TITLE, TITLE)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end);
        try { startActivity(insert); Log.i("HomeboardFossifyProbe", "INSERT title=" + TITLE + " begin=" + start + " end=" + end); }
        catch (RuntimeException failure) { Log.e("HomeboardFossifyProbe", "Fossify did not accept INSERT", failure); }
        finish();
    }
    private static long endOfDay() {
        Calendar c = Calendar.getInstance(); c.set(Calendar.HOUR_OF_DAY, 23); c.set(Calendar.MINUTE, 59); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}
