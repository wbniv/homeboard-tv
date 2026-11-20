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
    private final boolean banners, circular, reducedMotion, ribbon;
    private final int opacity, labelHeightDp;
    private final TextView header;
    private final RecyclerView list;
    /** Holds the shared focus highlight, clipped to the list's area. */
    private final FrameLayout layer;
    private final AppFocusHighlight highlight;
    private final Wedges wedges;
    private final Adapter adapter = new Adapter();
    private GridLayoutManager manager;
    private int columns=1, pitchDp, ribbonCellPx;
    private final java.util.Map<String,Integer> haloColors=new java.util.HashMap<>();
    private int pendingFocus=-1;
    private String selected;
    private boolean compact, headerSelected, interactive=true, enterPending, geometryReady;
    FavoritesWidgetView(Context context,List<String> keys,Function<String,View> artwork,Consumer<String> focused,
                        Runnable activate,Runnable actions,PaletteColors palette,int opacity,boolean banners,
                        boolean circular,int labelHeightDp,boolean reducedMotion,boolean ribbon,String selected) {
        super(context);this.keys=new ArrayList<>(keys);this.artwork=artwork;this.focused=focused;
        this.activate=activate;this.actions=actions;this.palette=palette;this.banners=banners;
        this.circular=circular;this.labelHeightDp=labelHeightDp;
        this.reducedMotion=reducedMotion;this.ribbon=ribbon;this.selected=selected;this.opacity=opacity;
        setClipChildren(true);setClipToPadding(true);setFocusable(true);
        UiBackgroundDrawable background=new UiBackgroundDrawable(palette.surface);background.setCornerRadius(dp(14));background.setStroke(dp(1),palette.edge);background.setFillOpacity(opacity);setBackground(background);
        header=new TextView(context);header.setTextColor(palette.text);header.setTextSize(16);header.setSingleLine(true);
        header.setEllipsize(android.text.TextUtils.TruncateAt.END);header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(12),0,dp(12),0);header.setFocusable(true);
        header.setText(context.getString(R.string.hb_fav_title)+" · "+keys.size());
        header.setOnFocusChangeListener((v,has)->{if(has){headerSelected=true;focused.accept("");hideHighlight();}header.setBackgroundColor(has?UiBackgroundDrawable.colorWithOpacity(palette.raised,opacity):0);invalidate();});
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
        list.addOnScrollListener(new RecyclerView.OnScrollListener(){@Override public void onScrolled(RecyclerView rv,int dx,int dy){wedges.invalidate();describe();if(dx!=0||dy!=0)syncHighlight();}});
        list.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(r-l!=or-ol||b-t!=ob-ot)syncHighlight();});
        addView(header,new FrameLayout.LayoutParams(-1,dp(30),Gravity.TOP));
        // The same highlight as All Apps. It sits behind round and banner artwork and over plain icons, so its layer
        // goes under or over the list to match, and is clipped to the list's area.
        layer=new FrameLayout(context);layer.setClipChildren(true);layer.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);layer.setFocusable(false);
        highlight=new AppFocusHighlight(context,this,layer,-1,banners,circular,palette.background,reducedMotion);
        // The reference rim is too faint on this lighter panel.
        highlight.setRim(AppAppearance.rimOver(palette.surface,3f));
        boolean behind=AppFocusHighlight.behindArtwork(banners,circular);
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-1,-1);lp.topMargin=dp(30);lp.bottomMargin=dp(18);
        FrameLayout.LayoutParams layerLp=new FrameLayout.LayoutParams(-1,-1);layerLp.topMargin=dp(30);layerLp.bottomMargin=dp(18);
        if(behind)addView(layer,layerLp);
        addView(list,lp);
        if(!behind)addView(layer,layerLp);
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
        int side=ribbon?dp(18):dp(AppGridSizing.PANEL_SIDE_PADDING);list.setPadding(side,dp(4),side,dp(4));
        FrameLayout.LayoutParams layerLp=(FrameLayout.LayoutParams)layer.getLayoutParams();
        layerLp.topMargin=content.topMargin;layerLp.bottomMargin=content.bottomMargin;layer.setVisibility(list.getVisibility());
        super.onMeasure(widthSpec,heightSpec);
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
        super.onSizeChanged(w,h,oldw,oldh);
        if(compact){wedges.invalidate();if(enterPending)post(this::enter);return;}
        // The All Apps cell geometry: the same column pitch, and the columns share the width evenly, as All Apps does.
        float density=getResources().getDisplayMetrics().density;
        pitchDp=roundIcons()?AppAppearance.favoritePitchDp(labelHeightDp):AppGridSizing.columnPitch(labelHeightDp,banners);
        columns=ribbon?1:Math.max(1,(Math.round(w/density)-AppGridSizing.PANEL_SIDE_PADDING*2)/pitchDp);
        // Whole cells only: the ribbon's viewport is shared evenly, so no icon is sliced at either end.
        ribbonCellPx=AppGridSizing.ribbonCell(w-2*dp(18),dp(pitchDp));
        manager=new GridLayoutManager(getContext(),columns,ribbon?RecyclerView.HORIZONTAL:RecyclerView.VERTICAL,false);
        list.setLayoutManager(manager);adapter.notifyDataSetChanged();
        if(enterPending)post(this::enter);
    }
    /** Round Favorites icons are larger than the app default, so their gaps match Google TV's. */
    private boolean roundIcons(){return circular&&!banners;}
    /** The focused icon's colour, for its halo; found once per app. */
    private int haloColor(String key,View tile){
        Integer known=haloColors.get(key);if(known!=null)return known;
        int color=0xffaeb9c0;
        View art=tile instanceof CircularAppTile?((CircularAppTile)tile).artwork():null;
        android.graphics.drawable.Drawable shown=art instanceof android.widget.ImageView?((android.widget.ImageView)art).getDrawable():null;
        android.graphics.drawable.Drawable.ConstantState state=shown==null?null:shown.getConstantState();
        if(state!=null){
            // A copy, so measuring does not disturb the bounds of the drawable on screen.
            android.graphics.drawable.Drawable copy=state.newDrawable().mutate();
            int size=24;android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(size,size,android.graphics.Bitmap.Config.ARGB_8888);
            copy.setBounds(0,0,size,size);copy.draw(new Canvas(bitmap));
            int[] pixels=new int[size*size];bitmap.getPixels(pixels,0,size,0,0,size,size);bitmap.recycle();
            color=AppAppearance.dominantColor(pixels,color);
        }
        haloColors.put(key,color);return color;
    }
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private final class Holder extends RecyclerView.ViewHolder {Holder(View view){super(view);}}
    private void hideHighlight(){highlight.hide();}
    /** Moves the shared highlight to whichever cell has focus, without sliding: after a scroll or a resize. */
    private void syncHighlight(){
        View cell=list.getFocusedChild();
        if(cell instanceof FrameLayout&&cell.isFocused()&&((FrameLayout)cell).getChildCount()>0)
            placeHighlight((FrameLayout)cell,false);
    }
    private void placeHighlight(FrameLayout cell,boolean animate){
        View tile=cell.getChildAt(0);if(tile==null)return;
        View target=AppFocusHighlight.target(tile);
        if(AppFocusHighlight.ready(target))highlight.move(target,animate);
        // Not laid out yet, as when focus is restored while the panel is still being built.
        else target.post(()->{if(cell.isFocused())placeHighlight(cell,false);});
    }
    /** The same tile change All Apps makes: round and banner tiles lift their artwork; plain icons scale. */
    private void focusTile(View tile,boolean has){
        boolean animate=!reducedMotion;
        if(tile instanceof CircularAppTile)((CircularAppTile)tile).setAppFocused(has,animate);
        else if(tile instanceof BannerAppTile)((BannerAppTile)tile).setAppFocused(has,animate);
        else{
            tile.animate().cancel();
            tile.animate().scaleX(has?AppFocusHighlight.SCALE:1f).scaleY(has?AppFocusHighlight.SCALE:1f)
                .setDuration(animate?AppAppearance.TRANSITION_MS:0).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        }
    }
    private final class Adapter extends RecyclerView.Adapter<Holder>{
        @Override public int getItemCount(){return keys.size();}
        @Override public Holder onCreateViewHolder(ViewGroup parent,int type){
            FrameLayout cell=new FrameLayout(getContext());
            cell.setFocusable(true);cell.setClipChildren(false);cell.setClipToPadding(false);
            // The gaps All Apps leaves between cells, as padding.
            cell.setPadding(dp(AppGridSizing.COLUMN_GAP/2f),dp(AppGridSizing.ROW_GAP/2f),dp(AppGridSizing.COLUMN_GAP/2f),dp(AppGridSizing.ROW_GAP/2f));
            cell.setLayoutParams(new RecyclerView.LayoutParams(ribbon?ribbonCellPx:-1,ribbon?-1:dp(AppGridSizing.rowPitch(roundIcons()?AppAppearance.favoriteLabelHeightDp(labelHeightDp):labelHeightDp,banners,roundIcons()?AppAppearance.FAVORITE_ICON_DP:AppAppearance.ICON_DIAMETER_DP))));
            return new Holder(cell);
        }
        @Override public void onBindViewHolder(Holder holder,int position){
            String key=keys.get(position);FrameLayout frame=(FrameLayout)holder.itemView;frame.removeAllViews();
            if(ribbon&&frame.getLayoutParams().width!=ribbonCellPx){ViewGroup.LayoutParams params=frame.getLayoutParams();params.width=ribbonCellPx;frame.setLayoutParams(params);}
            View tile=artwork.apply(key);tile.setFocusable(false);tile.setClickable(false);
            tile.setPadding(dp(2),dp(2),dp(2),dp(2));   // as All Apps tiles
            if(tile instanceof CircularAppTile&&roundIcons()){((CircularAppTile)tile).setPreferredDiameterDp(AppAppearance.FAVORITE_ICON_DP);((CircularAppTile)tile).setLabelTextSizeSp(AppAppearance.FAVORITE_LABEL_SP);}
            frame.addView(tile,new FrameLayout.LayoutParams(-1,-1));
            frame.setContentDescription(tile.getContentDescription());
            frame.setOnFocusChangeListener((v,has)->{
                focusTile(tile,has);
                if(has){selected=key;headerSelected=false;focused.accept(key);describe();highlight.setHalo(haloColor(key,tile));placeHighlight(frame,true);}
                // Hide only if focus has left the panel's apps, not merely moved to the next one.
                else frame.post(()->{if(!list.hasFocus())highlight.hide();});
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
