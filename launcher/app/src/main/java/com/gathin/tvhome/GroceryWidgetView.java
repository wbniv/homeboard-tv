package com.gathin.tvhome;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.View;
import org.json.*;

/** Noninteractive glance; the entire Home widget is one activation target. */
final class GroceryWidgetView extends LinearLayout {
    private final JSONObject list;private final TextView content;
    GroceryWidgetView(Context c,JSONObject list,PaletteColors palette,int opacity){
        super(c);this.list=list;setOrientation(VERTICAL);int pad=(int)(10*c.getResources().getDisplayMetrics().density);setPadding(pad,pad,pad,pad);
        UiBackgroundDrawable bg=new UiBackgroundDrawable(palette.surface);bg.setFillOpacity(opacity);bg.setCornerRadius(pad);setBackground(bg);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        String title=list==null?c.getString(R.string.hb_grocery_unavailable):list.optString("title");if(title.isEmpty())title=c.getString(R.string.hb_grocery_title);
        TextView heading=new TextView(c);heading.setText(title);heading.setTextColor(palette.text);heading.setTextSize(20);heading.setSingleLine(true);addView(heading);
        content=new TextView(c);content.setTextColor(palette.text);content.setTextSize(17);
        JSONArray items=list==null?new JSONArray():list.optJSONArray("items");StringBuilder text=new StringBuilder();
        if(list==null)text.append(c.getString(R.string.hb_grocery_unavailable));
        else if(items.length()==0)text.append(c.getString(R.string.hb_grocery_empty));
        else {text.append(c.getString(R.string.hb_grocery_count,items.length()));for(int i=0;i<items.length();i++)text.append("\n").append(GroceryData.text(items.optJSONObject(i)));}
        content.setText(text);content.setEllipsize(android.text.TextUtils.TruncateAt.END);addView(content,new LayoutParams(-1,0,1));
        setContentDescription(title+" · "+(list==null?c.getString(R.string.hb_grocery_unavailable):c.getString(R.string.hb_grocery_count,items.length())));
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);post(this::fit);}
    private void fit(){
        if(list==null)return;JSONArray items=list.optJSONArray("items");if(items.length()==0)return;
        int lines=Math.max(1,content.getHeight()/Math.max(1,content.getLineHeight()));
        StringBuilder text=new StringBuilder(getContext().getString(R.string.hb_grocery_count,items.length()));
        int count=Math.max(0,Math.min(items.length(),lines-1-(items.length()>lines-1?1:0)));
        for(int i=0;i<count;i++)text.append("\n").append(GroceryData.text(items.optJSONObject(i)));
        if(items.length()>count&&lines>1)text.append("\n").append(getContext().getString(R.string.hb_grocery_more,items.length()-count));
        content.setMaxLines(lines);content.setText(text);
    }
}
