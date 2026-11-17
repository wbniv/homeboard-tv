package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

final class AirExpanded implements WidgetExpansion.Content {
    private final WidgetExpansion h;
    private final AirQualityCard card;
    private final Consumer<Consumer<Location>> locate;
    private final LinearLayout root,body,stationList;
    private final TextView status,detail;
    private StationMapView map;
    private final Handler handler=new Handler();
    private final android.content.SharedPreferences cache;
    private List<StationCatalog.Station> all=new ArrayList<>(),nearby=new ArrayList<>();
    private StationCatalog.Station selected;
    private StationCatalog.Station historyStation;
    private Location actual;
    private int radius,range;
    private boolean history,model,closed,running,locating,loading,catalogFailed;
    private int historyGeneration;
    private long selectedTime;
    private List<AirQualityReading> samples=new ArrayList<>();
    private TextView historyDetail;
    private final java.util.LinkedHashMap<String,List<AirQualityReading>> histories=new java.util.LinkedHashMap<>();
    private String selectedId;
    private final Runnable tick=new Runnable(){public void run(){if(running&&!closed){refreshCatalog();handler.postDelayed(this,300_000);}}};
    AirExpanded(WidgetExpansion host,AirQualityCard card,Consumer<Consumer<Location>> locate,Bundle state){
        h=host;this.card=card;this.locate=locate;radius=state.getInt("radius",25);if(radius!=10&&radius!=25&&radius!=50&&radius!=100)radius=25;
        range=state.getInt("range",7);selectedId=state.getString("station","");selectedTime=state.getLong("sample",0);model=state.getBoolean("model",false);
        cache=h.activity.getSharedPreferences("air-expanded-cache",0);root=h.column();LinearLayout tabs=h.row();
        tabs.addView(h.button(R.string.hb_fs_nearby,()->showMap()));tabs.addView(h.button(R.string.hb_fs_history,()->showHistory(false)));
        if(card.expandedIsModel())tabs.addView(h.button(R.string.hb_fs_model,()->showHistory(true)));
        tabs.addView(h.button(R.string.hb_fs_locate,this::requestLocation));
        for(int km:new int[]{10,25,50,100})tabs.addView(h.button(km+" km",()->{if(history)showMap();radius=km;updateStations();if(actual!=null)map.locate(actual,radius);}));root.addView(tabs);
        status=h.text("",13);root.addView(status);body=h.column();root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        map=new StationMapView(h,this::select,()->{if(selected!=null)h.details(selected.name,describe(selected));});
        stationList=h.column();detail=h.text("",15);
        try{String saved=cache.getString("feed",card.expandedCachedFeed());all=StationCatalog.parse(saved,System.currentTimeMillis());}catch(Exception ignored){}
        showMap();if(state.getBoolean("history",false))showHistory(model);
    }
    private String t(int id){return h.activity.getString(id);}
    public View view(){return root;}
    public Bundle state(){Bundle b=new Bundle();b.putInt("radius",radius);b.putInt("range",range);b.putString("station",selectedId);b.putBoolean("history",history);b.putBoolean("model",model);b.putLong("sample",selectedTime);return b;}
    public void active(boolean value){running=value;map.active(value);handler.removeCallbacks(tick);if(value){if(actual==null)requestLocation();tick.run();}}
    public void close(){closed=true;active(false);historyGeneration++;map.close();}
    public boolean back(){if(map.exitPan())return true;if(history){showMap();return true;}return false;}
    private void requestLocation(){if(locating||closed)return;locating=true;updateStatus();locate.accept(fix->{if(closed)return;locating=false;if(fix!=null){actual=new Location(fix);map.locate(actual,radius);}updateStations();});}
    private void refreshCatalog(){
        if(loading||closed)return;loading=true;updateStatus();AirStationClient.fetch().whenComplete((response,error)->handler.post(()->{
            if(closed)return;loading=false;
            try{if(error!=null)throw new IllegalStateException("Unavailable");all=StationCatalog.parse(response,System.currentTimeMillis());cache.edit().putString("feed",response).apply();catalogFailed=false;}catch(Exception e){catalogFailed=true;}
            updateStations();
        }));
    }
    private void updateStatus(){
        String location=actual==null?t(R.string.hb_fs_location_missing):t(System.currentTimeMillis()-actual.getTime()>15*60_000?R.string.hb_rain_last_location:R.string.hb_fs_you)+" · "+(actual.hasAccuracy()?t(R.string.hb_fs_accuracy)+" ±"+Math.round(actual.getAccuracy())+" m":"")+" · "+t(R.string.hb_fs_age)+" "+Math.max(0,(System.currentTimeMillis()-actual.getTime())/60000)+" min";
        status.setText(location+" · "+t(R.string.hb_fs_radius)+" "+radius+" km · "+(loading?t(R.string.hb_rain_loading):catalogFailed?t(R.string.hb_rain_offline):"AirBKK"));
    }
    private void updateStations(){
        nearby=actual==null?new ArrayList<>():StationCatalog.nearby(all,actual.getLatitude(),actual.getLongitude(),radius);
        StationCatalog.Station next=null;for(StationCatalog.Station station:nearby)if(station.id.equals(selectedId))next=station;
        selected=next==null?(nearby.isEmpty()?null:nearby.get(0)):next;selectedId=selected==null?"":selected.id;
        stationList.removeAllViews();
        if(nearby.isEmpty())stationList.addView(h.text(t(actual==null?R.string.hb_fs_location_missing:R.string.hb_fs_no_stations),16));
        for(StationCatalog.Station s:nearby){
            android.widget.Button button=h.button(s.name+" · "+String.format(Locale.getDefault(),"%.1f km · %s",distance(s),Double.isFinite(s.pm25)?String.format(Locale.getDefault(),"%.1f µg/m³",s.pm25):"—"),()->select(s));
            button.setOnFocusChangeListener((v,focused)->{button.setBackground(h.background(focused));if(focused)select(s);});stationList.addView(button);
        }
        map.stations(nearby,selectedId);if(selected!=null)detail.setText(describe(selected));else detail.setText(t(R.string.hb_fs_no_stations));updateStatus();
    }
    private void select(StationCatalog.Station station){selected=station;selectedId=station.id;detail.setText(describe(station));map.stations(nearby,selectedId);map.setContentDescription(describe(station));}
    private double distance(StationCatalog.Station s){return actual==null?Double.NaN:StationCatalog.distanceKm(actual.getLatitude(),actual.getLongitude(),s.latitude,s.longitude);}
    private String time(long millis,ZoneId zone){return DateTimeFormatter.ofPattern("dd MMM uuuu HH:mm",h.activity.getResources().getConfiguration().getLocales().get(0)).withZone(zone).format(Instant.ofEpochMilli(millis));}
    private String describe(StationCatalog.Station s){
        AirQualityReading r=s.reading();return s.name+"\n"+s.area+"\n"+String.format(Locale.getDefault(),"%.1f km",distance(s))+"\n\nPM2.5: "+(r==null?"—":String.format(Locale.getDefault(),"%.1f µg/m³",s.pm25))+"\n"+t(R.string.hb_fs_measured)+"\n"+(r==null?t(R.string.hb_fs_missing):r.conditionLabel())+"\n\n"+(s.time>0?time(s.time,AirQualityReading.BANGKOK)+" · Asia/Bangkok\n"+t(R.string.hb_fs_age)+" "+Math.max(0,(System.currentTimeMillis()-s.time)/60000)+" min":"—")+"\n\nAirBKK / BMA\nhttps://official.airbkk.com/airbkk/th"+(r!=null&&r.stale(System.currentTimeMillis())?"\n"+h.activity.getString(R.string.hb_stale):"");
    }
    private void detach(View v){if(v.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)v.getParent()).removeView(v);}
    private void showMap(){
        history=false;historyGeneration++;body.removeAllViews();LinearLayout row=h.row();detach(map);row.addView(map,new LinearLayout.LayoutParams(0,-1,1.6f));
        LinearLayout side=h.column();ScrollView details=new ScrollView(h.activity);detach(detail);details.addView(detail);side.addView(details,new LinearLayout.LayoutParams(-1,0,1.2f));
        side.addView(h.button(R.string.hb_fs_history,()->showHistory(false)));
        ScrollView stations=new ScrollView(h.activity);detach(stationList);stations.addView(stationList);side.addView(stations,new LinearLayout.LayoutParams(-1,0,1));
        row.addView(side,new LinearLayout.LayoutParams(0,-1,1));body.addView(row,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout controls=h.row();controls.addView(h.button(R.string.hb_fs_pan,map::pan));controls.addView(h.button("−",()->map.zoom(-1)));controls.addView(h.button("+",()->map.zoom(1)));controls.addView(h.button(R.string.hb_fs_retry,this::refreshCatalog));body.addView(controls);updateStations();
    }
    private void showHistory(boolean modelView){
        history=true;model=modelView;historyStation=selected;body.removeAllViews();LinearLayout controls=h.row();
        for(int days:new int[]{1,7,30})controls.addView(h.button(days==1?R.string.hb_fs_hour24:days==7?R.string.hb_fs_day7:R.string.hb_fs_day30,()->{range=days;renderHistory();}));
        controls.addView(h.button(R.string.hb_fs_day,()->{
            ZoneId zone=model?card.expandedZone():AirQualityReading.BANGKOK;java.util.Map<java.time.LocalDate,List<AirQualityReading>> days=new java.util.TreeMap<>();long from=System.currentTimeMillis()-range*86400_000L;
            for(AirQualityReading sample:samples)if(sample.timeMillis>=from&&sample.timeMillis<=System.currentTimeMillis())days.computeIfAbsent(Instant.ofEpochMilli(sample.timeMillis).atZone(zone).toLocalDate(),k->new ArrayList<>()).add(sample);
            StringBuilder summary=new StringBuilder(t(model?R.string.hb_fs_model:R.string.hb_fs_measured));
            for(java.util.Map.Entry<java.time.LocalDate,List<AirQualityReading>> entry:days.entrySet()){double min=Double.POSITIVE_INFINITY,max=0;for(AirQualityReading sample:entry.getValue()){min=Math.min(min,sample.pm25);max=Math.max(max,sample.pm25);}summary.append("\n\n").append(entry.getKey()).append("\n").append(t(R.string.hb_fs_minimum)).append(": ").append(min).append(" · ").append(t(R.string.hb_fs_maximum)).append(": ").append(max).append(" µg/m³\n").append(t(R.string.hb_fs_samples)).append(": ").append(entry.getValue().size());}
            h.details(t(R.string.hb_fs_history),summary.toString());
        }));body.addView(controls);
        LinearLayout row=h.row();SampleChart chart=new SampleChart();row.addView(chart,new LinearLayout.LayoutParams(0,-1,1.5f));historyDetail=h.detailPane(row,1);body.addView(row,new LinearLayout.LayoutParams(-1,0,1));
        samples=new ArrayList<>();int token=++historyGeneration;
        if(model){samples=card.expandedHistory();renderHistory();}
        else if(selected!=null){
            StationCatalog.Station station=selected;List<AirQualityReading> saved=histories.get(station.id);if(saved!=null){samples=saved;renderHistory();}
            else{if(station.reading()!=null)samples.add(station.reading());renderHistory();historyDetail.append("\n"+h.activity.getString(R.string.hb_rain_loading));
                card.expandedHistory(station,values->{if(closed||!history||token!=historyGeneration)return;samples=values;histories.put(station.id,values);while(histories.size()>6)histories.remove(histories.keySet().iterator().next());renderHistory();});}
        }else renderHistory();
        chart.post(chart::requestFocus);
    }
    private void renderHistory(){
        if(!history||body.getChildCount()<2)return;LinearLayout row=(LinearLayout)body.getChildAt(1);SampleChart chart=(SampleChart)row.getChildAt(0);
        chart.values=new ArrayList<>();long from=System.currentTimeMillis()-range*86400_000L,now=System.currentTimeMillis();
        for(AirQualityReading r:samples)if(r.timeMillis>=from&&r.timeMillis<=now)chart.values.add(r);
        chart.index=Math.max(0,chart.values.size()-1);for(int i=0;i<chart.values.size();i++)if(chart.values.get(i).timeMillis==selectedTime)chart.index=i;
        chart.update();chart.invalidate();
    }
    private final class SampleChart extends View{
        List<AirQualityReading> values=new ArrayList<>();int index;final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        SampleChart(){super(h.activity);setFocusable(true);setFocusableInTouchMode(true);}
        void update(){
            if(values.isEmpty()){historyDetail.setText(t(R.string.hb_fs_missing));return;}
            AirQualityReading r=values.get(index);selectedTime=r.timeMillis;ZoneId zone=model?card.expandedZone():AirQualityReading.BANGKOK;
            double min=Double.POSITIVE_INFINITY,max=0;for(AirQualityReading value:values){min=Math.min(min,value.pm25);max=Math.max(max,value.pm25);}
            String label=(model?card.expandedLabel():historyStation==null?"":historyStation.name)+"\n"+String.format(Locale.getDefault(),"%.1f µg/m³",r.pm25)+"\n"+time(r.timeMillis,zone)+" · "+zone+"\n"+t(model?R.string.hb_fs_model:R.string.hb_fs_measured)+"\n\n"+t(R.string.hb_fs_samples)+": "+values.size()+"\n"+t(R.string.hb_fs_minimum)+": "+String.format(Locale.getDefault(),"%.1f",min)+"\n"+t(R.string.hb_fs_maximum)+": "+String.format(Locale.getDefault(),"%.1f",max)+"\n\n"+time(values.get(0).timeMillis,zone)+" — "+time(values.get(values.size()-1).timeMillis,zone);
            historyDetail.setText(label);setContentDescription(label);invalidate();
        }
        protected void onDraw(Canvas c){
            if(values.isEmpty())return;float l=h.dp(35),top=h.dp(24),bottom=getHeight()-h.dp(35),w=Math.max(1,getWidth()-l-h.dp(8));long now=System.currentTimeMillis(),from=now-range*86400_000L;
            double max=1;for(AirQualityReading r:values)max=Math.max(max,r.pm25);p.setTextSize(h.dp(12));p.setColor(h.palette.text);c.drawText("PM2.5 µg/m³",l,h.dp(16),p);
            AirQualityReading previous=null;float px=0,py=0;
            for(int i=0;i<values.size();i++){
                AirQualityReading r=values.get(i);float x=l+(float)((r.timeMillis-from)/(range*86400_000d)*w),y=(float)(bottom-(bottom-top)*r.pm25/max);
                p.setColor(0xff76cddd);p.setStrokeWidth(h.dp(2));if(previous!=null&&r.timeMillis-previous.timeMillis<=90*60_000)c.drawLine(px,py,x,y,p);c.drawCircle(x,y,h.dp(i==index?4:1),p);
                if(i==index){p.setColor(h.palette.text);c.drawCircle(x,y,h.dp(4),p);}px=x;py=y;previous=r;
            }p.setColor(h.palette.muted);c.drawText(range+" d · "+t(R.string.hb_fs_samples)+" "+values.size(),l,getHeight()-h.dp(8),p);
        }
        public boolean onKeyDown(int key,KeyEvent e){if(key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT){if(!values.isEmpty()){index=Math.max(0,Math.min(values.size()-1,index+(key==KeyEvent.KEYCODE_DPAD_LEFT?-1:1)));update();}return true;}return super.onKeyDown(key,e);}
        public boolean onKeyUp(int key,KeyEvent e){if(key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER){StringBuilder rows=new StringBuilder(historyDetail.getText());ZoneId zone=model?card.expandedZone():AirQualityReading.BANGKOK;for(AirQualityReading r:values)rows.append("\n").append(time(r.timeMillis,zone)).append(" · ").append(r.pm25).append(" µg/m³");h.details(t(R.string.hb_fs_history),rows.toString());return true;}return super.onKeyUp(key,e);}
        public boolean onTouchEvent(MotionEvent e){if(e.getAction()==MotionEvent.ACTION_UP&&!values.isEmpty()){long from=System.currentTimeMillis()-range*86400_000L;long target=from+(long)((e.getX()-h.dp(35))/Math.max(1,getWidth()-h.dp(43))*range*86400_000d);long best=Long.MAX_VALUE;for(int i=0;i<values.size();i++){long d=Math.abs(values.get(i).timeMillis-target);if(d<best){best=d;index=i;}}update();requestFocus();performClick();}return true;}
        public boolean performClick(){return super.performClick();}
    }
}
