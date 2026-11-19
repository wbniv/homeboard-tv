package com.gathin.tvhome;
import android.content.Context;
import android.os.*;
import android.view.View;
import android.widget.*;
import org.json.*;

/** Glance only: OK consistently opens full screen, even at 1x1. */
final class KitchenTimerWidgetView extends LinearLayout {
    private final TextView content,title;private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable tick=new Runnable(){public void run(){refresh();handler.postDelayed(this,1000);}};
    KitchenTimerWidgetView(Context c,PaletteColors palette,int opacity){super(c);setOrientation(VERTICAL);int pad=(int)(10*c.getResources().getDisplayMetrics().density);setPadding(pad,pad,pad,pad);UiBackgroundDrawable bg=new UiBackgroundDrawable(palette.surface);bg.setFillOpacity(opacity);bg.setCornerRadius(pad);setBackground(bg);setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        title=new TextView(c);title.setText(R.string.hb_timer_title);title.setTextSize(19);title.setTextColor(palette.text);title.setSingleLine();addView(title);content=new TextView(c);content.setTextSize(22);content.setTextColor(palette.text);addView(content,new LayoutParams(-1,0,1));}
    private void refresh(){
        if(getHeight()==0)return;
        JSONObject snapshot=KitchenTimerStore.snapshot(getContext());var items=KitchenTimerUi.ordered(snapshot.optJSONArray("items"));
        int available=getHeight()-getPaddingTop()-getPaddingBottom(),line=Math.max(1,content.getLineHeight());
        boolean headingFits=available>=title.getLineHeight()+2*line;
        title.setVisibility(headingFits?VISIBLE:GONE);
        int lines=Math.max(1,(available-(headingFits?title.getLineHeight():0))/line);
        StringBuilder text=new StringBuilder();int count=Math.min(items.size(),Math.max(1,(lines-1)/2));
        for(int i=0;i<count;i++){
            JSONObject t=items.get(i);String value=glance(t);if(i>0)text.append("\n");
            if(lines>=2)text.append(KitchenTimerUi.name(getContext(),t)).append("\n");
            text.append(value);
        }
        if(items.isEmpty())text.append(getContext().getString(R.string.hb_timer_empty));else if(items.size()>count&&lines>=3)text.append("\n").append(getContext().getString(R.string.hb_timer_more,items.size()-count));
        content.setMaxLines(lines);content.setText(text);StringBuilder description=new StringBuilder(getContext().getString(R.string.hb_timer_title));for(JSONObject item:items)description.append(" · ").append(KitchenTimerUi.name(getContext(),item)).append(" · ").append(KitchenTimerUi.detail(getContext(),item));setContentDescription(description);
    }
    private String glance(JSONObject t){
        String state=t.optString("state");
        if("done".equals(state))return getContext().getString(R.string.hb_timer_finished);
        if("timer".equals(t.optString("kind"))&&("running".equals(state)||"paused".equals(state)))return ("paused".equals(state)?"⏸︎ ":DisplayTime.STOPWATCH)+KitchenTimers.clock(KitchenTimers.remaining(t,SystemClock.elapsedRealtime(),System.currentTimeMillis()));
        if("event".equals(t.optString("kind"))&&"running".equals(state))return DisplayTime.timestamp(getContext(),java.time.Instant.ofEpochMilli(t.optLong("at")),java.time.ZoneId.systemDefault(),"HH:mm");
        return getContext().getString(KitchenTimerUi.state(state));
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);post(this::refresh);}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();handler.post(tick);}
    @Override protected void onDetachedFromWindow(){handler.removeCallbacks(tick);super.onDetachedFromWindow();}
}
