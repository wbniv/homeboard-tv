package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

final class RainExpanded implements WidgetExpansion.Content {
    private final WidgetExpansion h;
    private final RainWidgetController controller;
    private final LinearLayout root,days;
    private final TextView summary,detail;
    private HourChart chart;
    private final Handler handler=new Handler();
    private boolean running;
    private String date;
    private int range,index;
    private long selected;
    private RainForecast renderedForecast;
    private List<RainForecast.Hour> hours=new ArrayList<>();
    private final Runnable tick=new Runnable(){public void run(){if(running){controller.refresh(false);if(controller.forecast!=renderedForecast||controller.forecast==null)refresh();handler.postDelayed(this,3000);}}};
    RainExpanded(WidgetExpansion host,RainWidgetController controller,Bundle state){
        h=host;this.controller=controller;date=state.getString("date","");range=state.getInt("range",7);selected=state.getLong("selected",0);
        root=h.column();LinearLayout controls=h.row();
        for(int n:new int[]{24,48,7,0})controls.addView(h.button(n==24?R.string.hb_fs_hour24:n==48?R.string.hb_fs_hour48:n==7?R.string.hb_fs_day7:R.string.hb_fs_available,()->{range=n;selected=0;refresh();chart.requestFocus();}));
        controls.addView(h.button(R.string.hb_fs_reset,()->{date="";selected=0;refresh();}));controls.addView(h.button(R.string.hb_fs_retry,()->controller.refresh(true)));root.addView(controls);
        HorizontalScrollView strip=new HorizontalScrollView(h.activity);days=h.row();strip.addView(days);root.addView(strip,new LinearLayout.LayoutParams(-1,h.dp(58)));
        summary=h.text("",14);root.addView(summary);
        LinearLayout content=h.row();chart=new HourChart();content.addView(chart,new LinearLayout.LayoutParams(0,-1,1.5f));detail=h.detailPane(content,1);root.addView(content,new LinearLayout.LayoutParams(-1,0,1));refresh();chart.post(chart::requestFocus);
    }
    public View view(){return root;}
    public Bundle state(){Bundle b=new Bundle();b.putString("date",date);b.putInt("range",range);b.putLong("selected",selected);return b;}
    public void active(boolean value){running=value;controller.expandedActive(value);handler.removeCallbacks(tick);if(value){controller.refresh(false);tick.run();}}
    public void close(){active(false);}
    private String t(int id){return h.activity.getString(id);}
    private void refresh(){
        RainForecast f=controller.forecast;renderedForecast=f;if(f==null){summary.setText(controller.status());detail.setText(controller.actionHint());return;}
        Instant now=Instant.now();long start=f.windowStart(now),end;
        List<RainForecast.Period> periods=f.calendarDays(now,Math.min(10,Math.max(1,(int)((f.coverageEnd()-start)/86400)+1)));
        days.removeAllViews();int count=range==0?periods.size():Math.min(7,periods.size());
        for(int i=0;i<count;i++){
            RainForecast.Period p=periods.get(i);String day=Instant.ofEpochSecond(p.start).atZone(f.zone).toLocalDate().toString();
            String label=controller.time(p.start,"EEE dd MMM")+"\n"+controller.amount(p.mm)+" · "+controller.chance(p.chance);
            days.addView(h.button(label,()->{date=day;if(range!=0)range=7;selected=p.start;refresh();chart.requestFocus();}));
        }
        if(range==24||range==48){end=start+range*3600L;}
        else{
            LocalDate day;try{day=LocalDate.parse(date);}catch(RuntimeException e){day=now.atZone(f.zone).toLocalDate();}
            if(day.isBefore(now.atZone(f.zone).toLocalDate())||day.atStartOfDay(f.zone).toEpochSecond()>=f.coverageEnd())day=now.atZone(f.zone).toLocalDate();
            date=day.toString();start=Math.max(start,day.atStartOfDay(f.zone).toEpochSecond());end=day.plusDays(1).atStartOfDay(f.zone).toEpochSecond();
        }
        RainForecast.Period period=f.interval(start,end);hours=period.hours;
        index=0;for(int i=0;i<hours.size();i++)if(hours.get(i).start==selected){index=i;break;}
        if(!hours.isEmpty())selected=hours.get(index).start;
        String update=controller.fetchedAt>0?DisplayTime.CLOCK+controller.time(controller.fetchedAt/1000,"dd MMM HH:mm"):t(R.string.hb_fs_missing);
        summary.setText(controller.settings().displayName()+" · "+f.zone+"\n"+t(R.string.hb_fs_total)+": "+controller.amount(period.mm)+" · "+t(R.string.hb_fs_peak)+": "+controller.chance(period.chance)+" · "+(period.complete?"":t(R.string.hb_fs_partial))+"\n"+t(R.string.hb_fs_updated)+": "+update+" · "+controller.status());
        long wettest=period.wettestThreeHours();if(wettest>=0)summary.append(" · "+t(R.string.hb_fs_wettest)+": "+controller.time(wettest,"HH:mm")+"–"+controller.time(wettest+10800,"HH:mm"));
        updateDetail();chart.invalidate();
    }
    private void updateDetail(){
        if(hours.isEmpty())return;RainForecast.Hour hour=hours.get(index);selected=hour.start;
        String value=controller.time(hour.start,"EEE dd MMM HH:mm")+" — "+controller.time(hour.start+3600,"HH:mm")+"\n\n"+t(R.string.hb_fs_amount)+": "+controller.amount(hour.mm)+"\n\n"+t(R.string.hb_fs_chance)+": "+controller.chance(hour.chance);
        if(Double.isFinite(hour.snowCm)&&hour.snowCm>0)value+="\n"+h.activity.getString(R.string.hb_rain_snow)+": "+hour.snowCm+" cm";
        value+="\n\n"+h.activity.getString(R.string.hb_rain_source)+"\n"+controller.forecast.zone;
        detail.setText(value);chart.setContentDescription(value);chart.invalidate();
    }
    private final class HourChart extends View{
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);HourChart(){super(h.activity);setFocusable(true);setFocusableInTouchMode(true);}
        protected void onDraw(Canvas c){
            if(hours.isEmpty())return;float left=h.dp(32),top=h.dp(25),bottom=Math.max(top+1,getHeight()-h.dp(72)),width=Math.max(1,getWidth()-left-h.dp(8)),step=width/hours.size();
            double max=.1;for(RainForecast.Hour hour:hours)if(Double.isFinite(hour.mm))max=Math.max(max,hour.mm);double limit=Math.ceil(max*2)/2;
            p.setColor(h.palette.muted);p.setTextSize(h.dp(12));c.drawText(controller.settings().inches?"in":"mm",0,h.dp(16),p);
            for(int i=0;i<=2;i++){float y=bottom-(bottom-top)*i/2;p.setColor(h.palette.edge);c.drawLine(left,y,getWidth(),y,p);p.setColor(h.palette.muted);String label=String.format(java.util.Locale.getDefault(),controller.settings().inches?"%.2f":"%.1f",limit*i/2/(controller.settings().inches?25.4:1));c.drawText(label,0,y,p);}
            for(int i=0;i<hours.size();i++){
                RainForecast.Hour hour=hours.get(i);float x=left+i*step;
                p.setColor(0xff73c5df);if(Double.isFinite(hour.mm))c.drawRect(x+2,(float)(bottom-(bottom-top)*hour.mm/limit),x+step-2,bottom,p);
                else{p.setColor(h.palette.muted);c.drawText("—",x,bottom,p);}
                p.setColor(h.palette.edge);c.drawRect(x+2,bottom+h.dp(25),x+step-2,bottom+h.dp(32),p);
                if(Double.isFinite(hour.chance)){p.setColor(0xffb9a3ed);c.drawRect(x+2,bottom+h.dp(25),(float)(x+2+(step-4)*hour.chance/100),bottom+h.dp(32),p);}
                if(i%Math.max(1,hours.size()/6)==0){p.setColor(h.palette.muted);c.drawText(controller.time(hour.start,"HH:mm"),x,bottom+h.dp(18),p);}
                if(i==index){p.setColor(h.palette.text);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(h.dp(2));c.drawRect(x,top,x+step,bottom+h.dp(35),p);p.setStyle(Paint.Style.FILL);}
            }
            p.setColor(h.palette.text);c.drawText(t(R.string.hb_fs_chance)+" · 0–100%",left,getHeight()-h.dp(8),p);
        }
        public boolean onKeyDown(int key,KeyEvent e){if(key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT){if(!hours.isEmpty()){index=Math.max(0,Math.min(hours.size()-1,index+(key==KeyEvent.KEYCODE_DPAD_LEFT?-1:1)));updateDetail();}return true;}return super.onKeyDown(key,e);}
        public boolean onKeyUp(int key,KeyEvent e){if(key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER){h.details(t(R.string.hb_fs_details),detail.getText().toString());return true;}return super.onKeyUp(key,e);}
        public boolean onTouchEvent(MotionEvent e){if(e.getAction()==MotionEvent.ACTION_UP&&!hours.isEmpty()){float left=h.dp(32);int next=Math.max(0,Math.min(hours.size()-1,(int)((e.getX()-left)/Math.max(1,getWidth()-left-h.dp(8))*hours.size())));if(next==index)h.details(t(R.string.hb_fs_details),detail.getText().toString());else{index=next;updateDetail();requestFocus();}performClick();}return true;}
        public boolean performClick(){return super.performClick();}
    }
}
