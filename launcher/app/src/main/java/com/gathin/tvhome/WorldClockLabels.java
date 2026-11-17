package com.gathin.tvhome;

import android.content.Context;
import android.text.format.DateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.zone.ZoneOffsetTransition;
import java.util.Locale;

final class WorldClockLabels {
    private WorldClockLabels() { }
    static Locale locale(Context c) { return c.getResources().getConfiguration().getLocales().get(0); }
    static String name(Context c,WorldClockSettings.Place p) {
        return !p.label.isEmpty()?p.label:WorldClockSettings.DEVICE.equals(p.zone)?c.getString(R.string.hb_wc_local):p.zone.replace('_',' ');
    }
    static boolean twelve(Context c,WorldClockSettings s) { return "12".equals(s.format)||"device".equals(s.format)&&!DateFormat.is24HourFormat(c); }
    static String time(Context c,WorldClockSettings s,Instant at,ZoneId zone,boolean seconds) {
        return WorldClockModel.time(at,zone,locale(c),twelve(c,s),seconds);
    }
    static String date(Context c,Instant at,ZoneId zone) { return DateTimeFormatter.ofPattern("EEE d MMM uuuu",locale(c)).withZone(zone).format(at); }
    static String country(Context c,WorldClockSettings.Place p) { return p.country.isEmpty()?"":new Locale("",p.country).getDisplayCountry(locale(c)); }
    static String day(Context c,Instant at,ZoneId zone,ZoneId reference) {
        long days=WorldClockModel.dayDifference(at,zone,reference);
        return days==0?"":days==1?c.getString(R.string.hb_wc_tomorrow):days==-1?c.getString(R.string.hb_wc_yesterday):c.getString(R.string.hb_wc_day_difference,days);
    }
    static String detail(Context c,WorldClockSettings s,WorldClockSettings.Place p,Instant at,boolean seconds,Integer duration) {
        String name=name(c,p); ZoneId zone=p.zone(ZoneId.systemDefault());
        if(zone==null) return name+"\n\n"+c.getString(R.string.hb_wc_zone_unavailable)+"\n"+p.zone;
        StringBuilder b=new StringBuilder(name).append("\n").append(country(c,p)).append("\n\n")
                .append(time(c,s,at,zone,seconds)).append("\n").append(date(c,at,zone)).append("\n\n")
                .append(zone.getId()).append(" · ").append(WorldClockModel.offset(at,zone));
        if(duration!=null) {
            Instant end=at.plusSeconds(duration*60L);
            b.append("\n").append(c.getString(R.string.hb_wc_until)).append(" ").append(time(c,s,end,zone,false)).append(" · ").append(date(c,end,zone));
        }
        ZoneId reference=s.reference(ZoneId.systemDefault());
        int minutes=WorldClockModel.differenceMinutes(at,zone,reference);
        b.append("\n").append(c.getString(R.string.hb_wc_difference,String.format(Locale.ROOT,"%s%d:%02d",minutes<0?"−":"+",Math.abs(minutes)/60,Math.abs(minutes)%60),reference.getId()));
        String day=day(c,at,zone,reference); if(!day.isEmpty()) b.append("\n").append(day);
        boolean daylight=zone.getRules().isDaylightSavings(at);
        b.append("\n\n").append(c.getString(daylight?R.string.hb_wc_dst:R.string.hb_wc_standard));
        ZoneOffsetTransition next=zone.getRules().nextTransition(at);
        b.append("\n").append(c.getString(R.string.hb_wc_transition)).append(": ");
        if(next==null) b.append(c.getString(R.string.hb_wc_no_transition));
        else b.append(next.getDateTimeBefore()).append(" ").append(next.getOffsetBefore()).append(" → ")
                .append(next.getDateTimeAfter()).append(" ").append(next.getOffsetAfter());
        b.append("\n\n").append(c.getString(R.string.hb_wc_clock_basis)).append("\n").append(c.getString(R.string.hb_wc_daytime_legend));
        if(!p.located()) b.append("\n").append(c.getString(R.string.hb_wc_no_position));
        return b.toString();
    }
}
