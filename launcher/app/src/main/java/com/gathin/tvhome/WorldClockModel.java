package com.gathin.tvhome;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** All clocks in a frame derive from one instant, including DST and date-line changes. */
final class WorldClockModel {
    final Clock clock;
    WorldClockModel(Clock clock) { this.clock=clock; }
    Instant now() { return clock.instant(); }
    static List<WorldClockSettings.Place> ordered(WorldClockSettings s) {
        List<WorldClockSettings.Place> result=new ArrayList<>(s.places);
        if("west".equals(s.ordering)) result.sort(Comparator.comparingDouble(p->p.located()?p.longitude:Double.POSITIVE_INFINITY));
        return result;
    }
    static Map<String,Float> hues(WorldClockSettings s) {
        List<WorldClockSettings.Place> ranked=new ArrayList<>(s.places);
        ranked.removeIf(p->!p.located()); ranked.sort(Comparator.comparingDouble(p->p.longitude));
        Map<String,Float> colors=new LinkedHashMap<>();
        for(int i=0;i<ranked.size();i++) colors.put(ranked.get(i).id,ranked.size()==1?150f:i*300f/(ranked.size()-1));
        return colors;
    }
    static String time(Instant instant,ZoneId zone,Locale locale,boolean twelve,boolean seconds) {
        // AM/PM stays an explicit short marker in both supported UI languages.
        return DateTimeFormatter.ofPattern(twelve?(seconds?"h:mm:ss a":"h:mm a"):(seconds?"HH:mm:ss":"HH:mm"),twelve?Locale.ROOT:locale).withZone(zone).format(instant);
    }
    static long dayDifference(Instant instant,ZoneId zone,ZoneId reference) {
        return ChronoUnit.DAYS.between(instant.atZone(reference).toLocalDate(),instant.atZone(zone).toLocalDate());
    }
    static String offset(Instant instant,ZoneId zone) { return "UTC"+zone.getRules().getOffset(instant).getId().replace("Z","+00:00"); }
    static int differenceMinutes(Instant instant,ZoneId zone,ZoneId reference) {
        return (zone.getRules().getOffset(instant).getTotalSeconds()-reference.getRules().getOffset(instant).getTotalSeconds())/60;
    }
    static boolean daytime(Instant instant,ZoneId zone) { int hour=instant.atZone(zone).getHour(); return hour>=6&&hour<18; }
    static Instant windowStart(Instant now,ZoneId reference) { return now.atZone(reference).truncatedTo(ChronoUnit.HOURS).toInstant(); }
    static Instant nextHalfHour(Instant now,ZoneId reference) {
        Instant next=windowStart(now,reference); while(!next.isAfter(now)) next=next.plusSeconds(1800); return next;
    }
    static Instant pageStart(Instant now,ZoneId reference,int page) { return windowStart(now,reference).plus(Duration.ofHours(Math.max(0,page)*24L)); }
    static double[] overlay(Instant window,Instant selected,int minutes) {
        double start=Duration.between(window,selected).toMillis()/86400000d,end=start+minutes/1440d;
        double left=Math.max(0,Math.min(1,start)),right=Math.max(0,Math.min(1,end));
        return new double[]{left,Math.max(0,right-left)};
    }
    static double mapX(double longitude) { return (longitude+180)/360; }
    static double mapY(double latitude) { return (90-latitude)/180; }
}
