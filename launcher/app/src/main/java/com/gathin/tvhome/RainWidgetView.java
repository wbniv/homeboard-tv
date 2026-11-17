package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;
import java.time.Instant;
import java.util.List;

/** Reflows from measured space; text uses sp, never scales down with the artwork. */
final class RainWidgetView extends View {
    private final RainWidgetController controller;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint text=new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private PaletteColors palette=PaletteColors.from(0xff101824);
    private int opacity=75;
    private boolean running,reducedMotion;
    private Instant now=Instant.now();
    private RainForecast.Period period;
    private List<RainForecast.Period> days=java.util.Collections.emptyList();
    private final Runnable frame=()->{ invalidate(); };
    RainWidgetView(Context context,RainWidgetController controller) { super(context);this.controller=controller;refresh(); }
    void setStyle(PaletteColors palette,int opacity,boolean reducedMotion) {this.palette=palette;this.opacity=opacity;this.reducedMotion=reducedMotion;invalidate();}
    void setRunning(boolean value) {running=value;removeCallbacks(frame);invalidate();}
    void refresh() {
        now=Instant.now();RainSettings s=controller.settings();RainForecast f=controller.forecast;
        period=f==null?null:f.hourly(now,s.hours);days=f==null?java.util.Collections.emptyList():f.following(now,s.hours,s.days);
        setContentDescription(s.displayName()+". "+controller.status()+". "+(period==null?"":controller.headline(period)+". "+controller.s(R.string.hb_rain_window,s.hours)+". "+controller.amount(period.mm)+". "+controller.s(R.string.hb_rain_peak,controller.chance(period.chance)))+". "+controller.actionHint());
        invalidate();
    }
    private float dp(float v){return v*getResources().getDisplayMetrics().density;}
    private float sp(float v){return v*getResources().getDisplayMetrics().scaledDensity;}
    private float line(){return sp(22);}
    private boolean fits(String value,float width,float size){text.setTextSize(sp(size));return text.measureText(value)<=width;}
    private void drawText(Canvas c,String value,float x,float y,float width,float size,int color,boolean bold,boolean shorten){
        if(y-sp(size)<0||y>getHeight()-dp(5)||width<=0)return;
        text.setColor(color);text.setTextSize(sp(size));text.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);
        if(!shorten&&text.measureText(value)>width)return;
        c.drawText(shorten?TextUtils.ellipsize(value,text,width,TextUtils.TruncateAt.END).toString():value,x,y,text);
    }
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float w=getWidth(),h=getHeight(),pad=dp(10),width=w-2*pad,l=line();
        if(width<=0||h<sp(18))return;
        paint.setStyle(Paint.Style.FILL);paint.setColor(palette.surface);paint.setAlpha(Color.alpha(palette.surface)*opacity/100);
        c.drawRoundRect(0,0,w,h,dp(14),dp(14),paint);paint.setAlpha(255);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(isFocused()?3:1));paint.setColor(isFocused()?palette.focus:palette.edge);
        c.drawRoundRect(dp(2),dp(2),w-dp(2),h-dp(2),dp(13),dp(13),paint);paint.setStyle(Paint.Style.FILL);
        RainSettings s=controller.settings();String status=controller.status();
        if (period != null && !period.hours.isEmpty() && h <= dp(240)
                && drawHourlyRibbon(c, w, h)) { scheduleFrame(); return; }
        boolean tiny=width<sp(170)||h<l*3+2*pad;
        if(tiny){
            String head=status.isEmpty()?controller.s(R.string.hb_rain_rain):status;
            drawText(c,head,pad,pad+sp(18),width-dp(24),18,palette.text,true,true);
            cloud(c,w-pad-dp(8),pad+dp(10),dp(8));
            if(period!=null&&status.isEmpty()){
                RainForecast.Hour wet=period.firstWet();
                String cue=wet!=null?"~"+controller.time(wet.start,"HH:mm"):period.complete?controller.s(R.string.hb_rain_dry_short):controller.s(R.string.hb_rain_incomplete);
                drawText(c,cue,pad,pad+sp(18)+l,width,18,palette.text,true,false);
                if(h>l*4)drawText(c,controller.s(R.string.hb_rain_window,s.hours),pad,pad+sp(18)+l*2,width,16,palette.muted,false,false);
                if(h>l*7){drawText(c,s.displayName(),pad,pad+sp(18)+l*3,width,16,palette.muted,false,true);cloud(c,w/2,h-dp(45),Math.min(dp(27),width/3));}
            } else if(h>l*5)cloud(c,w/2,h-dp(40),Math.min(dp(27),width/3));
            scheduleFrame();return;
        }
        float y=pad+sp(16);
        drawText(c,s.displayName(),pad,y,width-dp(58),16,palette.muted,false,true);
        cloud(c,w-pad-dp(28),pad+dp(29),dp(23));y+=l;
        String headline=period==null?status:controller.headline(period);
        drawText(c,status.isEmpty()||period==null?headline:status+" · "+headline,pad,y,width-dp(55),18,palette.text,true,true);y+=l;
        if(period==null){drawText(c,controller.actionHint(),pad,y,width,16,palette.muted,false,true);return;}
        String total=controller.s(R.string.hb_rain_window,s.hours)+" · "+controller.amount(period.mm);
        String compact=controller.s(R.string.hb_rain_compact_summary,s.hours,controller.amount(period.mm),controller.chance(period.chance));
        if(fits(compact,width,16)){drawText(c,compact,pad,y,width,16,palette.text,false,false);y+=l;}
        else{
            drawText(c,total,pad,y,width,16,palette.text,false,false);y+=l;
            String peak=controller.s(R.string.hb_rain_peak,controller.chance(period.chance));
            drawText(c,peak,pad,y,width,16,palette.text,false,false);y+=l;
        }
        if(period.snowCm>0){drawText(c,controller.s(R.string.hb_rain_snow),pad,y,width,16,palette.text,false,true);y+=l;}
        // Keep daily figures legible: choose rows at wide sizes and a two-by-three grid otherwise.
        float dayCellWidth=width/3;
        boolean dayGrid=!days.isEmpty()&&width>=sp(280)&&h-y>=l*(5+2*Math.ceil(days.size()/3.0))&&fits("28/11 12:00",dayCellWidth-dp(6),16);
        boolean side=!days.isEmpty()&&width>=sp(690)&&h-y>=l*9;
        float chartWidth=side?width*.58f:width;
        float dayHeight=dayGrid?(float)(l*(1+2*Math.ceil(days.size()/3.0))):0;
        float footerHeight=l;
        // The chart's last time label sits on its bottom baseline. Reserve a
        // separate line before daily rows or attribution begin.
        float chartHeight=Math.min(dp(130),h-y-footerHeight-l-(side?0:dayHeight));
        if(chartWidth>=period.hours.size()*dp(4)&&chartHeight>=l*3){
            chart(c,pad,y,chartWidth,chartHeight);y+=chartHeight+l;
        }else if(width<sp(300)&&h-y>=l*5){
            for(int i=0;i<Math.min(3,period.hours.size());i++){
                RainForecast.Hour hour=period.hours.get(i);
                drawText(c,controller.time(hour.start,"HH:mm")+"  "+controller.amount(hour.mm)+"  "+controller.chance(hour.chance),pad,y,width,16,palette.text,false,false);y+=l;
            }
            drawText(c,controller.s(R.string.hb_rain_hours_preview,3,s.hours),pad,y,width,16,palette.muted,false,false);y+=l;
        }
        if(side){drawDays(c,pad+width*.62f,pad+sp(16)+l*4,width*.38f,true);}
        else if(dayGrid){drawDays(c,pad,y,width,false);y+=dayHeight;}
        else if(!days.isEmpty()&&y+l<h-pad){drawText(c,controller.s(R.string.hb_rain_following,s.days)+" · OK",pad,y,width,16,palette.muted,false,false);y+=l;}
        if(y+l<h-pad){drawText(c,"Open-Meteo · "+controller.s(R.string.hb_rain_updated,controller.time(controller.fetchedAt/1000,"HH:mm")),pad,y,width,16,palette.muted,false,true);}
        scheduleFrame();
    }
    /** Shallow wide cards prioritize every hour, independently of the larger card's headings. */
    private boolean drawHourlyRibbon(Canvas c, float w, float h) {
        float pad=dp(6), width=w-pad*2, size=14, line=sp(16);
        text.setTextSize(sp(size));
        float minimumPitch=Math.max(text.measureText("100"),text.measureText("00"))+dp(4);
        int count=period.hours.size();
        int columns=width/count>=minimumPitch?count:Math.min(12,count);
        if(width/columns<minimumPitch)return false;
        int rows=(count+columns-1)/columns;
        float rowHeight=sp(34)+dp(10), required=pad*2+line+rows*rowHeight+(!days.isEmpty()?line:0);
        if(h<required)return false;
        double max=.5;
        for(RainForecast.Hour hour:period.hours)if(Double.isFinite(hour.mm))max=Math.max(max,hour.mm);
        RainSettings settings=controller.settings();
        String heading=(controller.status().isEmpty()?"":controller.status()+" · ")+controller.s(R.string.hb_rain_window,settings.hours)+" · 0–"+controller.amount(max)+"/h"
                +" · "+controller.amount(period.mm)+" · "+controller.s(R.string.hb_fs_chance)+" %"
                +" · "+controller.time(period.start,"dd/MM HH")+"–"+controller.time(period.end,"dd/MM HH z")
                +" · "+settings.displayName()+" · Open-Meteo · "+controller.s(R.string.hb_rain_updated,controller.time(controller.fetchedAt/1000,"HH:mm"));
        drawText(c,heading,pad,pad+sp(14),width-dp(24),size,palette.muted,false,true);
        cloud(c,w-pad-dp(10),pad+dp(8),dp(7));
        for(int row=0;row<rows;row++) {
            float top=pad+line+row*rowHeight, bottom=top+dp(10), pitch=width/columns;
            for(int col=0;col<columns;col++) {
                int i=row*columns+col;if(i>=count)break;
                RainForecast.Hour hour=period.hours.get(i);
                float left=pad+col*pitch, right=left+pitch-dp(3);
                paint.setColor(palette.edge);c.drawLine(left,bottom,right,bottom,paint);
                if(Double.isFinite(hour.mm)) {
                    paint.setColor(0xff78c5fa);
                    if(hour.mm>0)c.drawRect(left,bottom-(float)(hour.mm/max)*dp(10),right,bottom,paint);
                } else {paint.setColor(palette.muted);c.drawLine(left,bottom-dp(6),right,bottom-dp(2),paint);}
                drawText(c,controller.time(hour.start,"HH"),left,bottom+sp(15),pitch,size,palette.text,false,false);
                String chance=Double.isFinite(hour.chance)?String.format(java.util.Locale.getDefault(),"%.0f",hour.chance):"—";
                drawText(c,chance,left,bottom+sp(31),pitch,size,0xffc6b5f1,false,false);
                if(controller.time(hour.start,"HH").equals("00")) {
                    paint.setColor(palette.muted);c.drawLine(left-dp(2),top,left-dp(2),bottom+sp(31),paint);
                }
            }
        }
        if(!days.isEmpty()) {
            float y=pad+line+rows*rowHeight+sp(14), pitch=width/days.size();
            for(int i=0;i<days.size();i++) {
                RainForecast.Period day=days.get(i);
                String label=controller.time(day.start,"EEE")+" "+controller.amount(day.mm)+" "+controller.chance(day.chance);
                if(!fits(label,pitch-dp(4),size))label=controller.time(day.start,"EEE")+" "+controller.chance(day.chance);
                drawText(c,label,pad+i*pitch,y,pitch-dp(4),size,palette.muted,false,true);
            }
        }
        return true;
    }
    private void drawDays(Canvas c,float x,float y,float width,boolean rows){
        float l=line();drawText(c,controller.s(R.string.hb_rain_following,days.size()),x,y,width,16,palette.muted,false,false);y+=l;
        for(int i=0;i<days.size();i++){
            RainForecast.Period p=days.get(i);float cw=rows?width:width/3;
            float px=x+(rows?0:i%3*cw),py=y+(rows?i*l*1.4f:(i/3)*l*2);
            String date=controller.time(p.start,"EEE dd/MM");
            if(!controller.time(p.start,"HH:mm").equals("00:00"))date=controller.time(p.start,"dd/MM HH:mm");
            if(rows){drawText(c,date+"  "+controller.amount(p.mm)+"  "+controller.chance(p.chance),px,py,cw-dp(6),16,palette.text,false,false);}
            else{drawText(c,date,px,py,cw-dp(6),16,palette.text,false,false);drawText(c,controller.amount(p.mm)+" "+controller.chance(p.chance),px,py+l,cw-dp(6),16,palette.text,true,false);}
        }
    }
    private void chart(Canvas c,float x,float y,float w,float h){
        float l=line();String label=controller.s(R.string.hb_rain_window,controller.settings().hours);
        String combined=label+" · "+controller.s(R.string.hb_fs_chance)+" %";
        boolean probability=h>=l*4&&fits(combined,w,16);
        drawText(c,probability?combined:label,x,y,w,16,palette.muted,false,false);
        double max=.5;for(RainForecast.Hour hour:period.hours)if(Double.isFinite(hour.mm))max=Math.max(max,hour.mm);
        float top=y+l,bottom=y+h-l*(probability?2:1),pitch=w/period.hours.size();
        paint.setColor(palette.edge);paint.setStrokeWidth(dp(1));c.drawLine(x,bottom,x+w,bottom,paint);
        for(int i=0;i<period.hours.size();i++){
            RainForecast.Hour hour=period.hours.get(i);float left=x+i*pitch+dp(1),right=x+(i+1)*pitch-dp(1);
            if(!Double.isFinite(hour.mm)){paint.setColor(palette.muted);c.drawLine(left,bottom-dp(7),right,bottom-dp(2),paint);}
            else{paint.setColor(0xff78c5fa);float bar=(float)(hour.mm/max)*(bottom-top);if(bar>0)c.drawRect(left,bottom-bar,right,bottom,paint);}
            if(probability){paint.setColor(palette.edge);c.drawRect(left,bottom+l*.25f,right,bottom+l*.55f,paint);if(Double.isFinite(hour.chance)){paint.setColor(0xffb9a3ed);c.drawRect(left,bottom+l*.25f,(float)(left+(right-left)*hour.chance/100),bottom+l*.55f,paint);}}
        }
        drawText(c,controller.amount(max),x,top,w,16,palette.muted,false,false);
        int ticks=Math.max(2,Math.min(4,(int)(w/sp(95))));
        for(int i=0;i<ticks;i++){
            int index=i*period.hours.size()/ticks;float px=x+w*i/ticks;
            drawText(c,controller.time(period.hours.get(index).start,"HH:mm"),px,y+h,w/ticks,16,palette.muted,false,false);
        }
    }
    private void cloud(Canvas c,float x,float y,float r){
        boolean wet=period!=null&&period.firstWet()!=null;
        boolean moving=wet&&running&&!reducedMotion&&controller.settings().animation&&controller.fresh();
        float phase=moving?(SystemClock.uptimeMillis()%1800)/1800f:.35f;
        if(moving&&getWidth()>dp(220))x+=(float)Math.sin(phase*Math.PI*2)*dp(2);
        paint.setColor(palette.muted);c.drawRoundRect(x-r,y-r*.3f,x+r,y+r*.35f,r*.4f,r*.4f,paint);
        c.drawCircle(x-r*.3f,y-r*.35f,r*.5f,paint);c.drawCircle(x+r*.25f,y-r*.15f,r*.5f,paint);
        if(wet){
            double mm=period.firstWet().mm;int count=mm>=5?8:mm>=1?5:3;
            paint.setColor(0xff78c5fa);paint.setStrokeWidth(dp(2));
            for(int i=0;i<count;i++){float px=x-r*.8f+(i%4)*r*.48f;float py=y+r*.5f+((phase+i*.23f)%1)*r*.75f;c.drawLine(px,py,px-r*.12f,py+r*.21f,paint);}
        }
    }
    private void scheduleFrame(){
        removeCallbacks(frame);
        if(running&&!reducedMotion&&controller.settings().animation&&controller.fresh()&&period!=null&&period.firstWet()!=null&&isShown())postDelayed(frame,50);
    }
    @Override protected void onFocusChanged(boolean gain,int direction,android.graphics.Rect previous){super.onFocusChanged(gain,direction,previous);invalidate();}
    @Override protected void onDetachedFromWindow(){removeCallbacks(frame);super.onDetachedFromWindow();}
}
