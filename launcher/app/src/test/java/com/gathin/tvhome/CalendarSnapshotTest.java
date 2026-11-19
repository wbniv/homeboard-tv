package com.gathin.tvhome;

import org.json.JSONObject;
import org.json.JSONArray;
import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import static org.junit.Assert.*;

public class CalendarSnapshotTest {
    private String asset() throws Exception { return new String(Files.readAllBytes(Path.of("src/main/assets/calendar-snapshot.json")), java.nio.charset.StandardCharsets.UTF_8); }
    private CalendarSnapshot snapshot() throws Exception { return CalendarSnapshot.parse(asset()); }
    private Instant now(String time) { return LocalDateTime.parse(time).atZone(CalendarSnapshot.BANGKOK).toInstant(); }
    private CalendarWidgetSettings settings(String... sources) {
        CalendarWidgetSettings s = new CalendarWidgetSettings(); s.enabled.clear(); s.enabled.addAll(Arrays.asList(sources)); return s;
    }
    private List<LocalDate> dates(CalendarSnapshot.Display d) { return d.days.stream().map(x -> x.date).collect(Collectors.toList()); }
    @Test public void sharedSevenThirtyAndCustomLookaheadsAreInclusiveWithoutChangingWeeks() throws Exception {
        CalendarSnapshot data = snapshot(); CalendarWidgetSettings s = settings("thai", "spain", "usa", "china");
        Instant now = now("2026-11-16T12:00");
        s.holidayLookaheadDays = 7; assertTrue(data.display(s, now).days.isEmpty());
        s.holidayLookaheadDays = 30;
        assertEquals(Arrays.asList(LocalDate.parse("2026-11-26"), LocalDate.parse("2026-12-05"), LocalDate.parse("2026-12-07"), LocalDate.parse("2026-12-08"), LocalDate.parse("2026-12-10")), dates(data.display(s, now)));
        s.holidayLookaheadDays = 11; assertEquals(Arrays.asList(LocalDate.parse("2026-11-26")), dates(data.display(s, now)));
        s.holidayLookaheadDays = 10; assertTrue(data.display(s, now).days.isEmpty());
        CalendarSnapshot.Window week = CalendarSnapshot.window("flowers", s, now, CalendarSnapshot.BANGKOK);
        s.holidayLookaheadDays = 999;
        assertEquals(week.end, CalendarSnapshot.window("flowers", s, now, CalendarSnapshot.BANGKOK).end);
    }
    @Test public void weekBoundariesAreMondayBasedUpcomingOnlyAndIncludeOutsideAnchors() throws Exception {
        CalendarSnapshot data = snapshot(); CalendarWidgetSettings s = settings("flowers", "buddhist");
        CalendarSnapshot.Display monday = data.display(s, now("2026-11-16T12:00"));
        assertEquals(Arrays.asList(16,17,21,22,23,24,29), monday.days.stream().map(d -> d.date.getDayOfMonth()).collect(Collectors.toList()));
        assertEquals(LocalDate.parse("2026-12-02"), monday.days.get(monday.days.size()-1).events.get(0).anchor);
        CalendarSnapshot.Display sunday = data.display(s, now("2026-11-22T23:59:59"));
        assertEquals(Arrays.asList(22,23,24,29), sunday.days.stream().map(d -> d.date.getDayOfMonth()).collect(Collectors.toList()));
        CalendarSnapshot.Window next = CalendarSnapshot.window("flowers", s, now("2026-11-23T00:00"), CalendarSnapshot.BANGKOK);
        assertEquals(LocalDate.parse("2026-12-06"), next.end);
        s.flowerWeeks = CalendarWidgetSettings.Weeks.NEXT; s.enabled.remove("buddhist");
        assertEquals(Arrays.asList(23,24,29), data.display(s, now("2026-11-16T12:00")).days.stream().map(d -> d.date.getDayOfMonth()).collect(Collectors.toList()));
        s.flowerWeeks = CalendarWidgetSettings.Weeks.THIS;
        assertEquals(Arrays.asList(16,17,21,22), data.display(s, now("2026-11-16T12:00")).days.stream().map(d -> d.date.getDayOfMonth()).collect(Collectors.toList()));
    }
    @Test public void eventDatesAreGroupedWithAllKindsAndCountriesAndNoEmptyDates() throws Exception {
        CalendarSnapshot data = snapshot(); CalendarWidgetSettings s = new CalendarWidgetSettings();
        CalendarSnapshot.Display d = data.display(s, now("2026-11-16T12:00"));
        CalendarSnapshot.Day phra = d.days.stream().filter(day -> day.date.equals(LocalDate.parse("2026-11-17"))).findFirst().get();
        assertEquals(2, phra.events.size()); assertEquals(Arrays.asList("TH"), phra.countries());
        s.holidayLookaheadDays=60;
        CalendarSnapshot.Day christmas = data.display(s, now("2026-11-16T12:00")).days.stream().filter(day -> day.date.equals(LocalDate.parse("2026-12-25"))).findFirst().get();
        assertEquals(Arrays.asList("ES","US"), christmas.countries());
        assertFalse(dates(d).contains(LocalDate.parse("2026-11-18")));
    }
    @Test public void fourCountryNewYearUsesOneCardInDeterministicFlagOrder() throws Exception {
        CalendarWidgetSettings s=settings("thai","spain","usa","china");s.holidayLookaheadDays=1;
        CalendarSnapshot.Display d=snapshot().display(s,now("2026-01-01T12:00"));
        assertEquals(1,d.days.size());assertEquals(Arrays.asList("TH","ES","US","CN"),d.days.get(0).countries());
        JSONObject root=new JSONObject(asset());JSONArray providers=root.getJSONArray("providers"), reverse=new JSONArray();
        for(int i=providers.length()-1;i>=0;i--)reverse.put(providers.get(i));
        root.put("providers",reverse);
        assertEquals(d.days.get(0).countries(),CalendarSnapshot.parse(root.toString()).display(s,now("2026-01-01T12:00")).days.get(0).countries());
    }
    @Test public void countryEventsRespectSourceWindowsBeforeUnion() throws Exception {
        CalendarWidgetSettings s = settings("flowers", "buddhist", "usa"); s.holidayLookaheadDays=1;
        CalendarSnapshot.Display d = snapshot().display(s, now("2026-11-16T12:00"));
        assertFalse(dates(d).contains(LocalDate.parse("2026-11-26")));
        assertTrue(dates(d).contains(LocalDate.parse("2026-11-24")));
    }
    @Test public void numbersRejectMalformedFractionsZeroAndOverflowAndPersistUndoSnapshot() throws Exception {
        for (String invalid : Arrays.asList("", "0", "-1", "1.5", " 7", "7 ", "+7", "NaN", "2147483648")) {
            try { CalendarWidgetSettings.days(invalid); fail(invalid); } catch (IllegalArgumentException expected) { }
        }
        assertEquals(7, CalendarWidgetSettings.days("7")); assertEquals(30, CalendarWidgetSettings.days("30"));
        assertEquals(77, CalendarWidgetSettings.days("77")); assertEquals(Integer.MAX_VALUE, CalendarWidgetSettings.days("2147483647"));
        HomeLayout l = new HomeLayout(); l.addPanel(HomeLayout.CALENDAR);
        String before=l.encode(); l.calendarSettings.get(HomeLayout.CALENDAR).holidayLookaheadDays=7;
        l.calendarSettings.get(HomeLayout.CALENDAR).enabled.remove("usa");
        l.calendarSettings.get(HomeLayout.CALENDAR).flowerWeeks=CalendarWidgetSettings.Weeks.THIS;
        HomeLayout after=HomeLayout.parse(l.encode());
        assertEquals(7,after.calendarSettings.get(HomeLayout.CALENDAR).holidayLookaheadDays);
        assertFalse(after.calendarSettings.get(HomeLayout.CALENDAR).enabled.contains("usa"));
        assertEquals(CalendarWidgetSettings.Weeks.THIS, after.calendarSettings.get(HomeLayout.CALENDAR).flowerWeeks);
        assertEquals(30,HomeLayout.parse(before).calendarSettings.get(HomeLayout.CALENDAR).holidayLookaheadDays);
        l.delete(HomeLayout.CALENDAR,true); assertFalse(l.calendarSettings.containsKey(HomeLayout.CALENDAR));
        assertTrue(HomeLayout.parse(before).calendarSettings.containsKey(HomeLayout.CALENDAR));
    }
    @Test public void emptySourcesRemainEmptyAndCorruptPreferencesFallBackWithoutClampingUserInput() throws Exception {
        CalendarWidgetSettings s = settings();
        s=CalendarWidgetSettings.parse(s.json()); assertTrue(s.enabled.isEmpty()); assertTrue(snapshot().display(s,Instant.now()).days.isEmpty());
        assertEquals(30,CalendarWidgetSettings.parse(new JSONObject().put("holidayLookaheadDays",1.25)).holidayLookaheadDays);
    }
    @Test public void coverageIsIndependentAndLargeUserRangeDoesNotGenerateMillionsOfEmptyDays() throws Exception {
        CalendarWidgetSettings s=settings("thai","usa");s.holidayLookaheadDays=Integer.MAX_VALUE;
        CalendarSnapshot.Display d=snapshot().display(s,now("2026-11-16T12:00"));
        assertTrue(d.incomplete.contains("thai"));assertTrue(d.incomplete.contains("usa"));assertTrue(d.days.size()<100);
        s.holidayLookaheadDays=60;
        d=snapshot().display(s,now("2026-11-16T12:00"));
        assertTrue(d.incomplete.contains("thai")); assertFalse(d.incomplete.contains("usa"));
        assertTrue(d.offline.contains("thai"));
    }
    @Test public void chinaWorkingDaysAreOffByDefaultAndHolidayPeriodsExpandDaily() throws Exception {
        CalendarWidgetSettings s=settings("china");s.holidayLookaheadDays=4;
        CalendarSnapshot data=snapshot();
        assertEquals(Arrays.asList(1,2,3),data.display(s,now("2026-01-01T12:00")).days.stream().map(d->d.date.getDayOfMonth()).collect(Collectors.toList()));
        s.chinaWorkingDays=true;
        CalendarSnapshot.Display d=data.display(s,now("2026-01-01T12:00"));
        assertEquals(CalendarSnapshot.Kind.WORKING,d.days.get(3).events.get(0).kind);
    }
    @Test public void scopeFiltersExcludeTarragonaCityAndKeepTivissaAndSubstitutes() throws Exception {
        CalendarSnapshot data=snapshot(); CalendarWidgetSettings s=settings("spain");s.holidayLookaheadDays=2;
        assertEquals(2,data.display(s,now("2026-07-23T12:00")).days.size());
        s.tivissa=false;assertTrue(data.display(s,now("2026-07-23T12:00")).days.isEmpty());
        s=settings("thai");s.holidayLookaheadDays=3;
        assertTrue(data.display(s,now("2026-09-27T12:00")).days.isEmpty());
        s.bangkok=true;assertEquals(2,data.display(s,now("2026-09-27T12:00")).days.size());
    }
    @Test public void actualObservedDatesAreRelatedIncludingCrossYearNewYear() throws Exception {
        CalendarWidgetSettings s=settings("usa");s.holidayLookaheadDays=2;
        CalendarSnapshot.Display d=snapshot().display(s,now("2026-07-03T12:00"));
        assertEquals(2,d.days.size()); assertEquals(CalendarSnapshot.Kind.OBSERVED,d.days.get(0).events.get(0).kind);
        assertEquals(LocalDate.parse("2026-07-04"),d.days.get(0).events.get(0).anchor);
        d=snapshot().display(s,now("2027-12-31T12:00"));
        assertEquals(LocalDate.parse("2028-01-01"),d.days.get(0).events.get(0).anchor); assertTrue(d.incomplete.contains("usa"));
    }
    @Test public void sourceLocalDatesDoNotShiftHolidayMidnightIntoBangkok() throws Exception {
        CalendarWidgetSettings s=settings("spain");s.holidayLookaheadDays=1;
        CalendarSnapshot.Display d=snapshot().display(s,now("2026-12-09T01:00"));
        assertEquals(LocalDate.parse("2026-12-08"),d.days.get(0).date);
        CalendarSnapshot.Window week=CalendarSnapshot.window("flowers",s,Instant.parse("2026-11-22T17:00:00Z"),CalendarSnapshot.BANGKOK);
        assertEquals(LocalDate.parse("2026-11-23"),week.start);
    }
    @Test public void sellingBreakIsApproximateOnBothDaysAtConfirmedBoundaries() throws Exception {
        CalendarSnapshot data=snapshot();
        for(int anchor:new int[]{3,9,17,24}) for(int offset:new int[]{0,1}) {
            LocalDate day=LocalDate.of(2026,11,anchor).minusDays(offset);
            CalendarSnapshot.Event event=data.display(settings("flowers"),day,day).days.get(0).events.get(0);
            assertFalse(CalendarSnapshot.sellingBreak(event,now(day+"T10:59:59")));
            assertTrue(CalendarSnapshot.sellingBreak(event,now(day+"T11:00:00")));
            assertTrue(CalendarSnapshot.sellingBreak(event,now(day+"T15:59:59")));
            assertFalse(CalendarSnapshot.sellingBreak(event,now(day+"T16:00:00")));
        }
    }
    @Test public void badSnapshotNeverPassesValidation() throws Exception {
        for(String bad:Arrays.asList(asset().replace("\"schemaVersion\": 1","\"schemaVersion\": 99"),asset().replace("https://www.opm.gov","http://www.opm.gov"),asset().replace("\"kind\": \"HOLIDAY\"","\"kind\": \"BOGUS\""))) {
            try {CalendarSnapshot.parse(bad);fail("Invalid snapshot accepted");}catch(org.json.JSONException expected) { }
        }
    }
}
