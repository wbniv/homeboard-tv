package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

/** Flat tinted city cards shared by compact and expanded native presentations. */
final class WorldClockCards {
    private final Context context;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint text=new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final PaletteColors palette;
    private final Map<String,Float> hues;
    private final Map<String,Integer> colors=new java.util.HashMap<>();
    WorldClockCards(Context c,WorldClockSettings settings,PaletteColors palette) { context=c; this.palette=palette; hues=WorldClockModel.hues(settings); }
    int color(WorldClockSettings.Place p) {
        Integer saved=colors.get(p.id);if(saved!=null)return saved;
        Float hue=hues.get(p.id);int base=hue==null?palette.focus:Color.HSVToColor(new float[]{hue,.45f,.94f});
        for(float amount=0;amount<=1.01f;amount+=.1f){int candidate=mix(base,palette.text,Math.min(1,amount));if(androidx.core.graphics.ColorUtils.calculateContrast(candidate,palette.surface)>=4.5){colors.put(p.id,candidate);return candidate;}}
        return palette.text;
    }
    static int mix(int base,int accent,float amount) {
        return Color.rgb(Math.round(Color.red(base)*(1-amount)+Color.red(accent)*amount),Math.round(Color.green(base)*(1-amount)+Color.green(accent)*amount),Math.round(Color.blue(base)*(1-amount)+Color.blue(accent)*amount));
    }
    void text(Canvas c,String value,float x,float baseline,float size,int color,float width,boolean mono) {
        text.setTextSize(size); text.setColor(color); text.setTypeface(mono?Typeface.MONOSPACE:Typeface.DEFAULT);
        c.drawText(TextUtils.ellipsize(value,text,Math.max(0,width),TextUtils.TruncateAt.END).toString(),x,baseline,text);
    }
    void header(Canvas c,WorldClockSettings s,WorldClockSettings.Place p,Instant at,float width,float height,int opacity) {
        int accent=color(p);paint.setStyle(Paint.Style.FILL);paint.setColor(mix(palette.surface,accent,.38f));paint.setAlpha(255*opacity/100);
        c.drawRect(1,1,width-1,height-1,paint);paint.setAlpha(255);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1);paint.setColor(accent);
        c.drawRect(1,1,width-1,height-1,paint);paint.setStyle(Paint.Style.FILL);
        float scale=context.getResources().getConfiguration().fontScale;
        text(c,WorldClockLabels.name(context,p).toUpperCase(java.util.Locale.getDefault()),7,scale>1.3f?20:14,11*scale,palette.text,width-14,false);
        ZoneId zone=p.zone(ZoneId.systemDefault());
        String country=WorldClockLabels.country(context,p);
        if(zone!=null){String day=WorldClockLabels.day(context,at,zone,s.reference(ZoneId.systemDefault()));if(!day.isEmpty())country+=" · "+day;}
        if(scale<=1.3f)text(c,country,7,26,8*scale,palette.text,width-14,false);
        if(zone==null){text(c,context.getString(R.string.hb_wc_zone_unavailable),7,48,10*scale,palette.text,width-14,false);return;}
        String time=WorldClockLabels.time(context,s,at,zone,false);float size=Math.min(26,24*scale),available=Math.max(1,width-39);
        text.setTypeface(Typeface.MONOSPACE);text.setTextSize(size);float measured=text.measureText(time);if(measured>available)size*=available/measured;
        text(c,time,7,50,size,palette.text,available,true);
        text(c,WorldClockModel.daytime(at,zone)?"☀":"☾",width-28,49,16,palette.text,26,false);
    }
    void draw(Canvas c,WorldClockSettings s,WorldClockSettings.Place p,Instant at,float width,float height,boolean selected,int opacity) {
        int accent=color(p); paint.setStyle(Paint.Style.FILL); paint.setColor(mix(palette.surface,accent,.38f));paint.setAlpha(255*opacity/100);
        c.drawRect(1,1,width-1,height-1,paint);paint.setAlpha(255);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(selected?2:1);paint.setColor(selected?palette.text:mix(palette.surface,accent,.5f));
        c.drawRect(1,1,width-1,height-1,paint);paint.setStyle(Paint.Style.FILL);
        float scale=context.getResources().getConfiguration().fontScale,pad=8,usable=width-16;
        boolean narrow=width<125*scale,shortCard=height<92*scale;
        String name=narrow&&!p.code.isEmpty()?p.code:WorldClockLabels.name(context,p).toUpperCase(java.util.Locale.getDefault());
        text(c,name,pad,Math.min(height*.23f,22*scale),14*scale,accent,usable,false);
        ZoneId zone=p.zone(ZoneId.systemDefault());
        if(zone==null) { text(c,context.getString(R.string.hb_wc_zone_unavailable),pad,height*.60f,12*scale,palette.text,usable,false); return; }
        float size=Math.min((shortCard?28:34)*scale,Math.min((usable-(narrow?0:22))/(WorldClockLabels.twelve(context,s)?5.4f:3.5f),height*.29f));
        String time=WorldClockLabels.time(context,s,at,zone,false);
        text(c,time,pad,height*(shortCard?.61f:.56f),size,palette.text,usable-(narrow?0:22),true);
        if(!narrow) text(c,WorldClockModel.daytime(at,zone)?"☀":"☾",width-27,height*(shortCard?.60f:.55f),18*scale,palette.text,22,false);
        String date=WorldClockLabels.date(context,at,zone),day=WorldClockLabels.day(context,at,zone,s.reference(ZoneId.systemDefault()));
        if(!day.isEmpty()) date+=" · "+day;
        text(c,date,pad,height-(shortCard?8:29)*scale,10*scale,palette.muted,usable,false);
        if(!shortCard) {
            if(!p.country.isEmpty()) text(c,WorldClockLabels.country(context,p),pad,Math.min(height*.33f,39*scale),10*scale,palette.muted,usable,false);
            String offset=WorldClockModel.offset(at,zone);text.setTextSize(10*scale);text.setTypeface(Typeface.MONOSPACE);
            paint.setColor(mix(palette.surface,accent,.18f));c.drawRect(pad-3,height-22*scale,Math.min(width-pad,pad+text.measureText(offset)+4),height-4,paint);
            text(c,offset,pad,height-9,10*scale,palette.text,usable,true);
        }
    }
}
