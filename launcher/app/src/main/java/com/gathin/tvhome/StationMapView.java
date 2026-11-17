package com.gathin.tvhome;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.location.Location;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.util.LruCache;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** Native geographic tiles and station markers; only visible tiles are requested. */
final class StationMapView extends View {
    private final WidgetExpansion h;
    private final Consumer<StationCatalog.Station> selected;
    private final Runnable details;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ExecutorService worker=Executors.newFixedThreadPool(2);
    private final Handler handler=new Handler();
    private final LruCache<String,Bitmap> memory=new LruCache<>(40);
    private final Set<String> pending=new HashSet<>();
    private final java.util.Map<String,Long> retryAfter=new java.util.HashMap<>();
    private final File cache;
    private List<StationCatalog.Station> stations=new ArrayList<>();
    private Location actual;
    private String stationId="";
    private double centerX=MapGeometry.x(100.5),centerY=MapGeometry.y(13.73);
    private int zoom=10,index,radius=25;
    private boolean pan,closed,active=true,failed;
    private float touchX,touchY;
    StationMapView(WidgetExpansion host,Consumer<StationCatalog.Station> selection,Runnable details){
        super(host.activity);h=host;selected=selection;this.details=details;setFocusable(true);setFocusableInTouchMode(true);
        cache=new File(h.activity.getCacheDir(),"station-map-tiles");cache.mkdirs();
        setContentDescription(h.activity.getString(R.string.hb_fs_nearby));
    }
    void stations(List<StationCatalog.Station> value,String id){stations=value;stationId=id;for(int i=0;i<stations.size();i++)if(stations.get(i).id.equals(id))index=i;invalidate();}
    void locate(Location value,int radius){this.radius=radius;actual=value==null?null:new Location(value);if(actual!=null){centerX=MapGeometry.x(actual.getLongitude());centerY=MapGeometry.y(actual.getLatitude());if(getWidth()>0)fit(radius);}retryAfter.clear();failed=false;invalidate();}
    protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);if(actual!=null&&oldw==0)fit(radius);}
    void fit(int radius){double ratio=Math.max(1,Math.min(getWidth(),getHeight()))*40075016.7*Math.cos(Math.toRadians(actual==null?13.73:actual.getLatitude()))/(2*radius*1000*256);zoom=Math.max(2,Math.min(17,(int)Math.floor(Math.log(Math.max(1,ratio))/Math.log(2))));invalidate();}
    void zoom(int delta){zoom=Math.max(2,Math.min(18,zoom+delta));invalidate();}
    void pan(){pan=!pan;requestFocus();invalidate();}
    boolean exitPan(){if(!pan)return false;pan=false;invalidate();return true;}
    void active(boolean value){active=value;if(value)invalidate();}
    void close(){closed=true;worker.shutdownNow();handler.removeCallbacksAndMessages(null);memory.evictAll();}
    private double world(){return 256*Math.pow(2,zoom);}
    private float sx(double lon){return (float)(getWidth()/2+MapGeometry.wrapped(MapGeometry.x(lon)-centerX)*world());}
    private float sy(double lat){return (float)(getHeight()/2+(MapGeometry.y(lat)-centerY)*world());}
    protected void onDraw(Canvas c){
        c.drawColor(h.palette.raised);double world=world();int n=1<<zoom;
        double left=centerX*world-getWidth()/2d,top=centerY*world-getHeight()/2d;
        for(int y=(int)Math.floor(top/256);y<=Math.floor((top+getHeight())/256);y++)for(int x=(int)Math.floor(left/256);x<=Math.floor((left+getWidth())/256);x++){
            if(y<0||y>=n)continue;int tx=Math.floorMod(x,n);String key=zoom+"_"+tx+"_"+y;
            Bitmap tile=memory.get(key);if(tile!=null)c.drawBitmap(tile,(float)(x*256-left),(float)(y*256-top),p);else if(active&&!closed)load(key,zoom,tx,y);
        }
        // Overlapping stations group by screen cell. The count is never an averaged reading.
        java.util.Map<String,List<StationCatalog.Station>> groups=new java.util.LinkedHashMap<>();
        for(StationCatalog.Station s:stations){String cell=((int)(sx(s.longitude)/h.dp(60)))+":"+((int)(sy(s.latitude)/h.dp(30)));groups.computeIfAbsent(cell,k->new ArrayList<>()).add(s);}
        for(List<StationCatalog.Station> group:groups.values()){
            StationCatalog.Station s=group.get(0);for(StationCatalog.Station station:group)if(station.id.equals(stationId))s=station;
            float x=sx(s.longitude),y=sy(s.latitude);if(x<0||x>getWidth()||y<0||y>getHeight())continue;
            AirQualityReading reading=s.reading();int color=reading==null||reading.stale(System.currentTimeMillis())?0xff9da5aa:AirQualityCard.conditionColor(reading.condition());
            p.setColor(color);c.drawRoundRect(x-h.dp(23),y-h.dp(12),x+h.dp(23),y+h.dp(12),h.dp(5),h.dp(5),p);
            if(s.id.equals(stationId)){p.setColor(0xffffffff);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(h.dp(2));c.drawRoundRect(x-h.dp(25),y-h.dp(14),x+h.dp(25),y+h.dp(14),h.dp(6),h.dp(6),p);p.setStyle(Paint.Style.FILL);}
            p.setTextSize(h.dp(12));p.setColor(0xff102333);String label=group.size()>1?"×"+group.size():reading==null?"—":String.format(java.util.Locale.getDefault(),"%.1f",s.pm25);c.drawText(label,x-p.measureText(label)/2,y+h.dp(4),p);
        }
        if(actual!=null){float x=sx(actual.getLongitude()),y=sy(actual.getLatitude());
            if(actual.hasAccuracy()){float radius=(float)(actual.getAccuracy()*world/(40075016.7*Math.cos(Math.toRadians(actual.getLatitude()))));p.setColor(0x444bb6ff);c.drawCircle(x,y,Math.max(h.dp(8),radius),p);}
            p.setColor(0xff4bb6ff);c.drawCircle(x,y,h.dp(6),p);p.setColor(0xffffffff);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(h.dp(2));c.drawCircle(x,y,h.dp(7),p);p.setStyle(Paint.Style.FILL);p.setTextSize(h.dp(12));p.setColor(0xee163c57);c.drawRoundRect(x+h.dp(7),y-h.dp(17),Math.min(getWidth(),x+h.dp(175)),y+h.dp(5),h.dp(4),h.dp(4),p);p.setColor(0xffffffff);c.drawText(h.activity.getString(System.currentTimeMillis()-actual.getTime()>15*60_000?R.string.hb_rain_last_location:R.string.hb_fs_you),x+h.dp(10),y,p);
        }
        p.setColor(0xdc102a35);c.drawRect(0,getHeight()-h.dp(37),getWidth(),getHeight(),p);p.setColor(0xffffffff);p.setTextSize(h.dp(10));
        c.drawText("© OpenStreetMap contributors · AirBKK · PM2.5 µg/m³",h.dp(5),getHeight()-h.dp(6),p);
        if(pan||failed||!pending.isEmpty())c.drawText(h.activity.getString(pan?R.string.hb_fs_pan_hint:failed?R.string.hb_fs_map_failed:R.string.hb_fs_map_loading),h.dp(5),getHeight()-h.dp(22),p);
    }
    private void load(String key,int z,int x,int y){
        if(System.currentTimeMillis()<retryAfter.getOrDefault(key,0L))return;
        if(!pending.add(key)||pending.size()>32){pending.remove(key);return;}
        worker.execute(()->{
            Bitmap bitmap=null;HttpURLConnection c=null;
            try{
                File file=new File(cache,key+".png");android.content.SharedPreferences meta=h.activity.getSharedPreferences("station-map-cache",0);
                long expires=meta.getLong(key+"-expires",0);
                if(file.isFile()){bitmap=BitmapFactory.decodeFile(file.toString());if(bitmap!=null){Bitmap cached=bitmap;handler.post(()->{if(!closed){memory.put(key,cached);invalidate();}});}}
                if(bitmap==null||expires<System.currentTimeMillis()){
                    c=(HttpURLConnection)new URL("https://tile.openstreetmap.org/"+z+"/"+x+"/"+y+".png").openConnection();
                    c.setConnectTimeout(8000);c.setReadTimeout(10000);c.setInstanceFollowRedirects(false);c.setRequestProperty("User-Agent","HomeboardTV (+https://github.com/wbniv/homeboard-tv)");
                    String etag=meta.getString(key+"-etag","");if(!etag.isEmpty()&&bitmap!=null)c.setRequestProperty("If-None-Match",etag);
                    int code=c.getResponseCode();if(code!=200&&code!=304)throw new java.io.IOException("Map unavailable");
                    if(code==200){try(InputStream in=c.getInputStream();java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){
                        byte[] buf=new byte[8192];int count;while((count=in.read(buf))!=-1){if(out.size()+count>512000)throw new java.io.IOException("Tile too large");out.write(buf,0,count);}byte[] data=out.toByteArray();bitmap=BitmapFactory.decodeByteArray(data,0,data.length);if(bitmap==null)throw new java.io.IOException("Invalid tile");
                        try(java.io.FileOutputStream output=new java.io.FileOutputStream(file)){output.write(data);}
                    }}
                    long ttl=7*86400_000L;String cc=c.getHeaderField("Cache-Control");if(cc!=null){java.util.regex.Matcher m=java.util.regex.Pattern.compile("max-age=(\\d+)").matcher(cc);if(m.find())ttl=Math.max(0,Long.parseLong(m.group(1))*1000);}
                    android.content.SharedPreferences.Editor edit=meta.edit().putLong(key+"-expires",System.currentTimeMillis()+ttl);String tag=c.getHeaderField("ETag");if(tag!=null)edit.putString(key+"-etag",tag);edit.apply();
                    synchronized(StationMapView.class){File[] files=cache.listFiles();if(files!=null&&files.length>80){java.util.Arrays.sort(files,java.util.Comparator.comparingLong(File::lastModified));for(int i=0;i<files.length-80;i++){String old=files[i].getName().replace(".png","");if(files[i].delete())meta.edit().remove(old+"-expires").remove(old+"-etag").apply();}}}
                }
            }catch(Exception ignored){handler.post(()->{if(!closed){failed=true;retryAfter.put(key,System.currentTimeMillis()+60_000);}});}finally{if(c!=null)c.disconnect();}
            Bitmap ready=bitmap;handler.post(()->{pending.remove(key);if(!closed){if(ready!=null)memory.put(key,ready);invalidate();}});
        });
    }
    public boolean onKeyDown(int key,KeyEvent event){
        int dx=key==KeyEvent.KEYCODE_DPAD_LEFT?-1:key==KeyEvent.KEYCODE_DPAD_RIGHT?1:0,dy=key==KeyEvent.KEYCODE_DPAD_UP?-1:key==KeyEvent.KEYCODE_DPAD_DOWN?1:0;
        if(pan&&(dx!=0||dy!=0)){centerX=(centerX+dx*64/world()+1)%1;centerY=Math.max(0,Math.min(1,centerY+dy*64/world()));invalidate();return true;}
        if(dx!=0&&!stations.isEmpty()){index=Math.max(0,Math.min(stations.size()-1,index+dx));selected.accept(stations.get(index));return true;}return super.onKeyDown(key,event);
    }
    public boolean onKeyUp(int key,KeyEvent event){if(key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER){details.run();return true;}return super.onKeyUp(key,event);}
    public boolean onTouchEvent(MotionEvent e){
        if(e.getAction()==MotionEvent.ACTION_DOWN){touchX=e.getX();touchY=e.getY();return true;}
        if(e.getAction()==MotionEvent.ACTION_UP){float dx=e.getX()-touchX,dy=e.getY()-touchY;if(Math.hypot(dx,dy)>h.dp(12)){centerX=(centerX-dx/world()+1)%1;centerY=Math.max(0,Math.min(1,centerY-dy/world()));invalidate();}
            else{StationCatalog.Station nearest=null;double best=h.dp(40);for(StationCatalog.Station s:stations){double distance=Math.hypot(e.getX()-sx(s.longitude),e.getY()-sy(s.latitude));if(distance<best){nearest=s;best=distance;}}if(nearest!=null)selected.accept(nearest);}performClick();return true;}return true;
    }
    public boolean performClick(){return super.performClick();}
}
