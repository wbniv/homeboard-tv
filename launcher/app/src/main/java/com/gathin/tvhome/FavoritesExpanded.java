package com.gathin.tvhome;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class FavoritesExpanded implements WidgetExpansion.Content {
    private final WidgetExpansion h;
    private final Supplier<List<String>> source;
    private final Consumer<String> launch,remove;
    private final Consumer<Intent> open;
    private final Consumer<List<String>> order;
    private final LinearLayout root,actions;
    private final TextView count,metadata;
    private final RecyclerView grid;
    private final GridLayoutManager manager;
    private final Adapter adapter=new Adapter();
    private List<String> keys;
    private String selected;
    private boolean reordering;
    private int cellHeight;
    FavoritesExpanded(WidgetExpansion host,Supplier<List<String>> source,Consumer<String> launch,Consumer<Intent> open,Consumer<String> remove,Consumer<List<String>> order,Bundle state){
        h=host;this.source=source;this.launch=launch;this.open=open;this.remove=remove;this.order=order;
        keys=new ArrayList<>(source.get());selected=state.getString("selected",keys.isEmpty()?"":keys.get(0));cellHeight=h.dp(104);
        root=h.column();count=h.text("",16);root.addView(count);
        LinearLayout row=h.row();grid=new RecyclerView(h.activity);grid.setItemAnimator(null);grid.setHasFixedSize(true);grid.setVerticalScrollBarEnabled(false);
        manager=new GridLayoutManager(h.activity,4);grid.setLayoutManager(manager);grid.setAdapter(adapter);
        row.addView(grid,new LinearLayout.LayoutParams(0,-1,2.1f));
        LinearLayout side=h.column();android.widget.ScrollView scroll=new android.widget.ScrollView(h.activity);metadata=h.text("",17);scroll.addView(metadata);side.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        actions=h.column();side.addView(actions);row.addView(side,new LinearLayout.LayoutParams(0,-1,1));root.addView(row,new LinearLayout.LayoutParams(-1,0,1));
        grid.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
            float density=h.activity.getResources().getDisplayMetrics().density;
            FavoritesExpandedGeometry geometry=new FavoritesExpandedGeometry((r-l)/density,(b-t)/density,h.activity.getResources().getConfiguration().fontScale);
            int height=Math.round(geometry.cellHeight*density);
            grid.post(()->{if(manager.getSpanCount()!=geometry.columns||cellHeight!=height){cellHeight=height;manager.setSpanCount(geometry.columns);adapter.notifyDataSetChanged();}});
        });
        refresh();android.os.Parcelable saved=state.getParcelable("scroll");if(saved!=null)manager.onRestoreInstanceState(saved);
        grid.post(this::focusSelection);
    }
    private ComponentName component(String key){return key!=null&&key.startsWith("app:")?ComponentName.unflattenFromString(key.substring(4)):null;}
    private ActivityInfo info(String key){try{ComponentName c=component(key);return c==null?null:h.activity.getPackageManager().getActivityInfo(c,0);}catch(PackageManager.NameNotFoundException e){return null;}}
    private String label(String key){ActivityInfo app=info(key);return app==null?h.activity.getString(R.string.hb_app_unavailable_hold_ok_to_remove_this_tile):String.valueOf(app.loadLabel(h.activity.getPackageManager()));}
    private void refresh(){
        if(!reordering)keys=new ArrayList<>(source.get());
        if(!keys.contains(selected))selected=keys.isEmpty()?"":keys.get(0);
        adapter.notifyDataSetChanged();showDetails();
    }
    private void showDetails(){
        int index=keys.indexOf(selected);count.setText(h.activity.getString(R.string.hb_fav_title)+" · "+(index+1)+" / "+keys.size());
        actions.removeAllViews();
        if(selected.isEmpty()){metadata.setText(h.activity.getString(R.string.hb_fs_app_empty));return;}
        StringBuilder text=new StringBuilder(label(selected));ComponentName c=component(selected);
        if(c!=null)try{
            PackageInfo pkg=h.activity.getPackageManager().getPackageInfo(c.getPackageName(),0);
            text.append("\n\n").append(h.activity.getString(R.string.hb_fs_version)).append(": ").append(pkg.versionName==null?"—":pkg.versionName);
            text.append("\n\n").append(h.activity.getString(R.string.hb_fs_updated)).append(": ").append(DateTimeFormatter.ofPattern("dd MMM uuuu",h.activity.getResources().getConfiguration().getLocales().get(0)).withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(pkg.lastUpdateTime)));
        }catch(PackageManager.NameNotFoundException e){text.append("\n\n").append(h.activity.getString(R.string.hb_fs_missing));}
        metadata.setText(text);String key=selected;
        if(reordering){
            actions.addView(h.button(R.string.hb_fs_move_left,()->move(-1)));actions.addView(h.button(R.string.hb_fs_move_right,()->move(1)));
            actions.addView(h.button(R.string.hb_fs_done,()->{order.accept(new ArrayList<>(keys));reordering=false;refresh();focusSelection();}));
            actions.addView(h.button(R.string.hb_fs_back,()->{reordering=false;refresh();focusSelection();}));
        }else{
            android.widget.Button go=h.button(R.string.hb_fs_open,()->launch.accept(key));go.setEnabled(info(key)!=null);actions.addView(go);
            android.widget.Button about=h.button(R.string.hb_fs_app_info,()->{ComponentName cmp=component(key);if(cmp!=null)open.accept(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+cmp.getPackageName())));});about.setEnabled(info(key)!=null);actions.addView(about);
            actions.addView(h.button(R.string.hb_fs_remove,()->{remove.accept(key);refresh();focusSelection();}));
            actions.addView(h.button(R.string.hb_fs_reorder,()->{reordering=true;showDetails();}));
        }
    }
    private void move(int delta){int from=keys.indexOf(selected),to=Math.max(0,Math.min(keys.size()-1,from+delta));if(from>=0&&from!=to){Collections.swap(keys,from,to);adapter.notifyDataSetChanged();showDetails();}}
    private void focusSelection(){int index=keys.indexOf(selected);if(index<0)return;manager.scrollToPosition(index);grid.post(()->{RecyclerView.ViewHolder holder=grid.findViewHolderForAdapterPosition(index);if(holder!=null)holder.itemView.requestFocus();});}
    public View view(){return root;}
    public Bundle state(){Bundle b=new Bundle();b.putString("selected",selected);b.putParcelable("scroll",manager.onSaveInstanceState());return b;}
    public boolean back(){if(reordering){reordering=false;refresh();focusSelection();return true;}return false;}
    public void active(boolean active){if(active)refresh();}
    private final class Holder extends RecyclerView.ViewHolder{Holder(View v){super(v);}}
    private final class Adapter extends RecyclerView.Adapter<Holder>{
        public int getItemCount(){return keys.size();}
        public Holder onCreateViewHolder(ViewGroup parent,int type){FrameLayout frame=new FrameLayout(h.activity);frame.setFocusable(true);frame.setPadding(h.dp(5),h.dp(5),h.dp(5),h.dp(5));frame.setLayoutParams(new RecyclerView.LayoutParams(-1,cellHeight));return new Holder(frame);}
        public void onBindViewHolder(Holder holder,int position){
            String key=keys.get(position);FrameLayout frame=(FrameLayout)holder.itemView;frame.removeAllViews();Drawable art=null;ActivityInfo app=info(key);boolean fallback=true;
            if(frame.getLayoutParams().height!=cellHeight){ViewGroup.LayoutParams params=frame.getLayoutParams();params.height=cellHeight;frame.setLayoutParams(params);}
            if(app!=null){art=app.loadBanner(h.activity.getPackageManager());fallback=art==null;if(fallback)art=app.loadIcon(h.activity.getPackageManager());}
            BannerAppTile tile=new BannerAppTile(h.activity,art,fallback,label(key));tile.setFocusable(false);frame.addView(tile,new FrameLayout.LayoutParams(-1,-1));frame.setContentDescription(label(key));
            frame.setOnFocusChangeListener((v,f)->{tile.setAppFocused(f,false);frame.setBackground(h.background(f));if(f){selected=key;showDetails();}});
            frame.setOnClickListener(v->{selected=key;launch.accept(key);});frame.setOnLongClickListener(v->{selected=key;showDetails();if(actions.getChildCount()>0)actions.getChildAt(0).requestFocus();return true;});
        }
    }
}
