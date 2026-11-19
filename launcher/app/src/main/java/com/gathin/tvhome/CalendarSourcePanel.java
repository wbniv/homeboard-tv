package com.gathin.tvhome;

import android.content.Context;
import android.view.*;
import android.widget.*;

/** Shared live source/refresh panel; lifecycle follows its attached view, not the Activity. */
final class CalendarSourcePanel extends LinearLayout {
    private final CalendarData data;
    private final CalendarWidgetSettings settings;
    private final TextView body;
    private final Button refresh;
    private final View wedge;
    private final ScrollView scroll;
    private final Runnable update=this::render;
    private boolean arrowDown=true;
    CalendarSourcePanel(Context c,CalendarData data,CalendarWidgetSettings settings,PaletteColors palette,int opacity,Runnable back){
        super(c);this.data=data;this.settings=settings;setOrientation(VERTICAL);int pad=dp(14);setPadding(pad,pad,pad,pad);
        UiBackgroundDrawable background=new UiBackgroundDrawable(palette.raised);background.setFillOpacity(opacity);background.setCornerRadius(dp(10));background.setStroke(dp(1),palette.edge);setBackground(background);
        TextView title=new TextView(c);title.setText(R.string.hb_data_title);title.setTextSize(22);title.setTextColor(palette.text);addView(title);
        scroll=new ScrollView(c);scroll.setVerticalScrollBarEnabled(false);scroll.setFocusable(true);
        body=new TextView(c);body.setTextColor(palette.text);body.setTextSize(17);body.setPadding(0,dp(10),0,dp(10));scroll.addView(body);
        addView(scroll,new LayoutParams(-1,0,1));
        wedge=new View(c){
            private final android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            @Override public CharSequence getAccessibilityClassName(){return Button.class.getName();}
            @Override protected void onDraw(android.graphics.Canvas canvas){
                super.onDraw(canvas);paint.setColor(palette.text);float x=getWidth()/2f,y=getHeight()/2f,half=dp(6),direction=arrowDown?1:-1;
                android.graphics.Path triangle=new android.graphics.Path();triangle.moveTo(x-half,y-direction*dp(3));triangle.lineTo(x+half,y-direction*dp(3));triangle.lineTo(x,y+direction*dp(4));triangle.close();canvas.drawPath(triangle,paint);
            }
        };
        wedge.setPadding(0,0,0,0);wedge.setMinimumHeight(0);wedge.setFocusable(true);wedge.setClickable(true);style(wedge,palette,opacity,false);
        wedge.setOnFocusChangeListener((v,focused)->style(wedge,palette,opacity,focused));wedge.setContentDescription(c.getString(R.string.hb_fs_next));
        wedge.setOnClickListener(v->{boolean down=scroll.canScrollVertically(1);scroll.smoothScrollBy(0,(down?1:-1)*Math.max(dp(80),scroll.getHeight()*4/5));});
        addView(wedge,new LayoutParams(-1,dp(28)));
        scroll.setOnScrollChangeListener((v,x,y,oldX,oldY)->arrows());
        LinearLayout actions=new LinearLayout(c);refresh=button(c.getString(R.string.hb_data_refresh),palette,opacity);
        refresh.setOnClickListener(v->data.refresh(true,null));actions.addView(refresh,new LayoutParams(0,dp(44),1));
        Button close=button(c.getString(R.string.hb_fs_back),palette,opacity);close.setOnClickListener(v->back.run());actions.addView(close,new LayoutParams(0,dp(44),1));addView(actions);
        render();close.requestFocus();
    }
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private Button button(String text,PaletteColors palette,int opacity){
        Button button=new Button(getContext());button.setText(text);button.setAllCaps(false);button.setTextSize(14);button.setTextColor(palette.text);
        button.setMinWidth(0);button.setMinimumWidth(0);button.setOnFocusChangeListener((v,focused)->style(button,palette,opacity,focused));style(button,palette,opacity,false);return button;
    }
    private void style(View button,PaletteColors palette,int opacity,boolean focused){UiBackgroundDrawable background=new UiBackgroundDrawable(palette.raised);background.setFillOpacity(opacity);background.setCornerRadius(dp(6));background.setStroke(dp(focused?2:1),focused?palette.text:palette.edge);button.setBackground(background);}
    private void render(){body.setText(data.describe(getContext(),settings));refresh.setText(data.busy?R.string.hb_data_refresh_short:R.string.hb_data_refresh);scroll.post(this::arrows);}
    private void arrows(){boolean down=scroll.canScrollVertically(1),up=scroll.canScrollVertically(-1);wedge.setVisibility(down||up?VISIBLE:INVISIBLE);arrowDown=down;wedge.invalidate();wedge.setContentDescription(getContext().getString(down?R.string.hb_fs_next:R.string.hb_fs_previous));}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();data.listen(update);render();}
    @Override protected void onDetachedFromWindow(){data.unlisten(update);super.onDetachedFromWindow();}
}
