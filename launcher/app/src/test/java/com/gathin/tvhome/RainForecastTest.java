package com.gathin.tvhome;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import org.junit.Test;
import java.time.*;
import java.util.List;
import static org.junit.Assert.*;

public class RainForecastTest {
    private JSONObject feed(String zone, Instant start, int count) throws Exception {
        JSONArray times=new JSONArray(),rain=new JSONArray(),showers=new JSONArray(),chance=new JSONArray(),snow=new JSONArray();
        for(int i=0;i<count;i++){times.put(start.getEpochSecond()+3600L*(i+1));rain.put(1);showers.put(.5);chance.put(i%2==0?20:80);snow.put(0);}
        return new JSONObject().put("timezone",zone).put("hourly_units",new JSONObject().put("time","unixtime").put("rain","mm").put("showers","mm").put("precipitation_probability","%").put("snowfall","cm"))
                .put("hourly",new JSONObject().put("time",times).put("rain",rain).put("showers",showers).put("precipitation_probability",chance).put("snowfall",snow));
    }
    @Test public void providerTimestampsEndIntervalsAndAmountsExcludeSnow() throws Exception {
        Instant start=Instant.parse("2026-11-16T00:00:00Z");JSONObject j=feed("Asia/Bangkok",start,240);
        j.getJSONObject("hourly").getJSONArray("snowfall").put(1,2);
        RainForecast f=RainForecast.parse(j.toString());RainForecast.Period p=f.hourly(start.minusSeconds(1),24);
        assertEquals(start.getEpochSecond(),p.start);assertEquals(36,p.mm,0.00001);assertEquals(80,p.chance,0);assertEquals(2,p.snowCm,0);
        assertEquals(start.getEpochSecond(),p.firstWet().start);
    }
    @Test public void hourlyThenSixDaysHaveNoOverlapAndFirstDayIsPartial() throws Exception {
        Instant start=Instant.parse("2026-11-16T00:00:00Z");RainForecast f=RainForecast.parse(feed("Asia/Bangkok",start,240).toString());
        Instant now=start.plusSeconds(15*60);RainForecast.Period p=f.hourly(now,24);List<RainForecast.Period> days=f.following(now,24,6);
        assertEquals(6,days.size());assertEquals(p.end,days.get(0).start);
        assertEquals(LocalTime.of(8,0),Instant.ofEpochSecond(days.get(0).start).atZone(f.zone).toLocalTime());
        assertEquals(16,days.get(0).hours.size());assertEquals(24,days.get(1).hours.size());
        for(int i=1;i<days.size();i++)assertEquals(days.get(i-1).end,days.get(i).start);
        assertEquals(0,f.following(now,6,0).size());
    }
    @Test public void nullOrOutOfRangeIsUnknownRatherThanDry() throws Exception {
        Instant start=Instant.parse("2026-11-16T00:00:00Z");JSONObject j=feed("UTC",start,48);
        j.getJSONObject("hourly").getJSONArray("rain").put(2,JSONObject.NULL);
        j.getJSONObject("hourly").getJSONArray("precipitation_probability").put(3,101);
        RainForecast.Period p=RainForecast.parse(j.toString()).hourly(start.minusSeconds(1),24);
        assertFalse(p.complete);assertTrue(Double.isNaN(p.mm));assertTrue(Double.isNaN(p.chance));assertFalse(p.hours.get(2).valid());assertFalse(p.hours.get(3).valid());
    }
    @Test public void expiredIntervalsRemainUnavailable() throws Exception {
        Instant start=Instant.parse("2026-11-16T00:00:00Z");RainForecast f=RainForecast.parse(feed("UTC",start,24).toString());
        RainForecast.Period p=f.hourly(start.plusSeconds(48*3600),24);assertFalse(p.complete);assertNull(p.firstWet());
    }
    @Test public void daylightSavingDailyRowsUseRealTwentyThreeOrTwentyFiveHours() throws Exception {
        for(String date:new String[]{"2026-03-27T23:00:00Z","2026-10-23T22:00:00Z"}) {
            Instant start=Instant.parse(date);RainForecast f=RainForecast.parse(feed("Europe/Berlin",start,240).toString());
            List<RainForecast.Period> periods=f.following(start.minusSeconds(1),24,6);
            assertEquals(date.contains("03-27")?23:25,periods.get(0).hours.size());
        }
    }
    @Test public void halfHourZoneUsesLocalWholeHour() throws Exception {
        Instant start=Instant.parse("2026-11-16T00:30:00Z");RainForecast f=RainForecast.parse(feed("Asia/Kolkata",start,48).toString());
        assertEquals(start.getEpochSecond(),f.windowStart(start.minusSeconds(1)));
    }
    @Test public void rejectsUnitsMismatchedArraysAndDuplicateTimes() throws Exception {
        Instant start=Instant.parse("2026-11-16T00:00:00Z");JSONObject j=feed("UTC",start,48);j.getJSONObject("hourly_units").put("rain","inch");
        assertThrows(JSONException.class,()->RainForecast.parse(j.toString()));
        JSONObject k=feed("UTC",start,48);k.getJSONObject("hourly").getJSONArray("time").put(1,start.getEpochSecond()+3600);
        assertThrows(JSONException.class,()->RainForecast.parse(k.toString()));
        JSONObject l=feed("UTC",start,48);l.getJSONObject("hourly").put("rain",new JSONArray());
        assertThrows(JSONException.class,()->RainForecast.parse(l.toString()));
    }
    @Test public void settingsAndHomeUndoSnapshotPreserveIndependentInstances() throws Exception {
        HomeLayout home=HomeLayout.firstRun();home.addPanel(HomeLayout.RAIN);home.addPanel(HomeLayout.RAIN+":2");
        RainSettings a=home.rainSettings.get(HomeLayout.RAIN);a.latitude=13.7;a.longitude=100.5;a.hours=48;a.days=3;a.animation=false;a.inches=true;a.name="Example";
        HomeLayout copy=HomeLayout.parse(home.encode());RainSettings b=copy.rainSettings.get(HomeLayout.RAIN);
        assertTrue(b.located());assertEquals(48,b.hours);assertEquals(3,b.days);assertTrue(b.inches);assertFalse(b.animation);
        assertFalse(copy.rainSettings.get(HomeLayout.RAIN+":2").located());assertEquals(4,copy.panelSpan(HomeLayout.RAIN).width);assertEquals(3,copy.panelSpan(HomeLayout.RAIN).height);
        copy.delete(HomeLayout.RAIN,true);assertFalse(copy.rainSettings.containsKey(HomeLayout.RAIN));assertTrue(home.rainSettings.containsKey(HomeLayout.RAIN));
        assertFalse(RainSettings.coordinates(Double.NaN,0));assertFalse(RainSettings.coordinates(0,181));assertTrue(RainSettings.coordinates(-90,-180));
    }
}
