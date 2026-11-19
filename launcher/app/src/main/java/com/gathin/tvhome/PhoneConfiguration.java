package com.gathin.tvhome;

import org.json.*;
import java.util.*;

/** Typed widget-only protocol. Never accepts arbitrary preference or system changes. */
final class PhoneConfiguration {
    static final Set<String> STATIONS=Set.of("114","119","122","70","65");
    static final class Invalid extends IllegalArgumentException {
        final String field;
        Invalid(String field,String message){super(message);this.field=field;}
    }
    static WidgetKind kind(String key){try{return WidgetKind.of(key);}catch(IllegalArgumentException e){return null;}}
    static JSONObject snapshot(HomeLayout layout,JSONObject air,int columns,int rows)throws JSONException {
        JSONArray widgets=new JSONArray();
        for(HomeLayout.Cell cell:layout.cells(layout.widgets,columns,rows)) {
            String key=cell.key;WidgetKind kind=kind(key);
            JSONObject settings=kind==WidgetKind.GROCERY?new JSONObject().put("listId",layout.groceryLists.getOrDefault(key,GroceryData.DEFAULT)):kind==WidgetKind.RAIN?layout.rainSettings.get(key).json():kind==WidgetKind.CALENDAR?layout.calendarSettings.get(key).json():kind==WidgetKind.WORLD_CLOCK?layout.clockSettings.get(key).json():kind==WidgetKind.AIR?air.optJSONObject(key):new JSONObject();
            if(settings==null)settings=defaultAir();
            widgets.put(new JSONObject().put("key",key).put("type",kind==null?"app":kind.name().toLowerCase(Locale.ROOT))
                    .put("page",cell.page).put("column",cell.column).put("row",cell.row).put("width",cell.width).put("height",cell.height).put("settings",settings));
        }
        return new JSONObject().put("widgets",widgets).put("favorites",new JSONArray(layout.favorites))
                .put("groceries",GroceryData.copy(layout.groceries)).put("headerClock",layout.headerClock.json()).put("columns",columns).put("rows",rows).put("artwork",layout.homeAppSize);
    }
    static JSONObject defaultAir()throws JSONException{return new JSONObject().put("source","model").put("location-mode","auto").put("location-name","").put("station","114");}
    static final class Result {final HomeLayout layout;final JSONObject air;Result(HomeLayout l,JSONObject a){layout=l;air=a;}}
    static Result validate(JSONObject draft,HomeLayout before,JSONObject beforeAir,Set<String> available)throws JSONException {
        keys(draft,"draft","widgets","favorites","columns","rows","artwork","headerClock","groceries");
        int columns=integer(draft,"columns",4,12),rows=integer(draft,"rows",2,12);
        String artwork=choice(draft,"artwork","small","banner");
        HomeLayout result=HomeLayout.parse(before.encode());
        GroceryData.validate(draft.getJSONObject("groceries"));
        result.groceries=GroceryData.copy(draft.getJSONObject("groceries"));
        WorldClockSettings.validate(draft.getJSONObject("headerClock"));
        result.headerClock=WorldClockSettings.parse(draft.getJSONObject("headerClock"));
        result.widgets.clear();result.homePositions.clear();result.panelSpans.clear();
        result.homeColumns=columns;result.homeRows=rows;result.homeAppSize=artwork;result.favoritesContainer=true;
        JSONObject air=new JSONObject(beforeAir.toString());
        JSONArray widgets=draft.getJSONArray("widgets");if(widgets.length()>32)throw new Invalid("widgets","At most 32 widgets");
        Set<String> seen=new HashSet<>(),occupied=new HashSet<>();
        for(int i=0;i<widgets.length();i++) {
            JSONObject w=widgets.getJSONObject(i);keys(w,"widgets","key","type","page","column","row","width","height","settings");
            String key=bounded(w,"key",180),type=w.getString("type");
            WidgetKind kind=kind(key);
            if(!seen.add(key))throw new Invalid("widgets","Duplicate widget instance");
            if(kind==null) {if(!before.widgets.contains(key)||!key.startsWith("app:")||!"app".equals(type))throw new Invalid("widgets","Unknown widget");}
            else if(!kind.name().toLowerCase(Locale.ROOT).equals(type)||!key.matches("widget:(air|rain|calendar|favorites|world_clock|grocery|timer)(:[1-9][0-9]{0,5})?"))throw new Invalid("widgets","Invalid widget identity");
            if(kind==WidgetKind.TIMER&&!HomeLayout.TIMER.equals(key)||kind==WidgetKind.CALENDAR&&!HomeLayout.CALENDAR.equals(key)||kind==WidgetKind.FAVORITES&&!HomeLayout.FAVORITES.equals(key))throw new Invalid("widgets","This widget has one instance");
            int page=integer(w,"page",0,15),x=integer(w,"column",0,columns-1),y=integer(w,"row",0,rows-1),width=integer(w,"width",1,columns),height=integer(w,"height",1,rows);
            if(kind==WidgetKind.CALENDAR&&width<2)throw new Invalid(key,"Calendar needs at least two columns");
            if(kind==null&&(height!=1||width!=(HomeLayout.APP_BANNER.equals(artwork)?2:1)))throw new Invalid(key,"App shortcut size must match its artwork");
            if(x+width>columns||y+height>rows)throw new Invalid(key,"Widget extends outside the grid");
            for(int yy=y;yy<y+height;yy++)for(int xx=x;xx<x+width;xx++)if(!occupied.add(page+":"+xx+":"+yy))throw new Invalid(key,"Widgets overlap; adjust position or size");
            JSONObject settings=w.getJSONObject("settings");
            if(kind==WidgetKind.GROCERY){keys(settings,"groceries","listId");String listId=bounded(settings,"listId",100);if(!result.groceries.has(listId))throw new Invalid("groceries","List unavailable");result.groceryLists.put(key,listId);}
            else if(kind==WidgetKind.RAIN){validateRain(settings);result.rainSettings.put(key,RainSettings.parse(settings));}
            else if(kind==WidgetKind.CALENDAR){validateCalendar(settings);result.calendarSettings.put(key,CalendarWidgetSettings.parse(settings));}
            else if(kind==WidgetKind.AIR){validateAir(settings);air.put(key,settings);}
            else if(kind==WidgetKind.WORLD_CLOCK){WorldClockSettings.validate(settings);result.clockSettings.put(key,WorldClockSettings.parse(settings));}
            else keys(settings,key);
            result.widgets.add(key);result.setPanelSpan(key,width,height);result.homePositions.put(key,new HomeLayout.HomePosition(page,y,x));
        }
        result.rainSettings.keySet().retainAll(seen);result.calendarSettings.keySet().retainAll(seen);
        result.clockSettings.keySet().retainAll(seen);
        JSONArray favorites=draft.getJSONArray("favorites");if(favorites.length()>256)throw new Invalid("favorites","Too many favorites");
        result.favorites.clear();
        for(int i=0;i<favorites.length();i++){
            String key=favorites.getString(i);
            if(!available.contains(key)&&!before.favorites.contains(key))throw new Invalid("favorites","App is unavailable on this TV");
            if(result.favorites.contains(key))throw new Invalid("favorites","Duplicate favorite");result.favorites.add(key);
        }
        return new Result(result,air);
    }
    private static void validateRain(JSONObject s)throws JSONException {
        keys(s,"rain","locationSource","name","label","hours","days","inches","animation","latitude","longitude");
        String source=choice(s,"locationSource","device","fixed");bounded(s,"name",240);bounded(s,"label",120);
        int hours=integer(s,"hours",6,48);if(hours!=6&&hours!=12&&hours!=24&&hours!=48)throw new Invalid("hours","Choose 6, 12, 24 or 48 hours");
        integer(s,"days",0,6);bool(s,"inches");bool(s,"animation");coordinates(s,"fixed".equals(source));
    }
    private static void validateCalendar(JSONObject s)throws JSONException {
        keys(s,"calendar","holidayLookaheadDays","enabled","flowerWeeks","buddhistWeeks","bangkok","thaiNames","chinaWorkingDays","spanishNational","catalunya","tarragona","tivissa");
        integer(s,"holidayLookaheadDays",1,Integer.MAX_VALUE);choice(s,"flowerWeeks","THIS","NEXT","BOTH");choice(s,"buddhistWeeks","THIS","NEXT","BOTH");
        JSONArray enabled=s.getJSONArray("enabled");Set<String> seen=new HashSet<>();for(int i=0;i<enabled.length();i++){String k=enabled.getString(i);if(!Arrays.asList(CalendarWidgetSettings.SOURCES).contains(k)||!seen.add(k))throw new Invalid("enabled","Invalid Calendar source");}
        for(String k:List.of("bangkok","thaiNames","chinaWorkingDays","spanishNational","catalunya","tarragona","tivissa"))bool(s,k);
    }
    private static void validateAir(JSONObject s)throws JSONException {
        keys(s,"air","source","location-mode","location-name","station","latitude","longitude");
        String source=choice(s,"source","model","airbkk"),mode=choice(s,"location-mode","auto","manual");
        if(!STATIONS.contains(s.getString("station")))throw new Invalid("station","Unknown station");bounded(s,"location-name",240);coordinates(s,"model".equals(source)&&"manual".equals(mode));
    }
    private static void coordinates(JSONObject s,boolean required)throws JSONException {
        if(required||s.has("latitude")||s.has("longitude"))if(!RainSettings.coordinates(s.getDouble("latitude"),s.getDouble("longitude")))throw new Invalid("location","Valid latitude and longitude required");
    }
    static int integer(JSONObject s,String key,int min,int max)throws JSONException {
        Object value=s.get(key);if(!(value instanceof Number)||!Double.isFinite(((Number)value).doubleValue())||((Number)value).doubleValue()!=((Number)value).longValue())throw new Invalid(key,"Whole number required");
        long n=((Number)value).longValue();if(n<min||n>max)throw new Invalid(key,"Value outside allowed range");return(int)n;
    }
    static String bounded(JSONObject s,String key,int max)throws JSONException {Object value=s.get(key);if(!(value instanceof String)||((String)value).length()>max)throw new Invalid(key,"Text is too long or invalid");return(String)value;}
    private static String choice(JSONObject s,String key,String...values)throws JSONException {String v=s.getString(key);if(!Arrays.asList(values).contains(v))throw new Invalid(key,"Unsupported choice");return v;}
    private static void bool(JSONObject s,String key)throws JSONException {if(!(s.get(key) instanceof Boolean))throw new Invalid(key,"Boolean required");}
    static void keys(JSONObject s,String field,String...allowed){Set<String> valid=new HashSet<>(Arrays.asList(allowed));for(Iterator<String> i=s.keys();i.hasNext();)if(!valid.contains(i.next()))throw new Invalid(field,"Unknown setting");}
}
