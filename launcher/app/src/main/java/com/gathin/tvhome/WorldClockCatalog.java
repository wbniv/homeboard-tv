package com.gathin.tvhome;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;

/** Offline suggestions retain real place metadata; all named platform zones are searchable. */
final class WorldClockCatalog {
    static final class Entry {
        final String name,zone,country,code;
        final double latitude,longitude;
        Entry(String n,String z,String c,String code,double lat,double lon) { name=n;zone=z;country=c;this.code=code;latitude=lat;longitude=lon; }
        WorldClockSettings.Place place(String id) {
            WorldClockSettings.Place p=new WorldClockSettings.Place(id,name,zone);
            p.country=country; p.code=code; p.latitude=latitude; p.longitude=longitude; return p;
        }
    }
    static final List<Entry> CITIES=List.of(
        new Entry("Bangkok","Asia/Bangkok","TH","BKK",13.7563,100.5018),
        new Entry("Tivissa","Europe/Madrid","ES","",41.0409,.7331),
        new Entry("Beijing","Asia/Shanghai","CN","PEK",39.9042,116.4074),
        new Entry("New York","America/New_York","US","NYC",40.7128,-74.006),
        new Entry("Seattle","America/Los_Angeles","US","SEA",47.6062,-122.3321),
        new Entry("Denver","America/Denver","US","DEN",39.7392,-104.9903),
        new Entry("Munich","Europe/Berlin","DE","MUC",48.1351,11.582),
        new Entry("Barcelona","Europe/Madrid","ES","BCN",41.3851,2.1734),
        new Entry("London","Europe/London","GB","LON",51.5074,-.1278),
        new Entry("Tokyo","Asia/Tokyo","JP","TYO",35.6762,139.6503),
        new Entry("Sydney","Australia/Sydney","AU","SYD",-33.8688,151.2093),
        new Entry("Kathmandu","Asia/Kathmandu","NP","KTM",27.7172,85.324),
        new Entry("Kolkata","Asia/Kolkata","IN","CCU",22.5726,88.3639),
        new Entry("Los Angeles","America/Los_Angeles","US","LAX",34.0522,-118.2437),
        new Entry("Shanghai","Asia/Shanghai","CN","SHA",31.2304,121.4737),
        new Entry("Singapore","Asia/Singapore","SG","SIN",1.3521,103.8198),
        new Entry("Paris","Europe/Paris","FR","PAR",48.8566,2.3522),
        new Entry("Auckland","Pacific/Auckland","NZ","AKL",-36.8509,174.7645));
    static List<Entry> search(String text) {
        String q=text.trim().toLowerCase(Locale.ROOT).replace('_',' '); List<Entry> result=new ArrayList<>();
        for(Entry e:CITIES) if((e.name+" "+e.zone+" "+e.country+" "+e.code).toLowerCase(Locale.ROOT).replace('_',' ').contains(q)) result.add(e);
        for(String z:new java.util.TreeSet<>(ZoneId.getAvailableZoneIds())) if(z.toLowerCase(Locale.ROOT).replace('_',' ').contains(q)&&result.stream().noneMatch(e->e.zone.equals(z)))
            result.add(new Entry(z,z,"","",Double.NaN,Double.NaN));
        return result;
    }
    static void studio(WorldClockSettings s) {
        if(s.places.size()+5>WorldClockSettings.MAX_PLACES) throw new IllegalArgumentException("Too many places");
        for(int index:new int[]{0,4,5,6,7}) s.places.add(CITIES.get(index).place(java.util.UUID.randomUUID().toString()));
    }
    static JSONArray json() throws JSONException {
        JSONArray a=new JSONArray(); for(Entry e:search("")) a.put(e.place("catalog").json()); return a;
    }
}
