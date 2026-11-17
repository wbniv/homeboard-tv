package com.gathin.tvhome;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/** Public station positions and individual observations; never inferred model locations. */
final class StationCatalog {
    static final class Station {
        final String id,name,area;
        final double latitude,longitude,pm25;
        final long time;
        Station(String id,String name,String area,double latitude,double longitude,double pm25,long time){this.id=id;this.name=name;this.area=area;this.latitude=latitude;this.longitude=longitude;this.pm25=pm25;this.time=time;}
        AirQualityReading reading(){return Double.isFinite(pm25)&&time>0?new AirQualityReading(pm25,time):null;}
    }
    static List<Station> parse(String body,long now)throws JSONException{
        JSONObject root=new JSONObject(body);if(!"Success".equals(root.optString("status")))throw new JSONException("Invalid feed");
        JSONArray rows=root.getJSONArray("message");if(rows.length()>5000)throw new JSONException("Too many stations");
        List<Station> result=new ArrayList<>();HashSet<String> ids=new HashSet<>();
        for(int i=0;i<rows.length();i++){
            JSONObject row=rows.optJSONObject(i);if(row==null)continue;String id=row.optString("MeasIndex","");
            double lat=row.optDouble("Lat",Double.NaN),lon=row.optDouble("Long",Double.NaN),value=row.optDouble("PM2.5",Double.NaN);
            if(id.isEmpty()||id.length()>80||!RainSettings.coordinates(lat,lon)||!ids.add(id))continue;
            if(!Double.isFinite(value)||value<0)value=Double.NaN;long time=0;
            try{time=LocalDateTime.parse(row.optString("DateTime"),AirQualityReading.SOURCE_TIME).atZone(AirQualityReading.BANGKOK).toInstant().toEpochMilli();}catch(RuntimeException ignored){}
            if(time>now+600_000||time<=0){time=0;value=Double.NaN;}
            result.add(new Station(id,row.optString("Area",id),row.optString("District",""),lat,lon,value,time));
        }return result;
    }
    static double distanceKm(double lat,double lon,double otherLat,double otherLon){
        double a=Math.toRadians(otherLat-lat),b=Math.toRadians(otherLon-lon);
        double q=Math.sin(a/2)*Math.sin(a/2)+Math.cos(Math.toRadians(lat))*Math.cos(Math.toRadians(otherLat))*Math.sin(b/2)*Math.sin(b/2);
        return 6371.0088*2*Math.asin(Math.sqrt(Math.max(0,Math.min(1,q))));
    }
    static List<Station> nearby(List<Station> all,double lat,double lon,double radius){
        List<Station> result=new ArrayList<>();if(!RainSettings.coordinates(lat,lon))return result;
        for(Station s:all)if(distanceKm(lat,lon,s.latitude,s.longitude)<=radius)result.add(s);
        result.sort(Comparator.comparingDouble(s->distanceKm(lat,lon,s.latitude,s.longitude)));return result;
    }
}
