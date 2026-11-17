package com.gathin.tvhome;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayDeque;

/** One native window, with a shared restore action and a nested detail stack. */
final class WidgetExpansion extends Dialog {
    interface Content {
        View view();
        Bundle state();
        default void active(boolean active) { }
        default boolean back() { return false; }
        default void close() { active(false); }
    }
    interface Factory { Content create(WidgetExpansion host, Bundle state); }
    final Activity activity;
    final PaletteColors palette;
    final int opacity;
    final String widgetKey;
    final int originPage;
    private final Factory factory;
    private final FrameLayout body;
    private Content content;
    private final ArrayDeque<View[]> stack = new ArrayDeque<>();
    private boolean running, released, configuring;
    WidgetExpansion(Activity activity, String key, String title, PaletteColors palette, int opacity,
                    int page, Bundle saved, Factory factory, Runnable configure, Runnable restored) {
        super(activity); this.activity=activity;this.widgetKey=key;this.palette=palette;this.opacity=opacity;
        originPage=saved==null?page:saved.getInt("originPage",page);this.factory=factory;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root=column();root.setPadding(dp(16),dp(12),dp(16),dp(10));root.setBackgroundColor(palette.surface);
        LinearLayout header=row();
        header.addView(button(R.string.hb_fs_restore,this::dismiss));
        TextView heading=text(title,20);heading.setTypeface(null,android.graphics.Typeface.BOLD);
        header.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        header.addView(button(R.string.hb_fs_configure,()->{configuring=true;setRunning(false);hide();configure.run();}));
        root.addView(header,new LinearLayout.LayoutParams(-1,dp(48)));
        body=new FrameLayout(activity);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        root.addView(text(activity.getString(R.string.hb_fs_hint),13));
        setContentView(root);
        Bundle initial=saved==null?null:saved.getBundle("content");
        content=factory.create(this,initial==null?new Bundle():initial);
        body.addView(content.view(),new FrameLayout.LayoutParams(-1,-1));
        setOnDismissListener(d->{releaseContent();restored.run();});
        setOnKeyListener((d,keyCode,event)->{
            if(keyCode==KeyEvent.KEYCODE_BACK||keyCode==KeyEvent.KEYCODE_ESCAPE){
                if(event.getAction()==KeyEvent.ACTION_UP)back();return true;
            }return false;
        });
    }
    void open() {
        if(released)return;show();
        if(getWindow()!=null){getWindow().setBackgroundDrawableResource(android.R.color.transparent);getWindow().setLayout(-1,-1);}
        setRunning(true);content.view().post(()->{if(content.view().findFocus()==null)content.view().requestFocus();});
    }
    void resumeAfterConfiguration() {
        if(!configuring||released)return;configuring=false;
        Bundle state=content.state();content.close();body.removeAllViews();stack.clear();
        content=factory.create(this,state);body.addView(content.view(),new FrameLayout.LayoutParams(-1,-1));open();
    }
    boolean configuring(){return configuring;}
    void setRunning(boolean value){running=value&&!configuring&&!released;content.active(running);}
    private void back(){
        if(!stack.isEmpty()){
            View[] prior=stack.pop();body.removeAllViews();body.addView(prior[0],new FrameLayout.LayoutParams(-1,-1));
            if(prior[1]!=null)prior[1].requestFocus();else prior[0].requestFocus();
        }else if(!content.back())dismiss();
    }
    void details(String title,String detail){
        View old=body.getChildAt(0);stack.push(new View[]{old,old.findFocus()});body.removeAllViews();
        LinearLayout panel=column();panel.addView(text(title,22));
        ScrollView scroll=new ScrollView(activity);TextView text=text(detail,18);text.setTextIsSelectable(true);scroll.addView(text);
        panel.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        Button back=button(R.string.hb_fs_back,this::back);panel.addView(back);body.addView(panel,new FrameLayout.LayoutParams(-1,-1));back.requestFocus();
    }
    Bundle state(){Bundle b=new Bundle();b.putString("key",widgetKey);b.putInt("originPage",originPage);b.putBundle("content",content.state());return b;}
    private void releaseContent(){if(released)return;released=true;content.close();}
    void release(){setOnDismissListener(null);releaseContent();dismiss();}
    int dp(float n){return Math.round(n*activity.getResources().getDisplayMetrics().density);}
    LinearLayout column(){LinearLayout v=new LinearLayout(activity);v.setOrientation(LinearLayout.VERTICAL);return v;}
    LinearLayout row(){LinearLayout v=new LinearLayout(activity);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    TextView text(String value,int size){TextView v=new TextView(activity);v.setText(value);v.setTextSize(size);v.setTextColor(palette.text);v.setPadding(dp(6),dp(3),dp(6),dp(3));return v;}
    Button button(int label,Runnable run){return button(activity.getString(label),run);}
    Button button(String label,Runnable run){Button b=new Button(activity);b.setText(label);b.setTextSize(14);b.setTextColor(palette.text);b.setAllCaps(false);b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(dp(10),0,dp(10),0);b.setBackground(background(false));b.setOnFocusChangeListener((v,f)->b.setBackground(background(f)));b.setOnClickListener(v->run.run());return b;}
    android.graphics.drawable.Drawable background(boolean focused){UiBackgroundDrawable bg=new UiBackgroundDrawable(palette.raised);bg.setFillOpacity(opacity);bg.setCornerRadius(dp(6));bg.setStroke(dp(focused?2:1),focused?palette.text:palette.edge);return bg;}
    TextView detailPane(LinearLayout row,float weight){ScrollView scroll=new ScrollView(activity);TextView detail=text("",16);detail.setTextIsSelectable(true);scroll.addView(detail);row.addView(scroll,new LinearLayout.LayoutParams(0,-1,weight));return detail;}
}
