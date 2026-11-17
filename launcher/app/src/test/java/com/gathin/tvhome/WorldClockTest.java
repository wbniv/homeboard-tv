package com.gathin.tvhome;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class WorldClockTest {
    private static final Instant NOW=Instant.parse("2026-11-17T10:30:00Z");
    @Test public void convertsTheSameInstantAndHandlesDateLineHalfAndQuarterHours() {
        assertEquals("17:30",WorldClockModel.time(NOW,ZoneId.of("Asia/Bangkok"),Locale.US,false,false));
        assertEquals("4:15 PM",WorldClockModel.time(NOW,ZoneId.of("Asia/Kathmandu"),Locale.US,true,false));
        assertEquals("4:15 PM",WorldClockModel.time(NOW,ZoneId.of("Asia/Kathmandu"),Locale.forLanguageTag("th"),true,false));
        assertEquals(345,WorldClockModel.differenceMinutes(NOW,ZoneId.of("Asia/Kathmandu"),ZoneOffset.UTC));
        assertEquals(825,WorldClockModel.differenceMinutes(NOW,ZoneId.of("Pacific/Chatham"),ZoneOffset.UTC));
        assertEquals(1,WorldClockModel.dayDifference(NOW,ZoneId.of("Pacific/Kiritimati"),ZoneId.of("Pacific/Honolulu")));
        WorldClockModel model=new WorldClockModel(Clock.fixed(NOW,ZoneOffset.UTC));assertEquals(NOW,model.now());
    }
    @Test public void springGapIsNotInventedAndAutumnRepeatedHourKeepsDistinctOffsets() {
        ZoneId ny=ZoneId.of("America/New_York");Instant spring=Instant.parse("2026-03-08T06:30:00Z");
        assertEquals("01:30",WorldClockModel.time(spring,ny,Locale.US,false,false));
        assertEquals("03:30",WorldClockModel.time(spring.plusSeconds(3600),ny,Locale.US,false,false));
        Instant autumn=Instant.parse("2026-11-01T05:30:00Z");
        assertEquals("01:30",WorldClockModel.time(autumn,ny,Locale.US,false,false));
        assertEquals("01:30",WorldClockModel.time(autumn.plusSeconds(3600),ny,Locale.US,false,false));
        assertEquals("UTC-04:00",WorldClockModel.offset(autumn,ny));
        assertEquals("UTC-05:00",WorldClockModel.offset(autumn.plusSeconds(3600),ny));
        assertEquals(3600,Duration.between(autumn,autumn.plusSeconds(3600)).getSeconds());
    }
    @Test public void comparisonUsesReferenceHourBoundariesRatherThanAssumingWholeHourZones() {
        Instant at=Instant.parse("2026-11-17T10:32:00Z");ZoneId ktm=ZoneId.of("Asia/Kathmandu");
        assertEquals(Instant.parse("2026-11-17T10:15:00Z"),WorldClockModel.windowStart(at,ktm));
        assertEquals(Instant.parse("2026-11-17T10:45:00Z"),WorldClockModel.nextHalfHour(at,ktm));
        assertEquals(WorldClockModel.windowStart(at,ktm),WorldClockModel.pageStart(at,ktm,-1));
        assertEquals(86400,Duration.between(WorldClockModel.pageStart(at,ktm,0),WorldClockModel.pageStart(at,ktm,1)).getSeconds());
    }
    @Test public void intervalOverlayHasSubHourPrecisionAndClipsAtBothEdges() {
        assertArrayEquals(new double[]{.5,30/1440d},WorldClockModel.overlay(NOW,NOW.plusSeconds(43200),30),1e-9);
        assertArrayEquals(new double[]{0,45/1440d},WorldClockModel.overlay(NOW,NOW.minusSeconds(900),60),1e-9);
        assertArrayEquals(new double[]{1,0},WorldClockModel.overlay(NOW,NOW.plusSeconds(90000),60),1e-9);
        assertArrayEquals(new double[]{0,0},WorldClockModel.overlay(NOW,NOW.minusSeconds(7200),30),1e-9);
    }
    @Test public void cityColorsAreSharedAndDoNotChangeWhenSavedOrderChanges() {
        WorldClockSettings s=new WorldClockSettings();s.places.add(WorldClockCatalog.CITIES.get(0).place("bkk"));s.places.add(WorldClockCatalog.CITIES.get(3).place("ny"));
        Map<String,Float> before=WorldClockModel.hues(s);assertEquals(0f,before.get("ny"),0);assertEquals(300f,before.get("bkk"),0);assertFalse(before.containsKey("local"));
        java.util.Collections.reverse(s.places);assertEquals(before,WorldClockModel.hues(s));
        s.ordering="west";List<WorldClockSettings.Place> sorted=WorldClockModel.ordered(s);
        assertEquals("ny",sorted.get(0).id);assertEquals("bkk",sorted.get(1).id);assertEquals("local",sorted.get(2).id);
    }
    @Test public void settingsRoundTripPreservesNamesAndIndependentReferenceAcrossReordering()throws Exception {
        WorldClockSettings s=new WorldClockSettings();s.places.add(WorldClockCatalog.CITIES.get(1).place("tivissa"));
        s.primary="tivissa";s.reference="local";s.title="Family";s.format="12";s.seconds=true;
        WorldClockSettings copy=s.copy();java.util.Collections.reverse(copy.places);
        assertEquals("tivissa",copy.primary().id);assertEquals("local",copy.reference);assertEquals("Family",copy.title);assertTrue(copy.seconds);
        copy.primary().label="A person";assertEquals("Tivissa",s.primary().label);assertEquals("Europe/Madrid",copy.primary().zone);
        assertEquals("",s.primary().code);WorldClockSettings.validate(s.json());
    }
    @Test public void unavailablePersistedZoneIsKeptVisibleButRejectedForNewDrafts()throws Exception {
        JSONObject json=new WorldClockSettings().json();json.getJSONArray("places").getJSONObject(0).put("zone","Invalid/Place").put("label","Retain me");
        WorldClockSettings kept=WorldClockSettings.parse(json);assertEquals("Retain me",kept.primary().label);assertNull(kept.primary().zone(ZoneOffset.UTC));
        assertThrows(PhoneConfiguration.Invalid.class,()->WorldClockSettings.validate(json));
    }
    @Test public void strictDraftValidationRejectsIdentityOverflowCoordinatesAndUnknownSettings()throws Exception {
        JSONObject unknown=new WorldClockSettings().json().put("systemClock",true);
        assertThrows(PhoneConfiguration.Invalid.class,()->WorldClockSettings.validate(unknown));
        JSONObject duplicate=new WorldClockSettings().json();duplicate.getJSONArray("places").put(duplicate.getJSONArray("places").getJSONObject(0));
        assertThrows(PhoneConfiguration.Invalid.class,()->WorldClockSettings.validate(duplicate));
        JSONObject bad=new WorldClockSettings().json();bad.getJSONArray("places").getJSONObject(0).put("latitude",91).put("longitude",0);
        assertThrows(PhoneConfiguration.Invalid.class,()->WorldClockSettings.validate(bad));
        JSONObject empty=new WorldClockSettings().json().put("places",new JSONArray());assertThrows(PhoneConfiguration.Invalid.class,()->WorldClockSettings.validate(empty));
        JSONObject missing=new WorldClockSettings().json().put("primary","absent");assertThrows(PhoneConfiguration.Invalid.class,()->WorldClockSettings.validate(missing));
        JSONObject crowded=new WorldClockSettings().json();for(int i=1;i<13;i++)crowded.getJSONArray("places").put(new WorldClockSettings.Place("p"+i,"","device").json());
        assertThrows(PhoneConfiguration.Invalid.class,()->WorldClockSettings.validate(crowded));
    }
    @Test public void localZoneFollowsDeviceWhileFixedZonesRemainFixed() {
        WorldClockSettings s=new WorldClockSettings();assertEquals(ZoneId.of("Asia/Bangkok"),s.primary().zone(ZoneId.of("Asia/Bangkok")));
        assertEquals(ZoneId.of("Europe/Madrid"),s.primary().zone(ZoneId.of("Europe/Madrid")));
        WorldClockSettings.Place fixed=WorldClockCatalog.CITIES.get(0).place("fixed");assertEquals(ZoneId.of("Asia/Bangkok"),fixed.zone(ZoneId.of("Europe/Madrid")));
    }
    @Test public void multipleWidgetInstancesAndPhoneEditsPreserveOtherSettingsAndRemovalPrunesOnlyThatInstance()throws Exception {
        HomeLayout home=HomeLayout.firstRun();home.palette="forest";home.addPanel(HomeLayout.WORLD_CLOCK);home.addPanel(HomeLayout.WORLD_CLOCK+":2");
        home.clockSettings.get(HomeLayout.WORLD_CLOCK).title="Home";home.clockSettings.get(HomeLayout.WORLD_CLOCK+":2").title="Work";
        JSONObject air=new JSONObject().put(HomeLayout.AIR,PhoneConfiguration.defaultAir());
        JSONObject draft=PhoneConfiguration.snapshot(home,air,8,4);
        for(int i=0;i<draft.getJSONArray("widgets").length();i++){JSONObject w=draft.getJSONArray("widgets").getJSONObject(i);if(w.getString("key").equals(HomeLayout.WORLD_CLOCK))w.getJSONObject("settings").put("title","Changed");}
        PhoneConfiguration.Result result=PhoneConfiguration.validate(draft,home,air,Set.of());
        assertEquals("Changed",result.layout.clockSettings.get(HomeLayout.WORLD_CLOCK).title);assertEquals("Home",home.clockSettings.get(HomeLayout.WORLD_CLOCK).title);
        assertEquals("Work",result.layout.clockSettings.get(HomeLayout.WORLD_CLOCK+":2").title);assertEquals("forest",result.layout.palette);
        result.layout.delete(HomeLayout.WORLD_CLOCK,true);assertFalse(result.layout.clockSettings.containsKey(HomeLayout.WORLD_CLOCK));assertTrue(result.layout.clockSettings.containsKey(HomeLayout.WORLD_CLOCK+":2"));
        assertEquals("Home",HomeLayout.parse(home.encode()).clockSettings.get(HomeLayout.WORLD_CLOCK).title);
        assertEquals(WidgetKind.WORLD_CLOCK,WidgetKind.of(HomeLayout.WORLD_CLOCK+":2"));
    }
    @Test public void catalogSearchIncludesCitiesCodesAndEveryPlatformZoneWithoutGuessingCoordinates() {
        assertTrue(WorldClockCatalog.search("Tivissa").stream().anyMatch(e->e.zone.equals("Europe/Madrid")));
        assertTrue(WorldClockCatalog.search("SEA").stream().anyMatch(e->e.name.equals("Seattle")));
        WorldClockCatalog.Entry zone=WorldClockCatalog.search("America/Detroit").get(0);assertFalse(zone.place("zone").located());
        WorldClockSettings settings=new WorldClockSettings();WorldClockCatalog.studio(settings);assertEquals(6,settings.places.size());
        for(WorldClockCatalog.Entry e:WorldClockCatalog.CITIES)assertNotNull(ZoneId.of(e.zone));
    }
    @Test public void daytimeMeansExplicitClockHoursAndMapProjectionUsesRealCoordinates()throws Exception {
        assertFalse(WorldClockModel.daytime(Instant.parse("2026-11-17T05:59:00Z"),ZoneOffset.UTC));
        assertTrue(WorldClockModel.daytime(Instant.parse("2026-11-17T06:00:00Z"),ZoneOffset.UTC));
        assertFalse(WorldClockModel.daytime(Instant.parse("2026-11-17T18:00:00Z"),ZoneOffset.UTC));
        assertEquals(0,WorldClockModel.mapX(-180),0);assertEquals(1,WorldClockModel.mapX(180),0);
        assertEquals(0,WorldClockModel.mapY(90),0);assertEquals(1,WorldClockModel.mapY(-90),0);
        JSONArray rings=new JSONArray(new String(Files.readAllBytes(Path.of("src/main/assets/world-clock-land.json")),java.nio.charset.StandardCharsets.UTF_8));
        assertTrue(rings.length()>100);for(int i=0;i<rings.length();i++)for(int j=0;j<rings.getJSONArray(i).length();j++){
            JSONArray point=rings.getJSONArray(i).getJSONArray(j);assertTrue(Math.abs(point.getDouble(0))<=180.001);assertTrue(Math.abs(point.getDouble(1))<=90.001);
        }
    }
}
