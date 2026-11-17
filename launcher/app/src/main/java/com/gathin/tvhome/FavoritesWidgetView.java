package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.GridLayoutManager;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Consumer;

/** Fixed outer rectangle; only this RecyclerView scrolls. Wedges are passive and never take focus. */
final class FavoritesWidgetView extends FrameLayout {
    private final List<String> keys;
    private final Function<String,View> artwork;
    private final Consumer<String> focused;
    private final Runnable activate, actions;
    private final PaletteColors palette;
    private final boolean banners, reducedMotion, ribbon;
    private final int opacity;
    private final TextView header;
    private final RecyclerView list;
    private final Wedges wedges;
    private final Adapter adapter = new Adapter();
    private GridLayoutManager manager;
    private int columns=1;
    private int pendingFocus=-1;
    private String selected;
    private boolean compact, headerSelected, interactive=true, enterPending, geometryReady;
    FavoritesWidgetView(Context context,List<String> keys,Function<String,View> artwork,Consumer<String> focused,
                        Runnable activate,Runnable actions,PaletteColors palette,int opacity,boolean banners,
                        boolean reducedMotion,boolean ribbon,String selected) {
        super(context);this.keys=new ArrayList<>(keys);this.artwork=artwork;this.focused=focused;
        this.activate=activate;this.actions=actions;this.palette=palette;this.banners=banners;
        this.reducedMotion=reducedMotion;this.ribbon=ribbon;this.selected=selected;this.opacity=opacity;
        setClipChildren(true);setClipToPadding(true);setFocusable(true);
        UiBackgroundDrawable background=new UiBackgroundDrawable(palette.surface);background.setCornerRadius(dp(14));background.setStroke(dp(1),palette.edge);background.setFillOpacity(opacity);setBackground(background);
        header=new TextView(context);header.setTextColor(palette.text);header.setTextSize(16);header.setSingleLine(true);
        header.setEllipsize(android.text.TextUtils.TruncateAt.END);header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(12),0,dp(12),0);header.setFocusable(true);
        header.setText(context.getString(R.string.hb_fav_title)+" · "+keys.size());
        header.setOnFocusChangeListener((v,has)->{if(has){headerSelected=true;focused.accept("");}header.setBackgroundColor(has?UiBackgroundDrawable.colorWithOpacity(palette.raised,opacity):0);invalidate();});
        header.setOnClickListener(v->activate.run());header.setOnLongClickListener(v->{actions.run();return true;});
        list=new RecyclerView(context);list.setHasFixedSize(true);list.setItemAnimator(null);
        list.setVerticalScrollBarEnabled(false);list.setHorizontalScrollBarEnabled(false);list.setOverScrollMode(OVER_SCROLL_NEVER);
        list.setClipToPadding(true);list.setPadding(dp(10),dp(4),dp(10),dp(4));list.setAdapter(adapter);
        list.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener(){
            @Override public void onChildViewDetachedFromWindow(View view){}
            @Override public void onChildViewAttachedToWindow(View view){
                view.post(()->{int index=list.getChildAdapterPosition(view);if(interactive&&pendingFocus>=0&&index==pendingFocus){pendingFocus=-1;view.requestFocus();}});
            }
        });
        list.addOnScrollListener(new RecyclerView.OnScrollListener(){@Override public void onScrolled(RecyclerView rv,int dx,int dy){wedges.invalidate();describe();}});
        addView(header,new FrameLayout.LayoutParams(-1,dp(30),Gravity.TOP));
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-1,-1);lp.topMargin=dp(30);lp.bottomMargin=dp(18);addView(list,lp);
        wedges=new Wedges(context);wedges.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);wedges.setFocusable(false);addView(wedges,new FrameLayout.LayoutParams(-1,-1));
        setOnFocusChangeListener((v,has)->{if(has)post(this::enter);});
    }
    String activeKey(){return headerSelected||compact||!keys.contains(selected)?"":selected;}
    String selectedKey(){return selected;}
    boolean browsing(){return list.hasFocus()&&!compact;}
    void header(){
        headerSelected=true;
        if(ribbon&&!compact){list.setVisibility(GONE);header.setVisibility(VISIBLE);FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)header.getLayoutParams();p.height=-1;header.setLayoutParams(p);}
        header.requestFocus();
    }
    void interactive(boolean value){interactive=value;setDescendantFocusability(value?FOCUS_AFTER_DESCENDANTS:FOCUS_BLOCK_DESCENDANTS);}
    void restore(){if(!interactive)requestFocus();else if(headerSelected)header();else enter();}
    void enter(){
        if(!interactive)return;
        if(getWidth()==0||getHeight()==0){enterPending=true;return;}
        enterPending=false;
        if(compact||keys.isEmpty()){header();return;}
        if(ribbon){header.setVisibility(GONE);list.setVisibility(VISIBLE);}
        int i=keys.indexOf(selected);focus(i<0?0:i);
    }
    boolean navigate(int key){
        if(compact)return false;
        if(header.hasFocus()){
            if(key==KeyEvent.KEYCODE_DPAD_DOWN&&!keys.isEmpty()){enter();return true;}
            return false;
        }
        int i=keys.indexOf(selected),dx=key==KeyEvent.KEYCODE_DPAD_LEFT?-1:key==KeyEvent.KEYCODE_DPAD_RIGHT?1:0;
        int dy=key==KeyEvent.KEYCODE_DPAD_UP?-1:key==KeyEvent.KEYCODE_DPAD_DOWN?1:0;
        if(dy<0&&(ribbon||i<columns)){header();return true;}
        int next=FavoritesNavigation.next(i,keys.size(),columns,ribbon,dx,dy);
        if(next<0)return false;
        focus(next);return true;
    }
    private void focus(int index){
        if(index<0||index>=keys.size()||manager==null)return;
        selected=keys.get(index);headerSelected=false;pendingFocus=index;list.stopScroll();
        RecyclerView.ViewHolder holder=list.findViewHolderForAdapterPosition(index);
        if(holder!=null&&manager.isViewPartiallyVisible(holder.itemView,true,false)){pendingFocus=-1;holder.itemView.requestFocus();}
        else{
            manager.scrollToPositionWithOffset(index,0);
            list.post(()->{RecyclerView.ViewHolder target=list.findViewHolderForAdapterPosition(index);if(target!=null)target.itemView.requestFocus();});
        }
        wedges.invalidate();describe();
    }
    private void describe(){
        int position=keys.indexOf(selected)+1;
        setContentDescription(getContext().getString(R.string.hb_fav_position,position,keys.size())+". "+getContext().getString(R.string.hb_fav_hint));
    }
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int w=MeasureSpec.getSize(widthSpec),h=MeasureSpec.getSize(heightSpec);
        boolean nextCompact=keys.isEmpty()||w<dp(banners?170:125)||h<dp(ribbon?90:140);
        // Header geometry must be set before measuring children. Changing it in
        // onSizeChanged left the two-line empty action clipped to the old 30dp.
        if(!geometryReady||nextCompact!=compact){
            compact=nextCompact;geometryReady=true;
            header.setGravity(compact?Gravity.CENTER:Gravity.CENTER_VERTICAL);
            header.setSingleLine(!compact);header.setMaxLines(compact?3:1);
            header.setText(compact?getContext().getString(keys.isEmpty()?R.string.hb_fav_empty:R.string.hb_fav_opener,keys.size()):getContext().getString(R.string.hb_fav_title)+" · "+keys.size());
        }
        FrameLayout.LayoutParams hp=(FrameLayout.LayoutParams)header.getLayoutParams();hp.height=compact||ribbon&&headerSelected?-1:dp(30);
        list.setVisibility(compact||ribbon&&headerSelected?GONE:VISIBLE);
        header.setVisibility(ribbon&&!compact&&!headerSelected?GONE:VISIBLE);
        FrameLayout.LayoutParams content=(FrameLayout.LayoutParams)list.getLayoutParams();content.topMargin=ribbon?0:dp(30);content.bottomMargin=ribbon?0:dp(18);
        list.setPadding(dp(ribbon?18:10),dp(4),dp(ribbon?18:10),dp(4));
        super.onMeasure(widthSpec,heightSpec);
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
        super.onSizeChanged(w,h,oldw,oldh);
        if(compact){wedges.invalidate();if(enterPending)post(this::enter);return;}
        int pitch=Math.max(dp(banners?176:104),Math.round(16*getResources().getDisplayMetrics().scaledDensity*(banners?6:4)));
        columns=ribbon?1:Math.max(1,(w-dp(20))/pitch);
        manager=new GridLayoutManager(getContext(),columns,ribbon?RecyclerView.HORIZONTAL:RecyclerView.VERTICAL,false);
        list.setLayoutManager(manager);adapter.notifyDataSetChanged();
        if(enterPending)post(this::enter);
    }
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private final class Holder extends RecyclerView.ViewHolder {Holder(View view){super(view);}}
    private final class Adapter extends RecyclerView.Adapter<Holder>{
        @Override public int getItemCount(){return keys.size();}
        @Override public Holder onCreateViewHolder(ViewGroup parent,int type){
            FrameLayout frame=new FrameLayout(getContext());frame.setFocusable(true);frame.setClipChildren(true);frame.setClipToPadding(true);frame.setPadding(dp(8),dp(6),dp(8),dp(6));
            frame.setLayoutParams(new RecyclerView.LayoutParams(ribbon?dp(banners?176:104):-1,ribbon?-1:dp(banners?112:110)));
            return new Holder(frame);
        }
        @Override public void onBindViewHolder(Holder holder,int position){
            String key=keys.get(position);FrameLayout frame=(FrameLayout)holder.itemView;frame.removeAllViews();frame.setBackground(null);
            View tile=artwork.apply(key);tile.setFocusable(false);tile.setClickable(false);frame.addView(tile,new FrameLayout.LayoutParams(-1,-1));
            frame.setContentDescription(tile.getContentDescription());
            frame.setOnFocusChangeListener((v,has)->{
                if(tile instanceof CircularAppTile)((CircularAppTile)tile).setAppFocused(has,!reducedMotion);
                if(tile instanceof BannerAppTile)((BannerAppTile)tile).setAppFocused(has,!reducedMotion);
                UiBackgroundDrawable bg=new UiBackgroundDrawable(palette.raised);bg.setCornerRadius(dp(12));bg.setFillOpacity(has?opacity:0);bg.setStroke(dp(has?2:0),palette.focus);frame.setBackground(bg);
                if(has){selected=key;headerSelected=false;focused.accept(key);describe();}
            });
            frame.setOnClickListener(v->activate.run());frame.setOnLongClickListener(v->{actions.run();return true;});
        }
    }
    private final class Wedges extends View{
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        Wedges(Context c){super(c);}
        @Override protected void onDraw(Canvas canvas){
            if(compact||manager==null)return;
            paint.setColor(palette.muted);
            if(ribbon){if(list.canScrollHorizontally(-1))wedge(canvas,dp(6),getHeight()/2f,-1,0);if(list.canScrollHorizontally(1))wedge(canvas,getWidth()-dp(6),getHeight()/2f,1,0);}
            else{if(list.canScrollVertically(-1))wedge(canvas,getWidth()-dp(20),dp(16),0,-1);if(list.canScrollVertically(1))wedge(canvas,getWidth()/2f,getHeight()-dp(8),0,1);}
        }
        void wedge(Canvas canvas,float x,float y,int dx,int dy){
            float r=dp(5);Path p=new Path();p.moveTo(x+dx*r,y+dy*r);p.lineTo(x-dx*r+dy*r,y-dy*r+dx*r);p.lineTo(x-dx*r-dy*r,y-dy*r-dx*r);p.close();canvas.drawPath(p,paint);
        }
    }
}
