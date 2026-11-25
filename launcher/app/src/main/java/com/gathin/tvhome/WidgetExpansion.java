package com.gathin.tvhome;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayDeque;

/** One native window, with a shared restore action and a nested detail stack. */
final class WidgetExpansion extends DashboardDialog {
    interface Content {
        View view();
        Bundle state();
        default void active(boolean active) { }
        default boolean configurable() { return true; }
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
    private final LinearLayout root;
    private final ImageButton configureButton;
    private final TextView hint;
    private final Bundle initialState;
    private Content content;
    private ViewTreeObserver.OnPreDrawListener firstDraw;
    private final Runnable mountContent = this::mountContent;
    private final ArrayDeque<View[]> stack = new ArrayDeque<>();
    private boolean running, released, configuring, openingFinished;
    WidgetExpansion(Activity activity, String key, String title, PaletteColors palette, int opacity,
                    int page, Bundle saved, Factory factory, Runnable configure, Runnable restored) {
        super(activity); this.activity=activity;this.widgetKey=key;this.palette=palette;this.opacity=opacity;
        originPage=saved==null?page:saved.getInt("originPage",page);this.factory=factory;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        root=column();root.setPadding(dp(16),dp(12),dp(16),dp(10));root.setBackgroundColor(palette.surface);
        LinearLayout header=row();
        header.addView(button(R.string.hb_fs_restore,this::dismiss));
        TextView heading=text(title,20);heading.setTypeface(null,android.graphics.Typeface.BOLD);
        header.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        configureButton=settingsButton(()->configure(configure));
        configureButton.setEnabled(false);header.addView(configureButton,new LinearLayout.LayoutParams(dp(48),dp(48)));
        root.addView(header,new LinearLayout.LayoutParams(-1,dp(48)));
        body=new FrameLayout(activity);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        hint=text(activity.getString(R.string.hb_fs_hint),13);root.addView(hint);
        setContentView(root);
        Bundle initial=saved==null?null:saved.getBundle("content");
        initialState=initial==null?new Bundle():new Bundle(initial);
        TextView loading=text(activity.getString(R.string.hb_loading),18);
        loading.setGravity(Gravity.CENTER);
        body.addView(loading,new FrameLayout.LayoutParams(-1,-1));
        setOnDismissListener(d->{releaseContent();restored.run();});
        setOnKeyListener((d,keyCode,event)->{
            if(keyCode==KeyEvent.KEYCODE_BACK||keyCode==KeyEvent.KEYCODE_ESCAPE){
                if(event.getAction()==KeyEvent.ACTION_UP)back();return true;
            }return false;
        });
    }
    void open(View source, boolean animate) {
        if(released)return;show();
        if(getWindow()!=null){getWindow().setWindowAnimations(0);getWindow().setBackgroundDrawableResource(android.R.color.transparent);setDashboardLayout(-1,-1);}
        setRunning(true);
        if(content!=null||firstDraw!=null||openingFinished)return;
        android.graphics.Rect origin=new android.graphics.Rect();
        boolean anchored=source!=null&&source.getGlobalVisibleRect(origin);
        // Let the shell draw before creating potentially expensive widget views.
        firstDraw=()->{
            root.getViewTreeObserver().removeOnPreDrawListener(firstDraw);firstDraw=null;
            if(released)return true;
            if(animate){
                int[] location=new int[2];root.getLocationOnScreen(location);
                root.setPivotX(anchored?origin.exactCenterX()-location[0]:root.getWidth()/2f);
                root.setPivotY(anchored?origin.exactCenterY()-location[1]:root.getHeight()/2f);
                root.setScaleX(.92f);root.setScaleY(.92f);
                root.animate().scaleX(1f).scaleY(1f).setDuration(140)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .withEndAction(this::finishOpening).start();
            }else finishOpening();
            return true;
        };
        root.getViewTreeObserver().addOnPreDrawListener(firstDraw);
    }
    private void finishOpening(){openingFinished=true;root.post(mountContent);}
    private void mountContent(){
        if(released||!running||!isShowing()||content!=null)return;
        content=factory.create(this,new Bundle(initialState));
        body.removeAllViews();body.addView(content.view(),new FrameLayout.LayoutParams(-1,-1));
        configureButton.setEnabled(true);configureButton.setVisibility(content.configurable()?View.VISIBLE:View.GONE);content.active(running);
        content.view().post(()->{if(!released&&content.view().findFocus()==null)content.view().requestFocus();});
    }
    void configure(Runnable action){configuring=true;setRunning(false);hide();action.run();}
    void resumeAfterConfiguration() {
        if(!configuring||released)return;configuring=false;
        Bundle state=content.state();content.close();body.removeAllViews();stack.clear();
        content=factory.create(this,state);body.addView(content.view(),new FrameLayout.LayoutParams(-1,-1));open(null,false);
    }
    boolean configuring(){return configuring;}
    void setRunning(boolean value){running=value&&!configuring&&!released;if(content!=null)content.active(running);else if(running&&openingFinished)root.post(mountContent);}
    private void back(){
        if(!stack.isEmpty()){
            View[] prior=stack.pop();body.removeAllViews();body.addView(prior[0],new FrameLayout.LayoutParams(-1,-1));
            hint.setVisibility(prior[0] instanceof CalendarSourcePanel?View.GONE:View.VISIBLE);
            if(prior[1]!=null)prior[1].requestFocus();else prior[0].requestFocus();
        }else if(content==null||!content.back())dismiss();
    }
    void details(String title,String detail){
        View old=body.getChildAt(0);stack.push(new View[]{old,old.findFocus()});body.removeAllViews();
        LinearLayout panel=column();panel.addView(text(title,22));
        ScrollView scroll=new ScrollView(activity);TextView text=text(detail,18);text.setTextIsSelectable(true);scroll.addView(text);
        panel.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        Button back=button(R.string.hb_fs_back,this::back);panel.addView(back);body.addView(panel,new FrameLayout.LayoutParams(-1,-1));back.requestFocus();
    }
    void calendarSources(CalendarData data,CalendarWidgetSettings settings){
        View old=body.getChildAt(0);stack.push(new View[]{old,old.findFocus()});body.removeAllViews();
        body.addView(new CalendarSourcePanel(activity,data,settings,palette,opacity,this::back),new FrameLayout.LayoutParams(-1,-1));
        hint.setVisibility(View.GONE);
    }
    Bundle state(){Bundle b=new Bundle();b.putString("key",widgetKey);b.putInt("originPage",originPage);b.putBundle("content",content==null?new Bundle(initialState):content.state());return b;}
    private void releaseContent(){
        if(released)return;released=true;root.animate().cancel();root.removeCallbacks(mountContent);
        if(firstDraw!=null){root.getViewTreeObserver().removeOnPreDrawListener(firstDraw);firstDraw=null;}
        if(content!=null)content.close();
    }
    void release(){setOnDismissListener(null);releaseContent();dismiss();}
    int dp(float n){return Math.round(n*activity.getResources().getDisplayMetrics().density);}
    LinearLayout column(){LinearLayout v=new LinearLayout(activity);v.setOrientation(LinearLayout.VERTICAL);return v;}
    LinearLayout row(){LinearLayout v=new LinearLayout(activity);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    TextView text(String value,int size){TextView v=new TextView(activity);v.setText(value);v.setTextSize(size);v.setTextColor(palette.text);v.setPadding(dp(6),dp(3),dp(6),dp(3));return v;}
    Button button(int label,Runnable run){return button(activity.getString(label),run);}
    private ImageButton settingsButton(Runnable run){
        ImageButton button=new ImageButton(activity);
        String label=activity.getString(R.string.hb_fs_configure);
        button.setContentDescription(label);button.setTooltipText(label);
        button.setImageResource(R.drawable.ic_settings);
        button.setImageTintList(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},
                new int[]{PaletteColors.mix(palette.surface,palette.text,.35f),palette.text}));
        button.setScaleType(ImageView.ScaleType.FIT_CENTER);
        button.setPadding(dp(12),dp(12),dp(12),dp(12));
        button.setFocusable(true);button.setClickable(true);
        button.setBackground(background(false));
        button.setOnFocusChangeListener((v,focused)->button.setBackground(background(focused)));
        button.setOnClickListener(v->run.run());
        return button;
    }
    Button button(String label,Runnable run){Button b=new Button(activity);b.setText(label);b.setTextSize(14);b.setTextColor(palette.text);b.setAllCaps(false);b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(dp(10),0,dp(10),0);b.setBackground(background(false));b.setOnFocusChangeListener((v,f)->b.setBackground(background(f)));b.setOnClickListener(v->run.run());return b;}
    android.graphics.drawable.Drawable background(boolean focused){UiBackgroundDrawable bg=new UiBackgroundDrawable(palette.raised);bg.setFillOpacity(opacity);bg.setCornerRadius(dp(6));bg.setStroke(dp(focused?2:1),focused?palette.text:palette.edge);return bg;}
    TextView detailPane(LinearLayout row,float weight){ScrollView scroll=new ScrollView(activity);TextView detail=text("",16);detail.setTextIsSelectable(true);scroll.addView(detail);row.addView(scroll,new LinearLayout.LayoutParams(0,-1,weight));return detail;}
}
