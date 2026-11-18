package com.gathin.tvhome;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Saved configuration only. Comparisons, ticks and selected places are transient. */
final class WorldClockSettings {
    static final String DEVICE = "device";
    static final int MAX_PLACES = 12;
    static final int DEFAULT_WIDTH=8,DEFAULT_HEIGHT=1;
    String title = "", ordering = "manual", format = "24", primary = "bkk", reference = "";
    boolean seconds;
    final List<Place> places = new ArrayList<>();

    WorldClockSettings() {
        // Five named cities; Local remains available in the city editor.
        for(int index:new int[]{0,4,5,6,7}) {
            WorldClockCatalog.Entry city=WorldClockCatalog.CITIES.get(index);
            places.add(city.place(city.code.toLowerCase(java.util.Locale.ROOT)));
        }
    }

    /** Only the captured standard preset has a redundant Local card. Custom lists stay intact. */
    void removeStandardLocal() {
        String[] zones={DEVICE,"Asia/Bangkok","America/Los_Angeles","America/Denver","Europe/Berlin","Europe/Madrid"};
        String[] labels={"","Bangkok","Seattle","Denver","Munich","Barcelona"};
        if(places.size()!=zones.length||!title.isEmpty()||!"manual".equals(ordering)||!"24".equals(format)||seconds||!reference.isEmpty()||!primary.equals(places.get(0).id))return;
        for(int i=0;i<zones.length;i++)if(!zones[i].equals(places.get(i).zone)||!labels[i].equals(places.get(i).label))return;
        places.remove(0);primary=places.get(0).id;
    }

    static final class Place {
        String id, label, zone, country = "", code = "";
        double latitude = Double.NaN, longitude = Double.NaN;
        Place(String id, String label, String zone) { this.id=id; this.label=label; this.zone=zone; }
        boolean located() { return Double.isFinite(latitude) && Double.isFinite(longitude) && Math.abs(latitude)<=90 && Math.abs(longitude)<=180; }
        ZoneId zone(ZoneId device) {
            try { return DEVICE.equals(zone) ? device : ZoneId.of(zone); }
            catch (DateTimeException invalid) { return null; }
        }
        JSONObject json() throws JSONException {
            JSONObject j=new JSONObject().put("id",id).put("label",label).put("zone",zone).put("country",country).put("code",code);
            if(located()) j.put("latitude",latitude).put("longitude",longitude);
            return j;
        }
    }

    Place place(String id) { for(Place p:places) if(p.id.equals(id)) return p; return null; }
    Place primary() { Place p=place(primary); return p==null?places.get(0):p; }
    ZoneId reference(ZoneId device) { Place p=place(reference); ZoneId z=p==null?device:p.zone(device); return z==null?device:z; }
    WorldClockSettings copy() { return parse(json()); }
    JSONObject json() {
        try {
            JSONArray list=new JSONArray(); for(Place p:places) list.put(p.json());
            return new JSONObject().put("title",title).put("ordering",ordering).put("format",format)
                    .put("primary",primary).put("reference",reference).put("seconds",seconds).put("places",list);
        } catch(JSONException invalid) { throw new IllegalStateException(invalid); }
    }
    static WorldClockSettings parse(JSONObject j) {
        WorldClockSettings s=new WorldClockSettings(); if(j==null) return s;
        s.title=j.optString("title",""); s.ordering="west".equals(j.optString("ordering"))?"west":"manual";
        s.format=j.optString("format","24"); if(!List.of("24","12","device").contains(s.format)) s.format="24";
        s.seconds=j.optBoolean("seconds",false);
        JSONArray list=j.optJSONArray("places");
        if(list!=null && list.length()>0) {
            s.places.clear(); Set<String> ids=new HashSet<>();
            for(int i=0;i<Math.min(MAX_PLACES,list.length());i++) {
                JSONObject p=list.optJSONObject(i); if(p==null) continue;
                String id=p.optString("id",""); if(id.isEmpty()||!ids.add(id)) continue;
                Place place=new Place(id,p.optString("label",""),p.optString("zone",""));
                place.country=p.optString("country",""); place.code=p.optString("code","");
                place.latitude=p.optDouble("latitude",Double.NaN); place.longitude=p.optDouble("longitude",Double.NaN);
                if(!place.located()) { place.latitude=Double.NaN; place.longitude=Double.NaN; }
                s.places.add(place);
            }
            if(s.places.isEmpty()) s.places.add(new Place("local","",DEVICE));
        }
        s.primary=j.optString("primary",s.places.get(0).id); if(s.place(s.primary)==null) s.primary=s.places.get(0).id;
        s.reference=j.optString("reference",""); if(!s.reference.isEmpty()&&s.place(s.reference)==null) s.reference="";
        return s;
    }

    /** Phone/native drafts are strict; persisted unknown zones remain visibly unavailable. */
    static void validate(JSONObject j) throws JSONException {
        PhoneConfiguration.keys(j,"world_clock","title","ordering","format","primary","reference","seconds","places");
        PhoneConfiguration.bounded(j,"title",120);
        if(!List.of("manual","west").contains(j.getString("ordering")) || !List.of("24","12","device").contains(j.getString("format"))) invalid();
        if(!(j.get("seconds") instanceof Boolean)) invalid();
        JSONArray places=j.getJSONArray("places"); if(places.length()<1||places.length()>MAX_PLACES) invalid();
        Set<String> ids=new HashSet<>();
        for(int i=0;i<places.length();i++) {
            JSONObject p=places.getJSONObject(i);
            PhoneConfiguration.keys(p,"place","id","label","zone","country","code","latitude","longitude");
            String id=PhoneConfiguration.bounded(p,"id",48),zone=PhoneConfiguration.bounded(p,"zone",120);
            if(!id.matches("[A-Za-z0-9_-]+")||!ids.add(id)) invalid();
            PhoneConfiguration.bounded(p,"label",120); PhoneConfiguration.bounded(p,"country",80); PhoneConfiguration.bounded(p,"code",8);
            if(!DEVICE.equals(zone)) { try { ZoneId.of(zone); } catch(DateTimeException bad) { invalid(); } }
            if(p.has("latitude")||p.has("longitude")) {
                double lat=p.getDouble("latitude"),lon=p.getDouble("longitude");
                if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180) invalid();
            }
        }
        if(!ids.contains(j.getString("primary"))||!j.getString("reference").isEmpty()&&!ids.contains(j.getString("reference"))) invalid();
    }
    private static void invalid() { throw new PhoneConfiguration.Invalid("world_clock","Invalid clock settings or time zone"); }
}
