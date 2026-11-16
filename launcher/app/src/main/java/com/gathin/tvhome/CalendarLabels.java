package com.gathin.tvhome;

import android.content.Context;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

final class CalendarLabels {
    private CalendarLabels() { }
    static String source(Context c, String id) {
        int resource;
        switch (id) {
            case "thai": resource = R.string.hb_cal_source_thai; break;
            case "flowers": resource = R.string.hb_cal_source_flowers; break;
            case "buddhist": resource = R.string.hb_cal_source_buddhist; break;
            case "spain": resource = R.string.hb_cal_source_spain; break;
            case "usa": resource = R.string.hb_cal_source_usa; break;
            default: resource = R.string.hb_cal_source_china; break;
        }
        return c.getString(resource);
    }
    static String event(Context c, CalendarSnapshot.Event event, CalendarWidgetSettings settings, Instant now) {
        int resource;
        switch (event.kind) {
            case MAKE: resource = R.string.hb_cal_make; break;
            case SELL: resource = CalendarSnapshot.sellingBreak(event, now) ? R.string.hb_cal_break : R.string.hb_cal_sell; break;
            case WAN_PHRA: resource = R.string.hb_cal_wan_phra; break;
            case WORKING: resource = R.string.hb_cal_working; break;
            default: return event.title(settings);
        }
        return c.getString(resource);
    }
    static String date(Context c, java.time.LocalDate date) {
        return DateTimeFormatter.ofPattern("EEE d MMM uuuu", c.getResources().getConfiguration().getLocales().get(0)).format(date);
    }
}
