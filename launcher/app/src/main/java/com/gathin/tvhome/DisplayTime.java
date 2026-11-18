package com.gathin.tvhome;

import android.content.Context;
import java.time.Instant;
import java.time.ZoneId;
import java.time.chrono.IsoChronology;
import java.time.chrono.ThaiBuddhistChronology;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Presentation only: stored dates, calculations and wire formats remain ISO. */
final class DisplayTime {
    static final String CLOCK="\u23f2\ufe0e ", STOPWATCH="\u23f1\ufe0e ";
    static Locale locale(Context c){return c.getResources().getConfiguration().getLocales().get(0);}
    static DateTimeFormatter formatter(String pattern,Locale locale){
        return DateTimeFormatter.ofPattern(pattern,locale).withChronology("th".equals(locale.getLanguage())
                ?ThaiBuddhistChronology.INSTANCE:IsoChronology.INSTANCE);
    }
    static String timestamp(Context c,Instant at,ZoneId zone,String pattern){
        return CLOCK+formatter(pattern,locale(c)).withZone(zone).format(at);
    }
    static String duration(long millis,String[] formats){
        long minutes=Math.max(0,millis)/60_000;
        if(minutes<60)return String.format(Locale.ROOT,formats[0],minutes);
        if(minutes<1440)return String.format(Locale.ROOT,formats[1],minutes/60,minutes%60);
        return String.format(Locale.ROOT,formats[2],minutes/1440,minutes/60%24);
    }
    static String duration(Context c,long millis){
        return STOPWATCH+duration(millis,new String[]{c.getString(R.string.hb_time_minutes),
                c.getString(R.string.hb_time_hours_minutes),c.getString(R.string.hb_time_days_hours)});
    }
    static String ago(Context c,long then,long now){return c.getString(R.string.hb_time_ago,duration(c,Math.max(0,now-then)));}
    private DisplayTime(){}
}
