package com.gathin.tvhome;

import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

/** Provider interaction stays inside the existing full-screen restore/Back shell. */
final class AndroidWidgetExpanded implements WidgetExpansion.Content {
    private final WidgetExpansion shell;private final AndroidWidgetHostController controller;private final String key;
    private final LinearLayout root;private final Button interact;private boolean interacting;
    AndroidWidgetExpanded(WidgetExpansion shell,AndroidWidgetHostController controller,String key,HomeLayout layout){
        this.shell=shell;this.controller=controller;this.key=key;
        root=shell.column();LinearLayout body=shell.row();root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        FrameLayout widget=new FrameLayout(shell.activity);body.addView(widget,new LinearLayout.LayoutParams(0,-1,2));
        View view=controller.expand(key);AndroidWidgetHostController.detach(view);widget.addView(view,new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER));
        ScrollView scroll=new ScrollView(shell.activity);LinearLayout detail=shell.column();scroll.addView(detail);body.addView(scroll,new LinearLayout.LayoutParams(0,-1,1));
        AndroidWidgetSpec spec=layout.androidWidget(key);String label=spec==null?controller.text(R.string.hb_aw_unavailable):spec.label;
        detail.addView(shell.text(label,22));
        if(spec!=null){
            ComponentName provider=ComponentName.unflattenFromString(spec.provider);String version="";
            try{version=shell.activity.getPackageManager().getPackageInfo(provider.getPackageName(),0).versionName;}catch(Exception ignored){}
            detail.addView(shell.text(controller.text(R.string.hb_aw_provider)+"\n"+provider.getPackageName()+"\n"+version,16));
            HomeLayout.PanelSpan span=layout.panelSpan(key);detail.addView(shell.text(controller.text(R.string.hb_aw_saved_size)+"\n"+(span==null?"":span.width+" × "+span.height),16));
            Intent open=shell.activity.getPackageManager().getLaunchIntentForPackage(provider.getPackageName());
            if(open==null)open=shell.activity.getPackageManager().getLeanbackLaunchIntentForPackage(provider.getPackageName());
            if(open!=null){Intent launch=open;detail.addView(shell.button(R.string.hb_fs_open,()->shell.activity.startActivity(launch)));}
            Intent info=new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+provider.getPackageName()));
            if(info.resolveActivity(shell.activity.getPackageManager())!=null)detail.addView(shell.button(R.string.hb_app_info,()->shell.activity.startActivity(info)));
        }
        interact=shell.button(R.string.hb_aw_interact,()->{
            interacting=true;controller.interact(key,true);
            if(!controller.hasControls(key)){interacting=false;controller.interact(key,false);shell.details(label,controller.text(R.string.hb_aw_display_only));}
        });root.addView(interact,new LinearLayout.LayoutParams(-1,shell.dp(48)));
        root.addView(shell.text(controller.text(R.string.hb_aw_interact_hint),14));
    }
    @Override public boolean configurable(){return controller.reconfigurable(key);}
    @Override public View view(){return root;}
    @Override public Bundle state(){return new Bundle();}
    @Override public boolean back(){if(!interacting)return false;interacting=false;controller.interact(key,false);interact.requestFocus();return true;}
    @Override public void close(){controller.restoreExpanded();}
}
