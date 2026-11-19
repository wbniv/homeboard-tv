package com.gathin.tvhome;
import android.content.Context;
import android.os.*;
import android.view.View;
import android.widget.*;
import org.json.*;

/** Glance only: OK consistently opens full screen, even at 1x1. */
final class KitchenTimerWidgetView extends LinearLayout {
    private final TextView content;private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable tick=new Runnable(){public void run(){refresh();handler.postDelayed(this,1000);}};
    KitchenTimerWidgetView(Context c,PaletteColors palette,int opacity){super(c);setOrientation(VERTICAL);int pad=(int)(10*c.getResources().getDisplayMetrics().density);setPadding(pad,pad,pad,pad);UiBackgroundDrawable bg=new UiBackgroundDrawable(palette.surface);bg.setFillOpacity(opacity);bg.setCornerRadius(pad);setBackground(bg);setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        TextView title=new TextView(c);title.setText(R.string.hb_timer_title);title.setTextSize(19);title.setTextColor(palette.text);title.setSingleLine();addView(title);content=new TextView(c);content.setTextSize(22);content.setTextColor(palette.text);addView(content,new LayoutParams(-1,0,1));}
    private void refresh(){JSONObject snapshot=KitchenTimerStore.snapshot(getContext());var items=KitchenTimerUi.ordered(snapshot.optJSONArray("items"));int lines=Math.max(1,content.getHeight()/Math.max(1,content.getLineHeight()));StringBuilder text=new StringBuilder();int count=Math.min(items.size(),Math.max(1,(lines-1)/2));
        for(int i=0;i<count;i++){JSONObject t=items.get(i);if(i>0)text.append("\n");text.append(KitchenTimerUi.name(getContext(),t)).append("\n").append(KitchenTimerUi.detail(getContext(),t));}
        if(items.isEmpty())text.append(getContext().getString(R.string.hb_timer_empty));else if(items.size()>count&&lines>=3)text.append("\n").append(getContext().getString(R.string.hb_timer_more,items.size()-count));content.setMaxLines(lines);content.setText(text);setContentDescription(getContext().getString(R.string.hb_timer_title)+" · "+text);}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);post(this::refresh);}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();handler.post(tick);}
    @Override protected void onDetachedFromWindow(){handler.removeCallbacks(tick);super.onDetachedFromWindow();}
}
