package com.gathin.tvhome;

import org.junit.Test;
import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class ExpandedWidgetModelsTest {
    @Test public void favoritesFitsSixteenBannerSlotsAndReflowsForLargeText(){
        FavoritesExpandedGeometry normal=new FavoritesExpandedGeometry(625,420,1);
        assertEquals(4,normal.columns);assertEquals(4,normal.rows);assertEquals(105,normal.cellHeight,.01);
        assertEquals(420,normal.cellHeight*normal.rows,.01);
        FavoritesExpandedGeometry large=new FavoritesExpandedGeometry(625,420,1.4f);
        assertTrue(large.columns<normal.columns);assertTrue(large.rows<normal.rows);
        assertTrue(large.cellHeight>normal.cellHeight);
    }
    @Test public void allDefaultWidgetTypesHaveExplicitPresentationsAndUnknownTypesFail(){
        HomeLayout home=HomeLayout.firstRun();
        for(String key:home.widgets)assertNotNull(WidgetKind.of(key));
        assertEquals(WidgetKind.AIR,WidgetKind.of("widget:air:12"));
        assertEquals(WidgetKind.RAIN,WidgetKind.of("widget:rain:2"));
        assertThrows(IllegalArgumentException.class,()->WidgetKind.of("widget:unknown"));
        assertThrows(IllegalArgumentException.class,()->WidgetKind.of("widget:calendar-fake"));
    }
    @Test public void fourWeeksIncludeOrdinaryDaysAndNavigateMonthYearBoundaries(){
        CalendarWeekWindow w=new CalendarWeekWindow(LocalDate.of(2026,11,17));
        assertEquals(LocalDate.of(2026,11,16),w.start);assertEquals(LocalDate.of(2026,12,13),w.end());
        assertEquals(DayOfWeek.MONDAY,w.start.getDayOfWeek());assertEquals(DayOfWeek.SUNDAY,w.end().getDayOfWeek());
        w.select(w.end().plusDays(1));assertEquals(LocalDate.of(2026,12,14),w.start);
        w.page(1);assertEquals(LocalDate.of(2027,1,11),w.start);
        w.select(w.start.minusDays(1));assertFalse(w.selected.isBefore(w.start));assertFalse(w.selected.isAfter(w.end()));
    }
    @Test public void expandedCalendarWindowBypassesCompactSevenDaysWithoutChangingSettings()throws Exception{
        CalendarSnapshot data=CalendarSnapshot.parse(new String(Files.readAllBytes(Path.of("src/main/assets/calendar-snapshot.json")),java.nio.charset.StandardCharsets.UTF_8));
        CalendarWidgetSettings settings=new CalendarWidgetSettings();settings.enabled.clear();settings.enabled.add("usa");settings.holidayLookaheadDays=7;
        String before=settings.json().toString();
        assertTrue(data.display(settings,Instant.parse("2026-11-17T05:00:00Z")).days.isEmpty());
        CalendarSnapshot.Display expanded=data.display(settings,LocalDate.of(2026,11,16),LocalDate.of(2026,12,13));
        assertTrue(expanded.days.stream().anyMatch(day->day.date.equals(LocalDate.of(2026,11,26))));
        assertEquals(before,settings.json().toString());
    }
    private JSONObject station(String id,double lat,double lon)throws Exception{
        return new JSONObject().put("MeasIndex",id).put("Lat",String.valueOf(lat)).put("Long",lon).put("Area","Station "+id).put("DateTime","2026-11-17 14:00:00").put("PM2.5",10.1);
    }
    @Test public void stationCatalogRejectsBadCoordinatesDeduplicatesAndKeepsMissingValues()throws Exception{
        JSONArray rows=new JSONArray().put(station("a",13.7,100.5)).put(station("a",13.8,100.5)).put(station("bad",91,100.5)).put(station("b",13.71,100.5).put("PM2.5",JSONObject.NULL));
        List<StationCatalog.Station> stations=StationCatalog.parse(new JSONObject().put("status","Success").put("message",rows).toString(),Instant.parse("2026-11-17T08:00:00Z").toEpochMilli());
        assertEquals(2,stations.size());assertNotNull(stations.get(0).reading());assertNull(stations.get(1).reading());
        assertEquals(1,StationCatalog.nearby(stations,13.7,100.5,.5).size());
        assertEquals(0,StationCatalog.nearby(stations,40,-3,100).size());
        assertEquals(0,StationCatalog.nearby(stations,Double.NaN,0,25).size());
    }
    @Test public void distancesAndMercatorKeepActualAndStationCoordinatesInOneProjection(){
        assertEquals(0,StationCatalog.distanceKm(13.7,100.5,13.7,100.5),1e-8);
        assertEquals(2.224,StationCatalog.distanceKm(0,179.99,0,-179.99),.01);
        for(double lat:new double[]{-80,0,13.7,80})assertEquals(lat,MapGeometry.latitude(MapGeometry.y(lat)),1e-7);
        assertEquals(100.5,MapGeometry.longitude(MapGeometry.x(100.5)),1e-7);
        assertEquals(.02,MapGeometry.wrapped(-.98),1e-7);
        assertTrue(Double.isFinite(MapGeometry.y(90)));
    }
    private RainForecast rain()throws Exception{
        Instant base=Instant.parse("2026-11-17T00:00:00Z");JSONArray times=new JSONArray(),amount=new JSONArray(),zero=new JSONArray(),chance=new JSONArray();
        for(int i=0;i<240;i++){times.put(base.getEpochSecond()+(i+1)*3600);amount.put(i==10?8:.2);zero.put(0);chance.put(i==10?20:90);}
        JSONObject units=new JSONObject().put("time","unixtime").put("rain","mm").put("showers","mm").put("snowfall","cm").put("precipitation_probability","%");
        JSONObject hourly=new JSONObject().put("time",times).put("rain",amount).put("showers",zero).put("snowfall",zero).put("precipitation_probability",chance);
        return RainForecast.parse(new JSONObject().put("timezone","UTC").put("hourly_units",units).put("hourly",hourly).toString());
    }
    @Test public void expandedRainCanReadSevenDaysIndependentlyOfCompactSettings()throws Exception{
        RainForecast f=rain();Instant now=Instant.parse("2026-11-17T00:30:00Z");
        List<RainForecast.Period> days=f.calendarDays(now,7);assertEquals(7,days.size());assertEquals(23,days.get(0).hours.size());assertEquals(24,days.get(1).hours.size());
        assertTrue(days.get(6).complete);assertEquals(days.get(0).end,days.get(1).start);
        RainForecast.Hour heavy=days.get(0).hours.stream().filter(hour->hour.mm==8).findFirst().get();assertEquals(20,heavy.chance,0);assertEquals(8,heavy.mm,0);
        assertTrue(days.get(0).wettestThreeHours()>=0);
        assertThrows(IllegalArgumentException.class,()->f.interval(100,99));
    }
    @Test public void rainAmountAndProbabilityHaveIndependentMissingValuesAndNoProbabilityWeighting(){
        java.util.Map<Long,RainForecast.Hour> samples=new java.util.TreeMap<>();
        samples.put(0L,new RainForecast.Hour(0,8,20,0));samples.put(3600L,new RainForecast.Hour(3600,1,Double.NaN,0));
        RainForecast.Period amount=new RainForecast.Period(0,7200,samples);assertEquals(9,amount.mm,0);assertTrue(Double.isNaN(amount.chance));assertFalse(amount.complete);
        samples.put(3600L,new RainForecast.Hour(3600,Double.NaN,90,0));RainForecast.Period chance=new RainForecast.Period(0,7200,samples);assertTrue(Double.isNaN(chance.mm));assertEquals(90,chance.chance,0);assertEquals(-1,chance.wettestThreeHours());
    }
}
