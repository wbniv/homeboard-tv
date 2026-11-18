package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

final class CalendarExpanded implements WidgetExpansion.Content {
    private final WidgetExpansion h;
    private final CalendarSnapshot snapshot;
    private final CalendarWidgetSettings settings;
    final CalendarWeekWindow window;
    private final LinearLayout root;
    private final TextView range,detail;
    private final WeekGrid grid;
    private CalendarSnapshot.Display display;
    private final Map<LocalDate,CalendarSnapshot.Day> days=new HashMap<>();
    CalendarExpanded(WidgetExpansion host,CalendarSnapshot snapshot,CalendarWidgetSettings settings,LocalDate anchor,Bundle state){
        h=host;this.snapshot=snapshot;this.settings=settings;
        LocalDate selected=anchor==null?LocalDate.now(CalendarSnapshot.BANGKOK):anchor;
        try{selected=LocalDate.parse(state.getString("selected",selected.toString()));}catch(RuntimeException ignored){}
        window=new CalendarWeekWindow(selected);
        try{window.start=LocalDate.parse(state.getString("start",window.start.toString()));window.select(selected);}catch(RuntimeException ignored){}
        root=h.column();LinearLayout controls=h.row();
        controls.addView(h.button(R.string.hb_fs_previous,()->{window.page(-1);refresh();}));
        controls.addView(h.button(R.string.hb_fs_today,()->{LocalDate today=LocalDate.now(CalendarSnapshot.BANGKOK);window.start=CalendarWeekWindow.monday(today);window.select(today);refresh();}));
        controls.addView(h.button(R.string.hb_fs_next,()->{window.page(1);refresh();}));
        controls.addView(h.button(R.string.hb_fs_all_dates,()->{
            StringBuilder text=new StringBuilder();for(CalendarSnapshot.Day day:display.days)text.append(describe(day.date,true)).append("\n\n");h.details(h.activity.getString(R.string.hb_fs_all_dates),text.toString());
        }));
        root.addView(controls);range=h.text("",16);root.addView(range);
        LinearLayout content=h.row();grid=new WeekGrid();content.addView(grid,new LinearLayout.LayoutParams(0,-1,1.45f));
        detail=h.detailPane(content,1);root.addView(content,new LinearLayout.LayoutParams(-1,0,1));refresh();
        grid.post(grid::requestFocus);
    }
    private CalendarSnapshot.Day day(LocalDate date){CalendarSnapshot.Day d=days.get(date);return d==null?new CalendarSnapshot.Day(date):d;}
    private void refresh(){
        display=snapshot.display(settings,window.start,window.end());days.clear();for(CalendarSnapshot.Day day:display.days)days.put(day.date,day);
        range.setText(CalendarLabels.date(h.activity,window.start)+" — "+CalendarLabels.date(h.activity,window.end())+" · "+h.activity.getString(R.string.hb_cal_week_start));
        detail.setText(describe(window.selected));grid.setContentDescription(describe(window.selected));grid.invalidate();
    }
    private String describe(LocalDate date){return describe(date,false);}
    private String describe(LocalDate date,boolean full){
        StringBuilder text=new StringBuilder(CalendarLabels.date(h.activity,date));CalendarSnapshot.Day day=day(date);
        boolean covered=!settings.enabled.isEmpty();
        for(String id:settings.enabled){CalendarSnapshot.Provider p=snapshot.providers.get(id);if(p==null||date.isBefore(p.start)||date.isAfter(p.end)||"partial".equals(p.status)||"stale".equals(p.status))covered=false;}
        if(day.events.isEmpty())text.append("\n\n").append(h.activity.getString(settings.enabled.isEmpty()?R.string.hb_fs_no_sources:covered?R.string.hb_fs_empty:R.string.hb_fs_coverage));
        for(CalendarSnapshot.Event e:day.events){
            text.append("\n\n").append(CalendarLabels.event(h.activity,e,settings,Instant.now()));
            if(!e.title(settings).equals(CalendarLabels.event(h.activity,e,settings,Instant.now())))text.append("\n").append(e.title(settings));
            String country=new java.util.Locale("",e.country).getDisplayCountry(h.activity.getResources().getConfiguration().getLocales().get(0));
            String scope=switch(e.scope){case "national"->country;case "bangkok"->"Bangkok";case "catalunya"->h.activity.getString(R.string.hb_cal_es_region);case "tarragona-province"->h.activity.getString(R.string.hb_cal_es_province);case "tivissa"->h.activity.getString(R.string.hb_cal_es_local);default->country;};
            int kind=switch(e.kind){case HOLIDAY->R.string.hb_cal_holiday;case OBSERVED->R.string.hb_cal_observed;case WORKING->R.string.hb_cal_working;case WAN_PHRA->R.string.hb_cal_wan_phra;case MAKE->R.string.hb_cal_make;case SELL->R.string.hb_cal_sell;};
            text.append("\n").append(scope.equals(country)?country:country+" · "+scope).append(" · ").append(h.activity.getString(kind));
            if(e.anchor!=null&&(e.kind==CalendarSnapshot.Kind.MAKE||e.kind==CalendarSnapshot.Kind.SELL)) {
                text.append("\n").append(h.activity.getString(e.kind==CalendarSnapshot.Kind.MAKE?R.string.hb_fs_make:R.string.hb_fs_sell));
                text.append("\n").append(h.activity.getString(R.string.hb_fs_preparation)).append(": ").append(CalendarLabels.date(h.activity,e.anchor.minusDays(3))).append(" — ").append(CalendarLabels.date(h.activity,e.anchor.minusDays(2)));
                text.append("\n").append(h.activity.getString(R.string.hb_fs_selling)).append(": ").append(CalendarLabels.date(h.activity,e.anchor.minusDays(1))).append(" — ").append(CalendarLabels.date(h.activity,e.anchor));
            }
            text.append("\n").append(CalendarLabels.source(h.activity,e.provider));if(full)text.append("\n").append(e.url);
        }
        if(!covered&&!settings.enabled.isEmpty()&&!day.events.isEmpty())text.append("\n\n").append(h.activity.getString(R.string.hb_fs_coverage));
        if(!display.offline.isEmpty())text.append("\n\n").append(h.activity.getString(R.string.hb_cal_offline));
        return text.toString();
    }
    public View view(){return root;}
    public Bundle state(){Bundle b=new Bundle();b.putString("start",window.start.toString());b.putString("selected",window.selected.toString());return b;}
    private final class WeekGrid extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final CalendarWidgetView cards=new CalendarWidgetView(h.activity,snapshot,settings,h.palette);
        WeekGrid(){super(h.activity);setFocusable(true);setFocusableInTouchMode(true);setContentDescription(h.activity.getString(R.string.hb_cal_calendar));}
        private float pitch(){return Math.min(getWidth()/7f,Math.max(1,(getHeight()-h.dp(26))/4f));}
        protected void onDraw(Canvas c){
            float step=pitch(),size=step-h.dp(4);float dx=(getWidth()-7*step)/2;
            paint.setColor(h.palette.text);paint.setTextSize(h.dp(12));
            java.time.format.TextStyle style=java.time.format.TextStyle.SHORT;
            for(int col=0;col<7;col++)c.drawText(java.time.DayOfWeek.of(col+1).getDisplayName(style,h.activity.getResources().getConfiguration().getLocales().get(0)),dx+col*step,h.dp(18),paint);
            for(int i=0;i<28;i++){
                LocalDate date=window.start.plusDays(i);float x=dx+i%7*step,y=h.dp(26)+i/7*step;
                int save=c.save();c.translate(x,y);c.scale(size/200,size/200);cards.card(c,day(date),true,size/getResources().getDisplayMetrics().density);c.restoreToCount(save);
                if(date.equals(window.selected)){paint.setColor(h.palette.text);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(h.dp(hasFocus()?2:1));c.drawRoundRect(x,y,x+size,y+size,h.dp(5),h.dp(5),paint);paint.setStyle(Paint.Style.FILL);}
            }
        }
        public boolean onKeyDown(int key,KeyEvent event){
            int delta=key==KeyEvent.KEYCODE_DPAD_LEFT?-1:key==KeyEvent.KEYCODE_DPAD_RIGHT?1:key==KeyEvent.KEYCODE_DPAD_UP?-7:key==KeyEvent.KEYCODE_DPAD_DOWN?7:0;
            if(delta!=0){if(delta==-7&&window.selected.isBefore(window.start.plusDays(7)))return super.onKeyDown(key,event);window.select(window.selected.plusDays(delta));refresh();return true;}
            return super.onKeyDown(key,event);
        }
        public boolean onKeyUp(int key,KeyEvent event){if(key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER){h.details(CalendarLabels.date(h.activity,window.selected),describe(window.selected,true));return true;}return super.onKeyUp(key,event);}
        public boolean onTouchEvent(MotionEvent e){if(e.getAction()==MotionEvent.ACTION_UP){float step=pitch();int col=(int)((e.getX()-(getWidth()-7*step)/2)/step),row=(int)((e.getY()-h.dp(26))/step);if(col>=0&&col<7&&row>=0&&row<4){LocalDate next=window.start.plusDays(row*7L+col);if(next.equals(window.selected))h.details(CalendarLabels.date(h.activity,next),describe(next,true));else{window.select(next);refresh();requestFocus();}}performClick();}return true;}
        public boolean performClick(){return super.performClick();}
        protected void onFocusChanged(boolean gain,int direction,android.graphics.Rect previous){super.onFocusChanged(gain,direction,previous);invalidate();}
    }
}
