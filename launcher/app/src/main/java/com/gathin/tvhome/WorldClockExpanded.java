package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.json.JSONArray;

/** Rich offline comparison and map; browsing never mutates saved widget settings. */
final class WorldClockExpanded implements WidgetExpansion.Content {
    private final WidgetExpansion h;
    private final WorldClockSettings settings;
    private final List<WorldClockSettings.Place> places;
    private final WorldClockCards cards;
    private final WorldClockTicker ticker;
    private final LinearLayout root;
    private final TextView status,detail;
    private final Stage stage;
    private final Timeline timeline;
    private final ScrollView timelineScroll;
    private final Button previous,durationButton,mapButton;
    private int selected,page,duration=30;
    private boolean comparing,map,expiredNotice;
    private Instant at,now,window;
    private static Path land;

    WorldClockExpanded(WidgetExpansion host,WorldClockSettings settings,Bundle saved) {
        h=host;this.settings=settings;places=WorldClockModel.ordered(settings);cards=new WorldClockCards(h.activity,settings,h.palette);
        String id=saved.getString("selected",settings.primary);for(int i=0;i<places.size();i++)if(places.get(i).id.equals(id))selected=i;
        comparing=saved.getBoolean("comparing",false);map=saved.getBoolean("map",false);page=Math.max(0,saved.getInt("page",0));
        duration=saved.getInt("duration",30);if(duration!=15&&duration!=30&&duration!=60&&duration!=90)duration=30;
        try { at=Instant.parse(saved.getString("at","")); } catch(RuntimeException invalid) { at=Instant.now(); }
        root=h.column();LinearLayout controls=h.row();
        controls.addView(h.button(R.string.hb_wc_compare,this::compareAction));
        controls.addView(h.button(R.string.hb_wc_now,()->{comparing=false;page=0;expiredNotice=false;refresh();}));
        mapButton=h.button(R.string.hb_wc_map,this::toggleMap);controls.addView(mapButton);
        previous=h.button(R.string.hb_fs_previous,()->page(-1));controls.addView(previous);
        controls.addView(h.button(R.string.hb_fs_next,()->page(1)));
        durationButton=h.button("",()->{duration=duration==15?30:duration==30?60:duration==60?90:15;refresh();});controls.addView(durationButton);
        root.addView(controls);status=h.text("",14);root.addView(status);
        LinearLayout middle=h.row();stage=new Stage();middle.addView(stage,new LinearLayout.LayoutParams(0,-1,1.8f));
        detail=h.detailPane(middle,1);root.addView(middle,new LinearLayout.LayoutParams(-1,0,1.6f));
        root.addView(h.text(h.activity.getString(R.string.hb_wc_daytime_legend),11));
        timeline=new Timeline();timelineScroll=new ScrollView(h.activity);timelineScroll.setFillViewport(true);
        timelineScroll.addView(timeline,new ScrollView.LayoutParams(-1,h.dp(28+places.size()*23)));
        root.addView(timelineScroll,new LinearLayout.LayoutParams(-1,0,1));
        ticker=new WorldClockTicker(h.activity,this::refresh);refresh();stage.post(stage::requestFocus);
    }
    private String t(int id) { return h.activity.getString(id); }
    private void compareAction() { startComparison();timeline.requestFocus();revealSelected(); }
    private void toggleMap() { map=!map;refresh();stage.requestFocus(); }
    private ZoneId reference() { return settings.reference(ZoneId.systemDefault()); }
    private void startComparison() { if(!comparing){comparing=true;at=WorldClockModel.nextHalfHour(Instant.now(),reference());}refresh(); }
    private void page(int delta) {
        page=Math.max(0,page+delta);if(!comparing)startComparison();
        at=WorldClockModel.pageStart(Instant.now(),reference(),page);
        if(!at.isAfter(Instant.now()))at=WorldClockModel.nextHalfHour(Instant.now(),reference());refresh();
    }
    private void select(int index) { selected=Math.max(0,Math.min(places.size()-1,index));refresh();revealSelected(); }
    private void revealSelected() {
        timeline.post(()->timeline.requestRectangleOnScreen(new Rect(0,h.dp(25+selected*23),timeline.getWidth(),h.dp(25+(selected+1)*23)),false));
    }
    private void refresh() {
        now=Instant.now();window=WorldClockModel.pageStart(now,reference(),page);
        if(!comparing)at=now;
        else if(at.isBefore(now)) { at=WorldClockModel.nextHalfHour(now,reference());page=0;window=WorldClockModel.pageStart(now,reference(),0);expiredNotice=true; }
        String mode=t(comparing?R.string.hb_wc_comparing:R.string.hb_wc_now);
        status.setText(mode+" · "+WorldClockLabels.date(h.activity,window,reference())+" · "+WorldClockLabels.time(h.activity,settings,at,reference(),false)
                +" · "+reference().getId()+(expiredNotice?" · "+t(R.string.hb_wc_expired):""));
        previous.setEnabled(page>0);durationButton.setText(h.activity.getString(R.string.hb_wc_duration,duration));mapButton.setText(t(map?R.string.hb_wc_clocks:R.string.hb_wc_map));
        detail.setText(WorldClockLabels.detail(h.activity,settings,places.get(selected),at,!comparing&&settings.seconds,comparing?duration:null));
        stage.setContentDescription(detail.getText());timeline.setContentDescription(t(R.string.hb_wc_compare)+" · "+detail.getText());stage.invalidate();timeline.invalidate();
    }
    private void moveTime(int delta) {
        if(!comparing)startComparison();
        Instant proposed=at.plusSeconds(delta*1800L),minimum=WorldClockModel.nextHalfHour(Instant.now(),reference());
        if(proposed.isBefore(minimum))proposed=minimum;at=proposed;
        Instant base=WorldClockModel.windowStart(Instant.now(),reference());
        page=Math.max(0,(int)(java.time.Duration.between(base,at).getSeconds()/86400));refresh();
    }
    private void details() { h.details(WorldClockLabels.name(h.activity,places.get(selected)),WorldClockLabels.detail(h.activity,settings,places.get(selected),at,!comparing&&settings.seconds,comparing?duration:null)); }
    public View view() { return root; }
    public void active(boolean active) { if(active){ticker.start(settings.seconds);root.post(()->{stage.requestFocus();revealSelected();});}else ticker.stop(); }
    public void close() { ticker.stop(); }
    public boolean back() { if(comparing){comparing=false;page=0;expiredNotice=false;refresh();stage.requestFocus();return true;}return false; }
    public Bundle state() {
        Bundle b=new Bundle();b.putString("selected",places.get(selected).id);b.putString("at",at.toString());b.putBoolean("comparing",comparing);b.putBoolean("map",map);b.putInt("page",page);b.putInt("duration",duration);return b;
    }
    private final class Stage extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private float mx,my,mw,mh;
        private int columns=1,visibleRows=1,firstRow;
        Stage() { super(h.activity);setFocusable(true);setFocusableInTouchMode(true);setOnClickListener(v->details()); }
        @Override protected void onDraw(Canvas c) {
            if(map)drawMap(c);else drawCards(c);
        }
        private void drawCards(Canvas c) {
            float density=getResources().getDisplayMetrics().density,scale=getResources().getConfiguration().fontScale;
            int save=c.save();c.scale(density,density);float width=getWidth()/density,height=getHeight()/density;
            columns=Math.max(1,(int)(width/(145*scale)));visibleRows=Math.max(1,(int)(height/(91*scale)));
            firstRow=Math.max(0,Math.min(firstRow,selected/columns));if(selected/columns>=firstRow+visibleRows)firstRow=selected/columns-visibleRows+1;
            float cellWidth=width/columns,cellHeight=height/visibleRows;
            for(int row=0;row<visibleRows;row++)for(int col=0;col<columns;col++) {
                int index=(row+firstRow)*columns+col;if(index>=places.size())break;
                int cell=c.save();c.translate(col*cellWidth,row*cellHeight);
                cards.draw(c,settings,places.get(index),at,cellWidth-4,cellHeight-4,index==selected,h.opacity);c.restoreToCount(cell);
            }
            c.restoreToCount(save);
        }
        private void drawMap(Canvas c) {
            loadLand();float pad=h.dp(5);mw=Math.max(1,getWidth()-2*pad);mh=mw/2;
            if(mh>getHeight()-h.dp(26)){mh=Math.max(1,getHeight()-h.dp(26));mw=mh*2;}
            mx=(getWidth()-mw)/2;my=(getHeight()-mh)/2;
            paint.setColor(h.palette.raised);c.drawRect(mx,my,mx+mw,my+mh,paint);
            if(land!=null){int save=c.save();c.translate(mx,my);c.scale(mw,mh);paint.setColor(WorldClockCards.mix(h.palette.raised,h.palette.muted,.27f));c.drawPath(land,paint);c.restoreToCount(save);}
            else cards.text(c,t(R.string.hb_wc_map_unavailable),mx+h.dp(8),my+h.dp(22),h.dp(12),h.palette.text,mw-h.dp(16),false);
            // Labels use alternating lanes; the companion selection/detail always retains full names.
            int lane=0;java.util.List<android.graphics.RectF> labels=new java.util.ArrayList<>();
            for(int pass=0;pass<places.size();pass++) {
                int i=pass==places.size()-1?selected:pass>=selected?pass+1:pass;
                WorldClockSettings.Place p=places.get(i);if(!p.located())continue;
                float x=mx+(float)WorldClockModel.mapX(p.longitude)*mw,y=my+(float)WorldClockModel.mapY(p.latitude)*mh;
                ZoneId zone=p.zone(ZoneId.systemDefault());
                paint.setColor(zone!=null&&!WorldClockModel.daytime(at,zone)?WorldClockCards.mix(h.palette.surface,cards.color(p),.65f):cards.color(p));c.drawCircle(x,y,h.dp(i==selected?4:3),paint);
                if(i==selected){paint.setStyle(Paint.Style.STROKE);paint.setColor(h.palette.text);paint.setStrokeWidth(h.dp(2));c.drawCircle(x,y,h.dp(7),paint);paint.setStyle(Paint.Style.FILL);}
                String label=WorldClockLabels.name(h.activity,p)+(zone==null?"":" · "+WorldClockLabels.time(h.activity,settings,at,zone,false));
                float w=Math.min(h.dp(145),mw),left=Math.max(mx,Math.min(mx+mw-w,x-w/2)),baseline=Math.max(my+h.dp(14),Math.min(my+mh-h.dp(4),y-h.dp(11+lane%3*14)));
                android.graphics.RectF box=new android.graphics.RectF(left,baseline-h.dp(12),left+w,baseline+h.dp(3));
                boolean collision=false;for(android.graphics.RectF prior:labels)if(android.graphics.RectF.intersects(prior,box))collision=true;
                if(collision&&i!=selected){lane++;continue;}labels.add(box);
                paint.setColor(h.palette.surface);paint.setAlpha(235);c.drawRect(box,paint);paint.setAlpha(255);
                cards.text(c,label,left+h.dp(3),baseline,h.dp(10),cards.color(p),w-h.dp(6),false);lane++;
            }
            cards.text(c,t(R.string.hb_wc_map_controls)+" · "+(selected+1)+"/"+places.size(),h.dp(4),getHeight()-h.dp(4),h.dp(10),h.palette.muted,getWidth()-h.dp(8),false);
        }
        private void loadLand() {
            if(land!=null)return;
            try(InputStream in=getContext().getAssets().open("world-clock-land.json")) {
                java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[8192];int count;
                while((count=in.read(buffer))!=-1)bytes.write(buffer,0,count);
                JSONArray rings=new JSONArray(bytes.toString(StandardCharsets.UTF_8.name()));Path outline=new Path();outline.setFillType(Path.FillType.EVEN_ODD);
                for(int i=0;i<rings.length();i++){JSONArray ring=rings.getJSONArray(i);for(int j=0;j<ring.length();j++){JSONArray point=ring.getJSONArray(j);float x=(float)WorldClockModel.mapX(point.getDouble(0)),y=(float)WorldClockModel.mapY(point.getDouble(1));if(j==0)outline.moveTo(x,y);else outline.lineTo(x,y);}outline.close();}land=outline;
            }catch(Exception unavailable){/* The clocks and list remain useful without the map asset. */}
        }
        @Override public boolean onKeyDown(int key,KeyEvent e) {
            int next=selected;
            if(key==KeyEvent.KEYCODE_DPAD_UP){if(map)next--;else next-=columns;if(next<0)return super.onKeyDown(key,e);}
            else if(key==KeyEvent.KEYCODE_DPAD_DOWN){next+=map?1:columns;if(next>=places.size())return super.onKeyDown(key,e);}
            else if(!map&&key==KeyEvent.KEYCODE_DPAD_LEFT&&selected%columns>0)next--;
            else if(!map&&key==KeyEvent.KEYCODE_DPAD_RIGHT&&selected%columns<columns-1&&selected+1<places.size())next++;
            else if(key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER)return true;
            else return super.onKeyDown(key,e);
            select(next);return true;
        }
        @Override public boolean onKeyUp(int key,KeyEvent e) { if(key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER){performClick();return true;}return super.onKeyUp(key,e); }
        @Override public boolean onTouchEvent(MotionEvent e) {
            if(e.getAction()!=MotionEvent.ACTION_UP)return true;
            int next=-1;if(map){double best=h.dp(25);for(int i=0;i<places.size();i++){WorldClockSettings.Place p=places.get(i);if(!p.located())continue;double distance=Math.hypot(e.getX()-mx-WorldClockModel.mapX(p.longitude)*mw,e.getY()-my-WorldClockModel.mapY(p.latitude)*mh);if(distance<best){best=distance;next=i;}}}
            else next=(int)(e.getY()/(float)getHeight()*visibleRows+firstRow)*columns+Math.min(columns-1,(int)(e.getX()/getWidth()*columns));
            if(next>=0&&next<places.size()){if(next==selected)performClick();else select(next);requestFocus();}return true;
        }
        @Override protected void onFocusChanged(boolean focused,int direction,Rect old){super.onFocusChanged(focused,direction,old);invalidate();}
    }
    private final class Timeline extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        Timeline(){super(h.activity);setFocusable(true);setFocusableInTouchMode(true);setOnClickListener(v->details());}
        @Override protected void onDraw(Canvas c) {
            float label=Math.min(getWidth()*.23f,h.dp(118)),cell=(getWidth()-label)/24,row=h.dp(23),top=h.dp(25);
            DateTimeFormatter hour=DateTimeFormatter.ofPattern(WorldClockLabels.twelve(h.activity,settings)?"ha":"HH",java.util.Locale.ROOT);
            for(int i=0;i<24;i++)cards.text(c,hour.withZone(reference()).format(window.plusSeconds(i*3600L)),label+i*cell+h.dp(1),h.dp(17),h.dp(9),h.palette.muted,cell,false);
            for(int index=0;index<places.size();index++) {
                WorldClockSettings.Place p=places.get(index);ZoneId zone=p.zone(ZoneId.systemDefault());float y=top+index*row;
                cards.text(c,WorldClockLabels.name(h.activity,p),h.dp(4),y+h.dp(15),h.dp(11),cards.color(p),label-h.dp(6),false);
                if(zone==null)continue;
                for(int i=0;i<24;i++) {
                    Instant instant=window.plusSeconds(i*3600L);float x=label+i*cell;
                    paint.setColor(WorldClockCards.mix(h.palette.surface,cards.color(p),WorldClockModel.daytime(instant,zone)?.45f:.13f));
                    c.drawRect(x,y,x+cell-h.dp(1),y+row-h.dp(2),paint);
                    java.time.ZonedDateTime local=instant.atZone(zone);boolean repeated=zone.getRules().getValidOffsets(local.toLocalDateTime()).size()>1;
                    cards.text(c,hour.withZone(zone).format(instant),x+h.dp(2),y+h.dp(repeated?10:15),h.dp(repeated?8:9),h.palette.text,cell-h.dp(2),true);
                    if(repeated)cards.text(c,local.getOffset().getId(),x+h.dp(1),y+h.dp(20),h.dp(7),h.palette.text,cell-h.dp(1),true);
                    if(instant.isBefore(now)){float fraction=Math.min(1,java.time.Duration.between(instant,now).toMillis()/3600000f);paint.setColor(h.palette.surface);paint.setAlpha(150);c.drawRect(x,y,x+cell*fraction,y+row,paint);paint.setAlpha(255);}
                }
                if(index==selected){paint.setColor(h.palette.text);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(h.dp(hasFocus()?2:1));c.drawRect(1,y,getWidth()-1,y+row-1,paint);paint.setStyle(Paint.Style.FILL);}
            }
            double[] overlay=WorldClockModel.overlay(window,at,comparing?duration:1);
            if(overlay[1]>0){float left=label+(float)overlay[0]*(getWidth()-label),right=left+(float)overlay[1]*(getWidth()-label);paint.setColor(h.palette.text);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(h.dp(2));c.drawRect(left,top,Math.max(left+1,right),top+row*places.size(),paint);paint.setStyle(Paint.Style.FILL);}
            double live=java.time.Duration.between(window,now).toMillis()/86400000d;if(live>=0&&live<1){float x=label+(float)live*(getWidth()-label);paint.setColor(h.palette.muted);paint.setStrokeWidth(h.dp(1));c.drawLine(x,top,x,top+row*places.size(),paint);}
        }
        @Override public boolean onKeyDown(int key,KeyEvent e) {
            if(key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT){moveTime(key==KeyEvent.KEYCODE_DPAD_LEFT?-1:1);return true;}
            if(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN){int next=selected+(key==KeyEvent.KEYCODE_DPAD_UP?-1:1);if(next<0||next>=places.size())return super.onKeyDown(key,e);select(next);requestRectangleOnScreen(new Rect(0,h.dp(25+selected*23),getWidth(),h.dp(25+(selected+1)*23)),false);return true;}
            if(key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER)return true;return super.onKeyDown(key,e);
        }
        @Override public boolean onKeyUp(int key,KeyEvent e){if(key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER){performClick();return true;}return super.onKeyUp(key,e);}
        @Override public boolean onTouchEvent(MotionEvent e) {
            if(e.getAction()!=MotionEvent.ACTION_UP)return true;
            int row=(int)((e.getY()-h.dp(25))/h.dp(23));float label=Math.min(getWidth()*.23f,h.dp(118));
            if(row>=0&&row<places.size()){select(row);if(e.getX()>label){Instant proposed=window.plusSeconds(Math.max(0,Math.min(47,(int)((e.getX()-label)/(getWidth()-label)*48)))*1800L);if(!proposed.isBefore(Instant.now())){comparing=true;at=proposed;refresh();}}requestFocus();}return true;
        }
        @Override protected void onFocusChanged(boolean focused,int direction,Rect old){super.onFocusChanged(focused,direction,old);invalidate();}
        @Override public boolean performClick(){return super.performClick();}
    }
}
