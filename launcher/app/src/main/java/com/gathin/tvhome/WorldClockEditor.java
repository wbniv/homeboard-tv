package com.gathin.tvhome;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** One draft, one explicit Save and Undo. Back through subpages never commits settings. */
final class WorldClockEditor extends Dialog {
    private final Activity activity;
    private final PaletteColors palette;
    private final WorldClockSettings draft;
    private final Consumer<WorldClockSettings> save;
    private final LinearLayout root;
    private Runnable back=this::dismiss;
    WorldClockEditor(Activity activity,WorldClockSettings saved,PaletteColors palette,Consumer<WorldClockSettings> save) {
        super(activity);this.activity=activity;this.palette=palette;this.save=save;draft=saved.copy();
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        root=new LinearLayout(activity);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(16),dp(20),dp(16));root.setBackgroundColor(palette.surface);
        setContentView(root);setOnKeyListener((d,key,event)->{if(key==KeyEvent.KEYCODE_BACK||key==KeyEvent.KEYCODE_ESCAPE){if(event.getAction()==KeyEvent.ACTION_UP)back.run();return true;}return false;});main();
    }
    void open() { show();if(getWindow()!=null)getWindow().setLayout(Math.min(dp(820),activity.getResources().getDisplayMetrics().widthPixels-dp(40)),Math.min(dp(470),activity.getResources().getDisplayMetrics().heightPixels-dp(36))); }
    private int dp(float n) { return Math.round(n*activity.getResources().getDisplayMetrics().density); }
    private String t(int id) { return activity.getString(id); }
    private TextView text(String value,int size) { TextView v=new TextView(activity);v.setText(value);v.setTextColor(palette.text);v.setTextSize(size);v.setPadding(dp(6),dp(3),dp(6),dp(3));return v; }
    private void screen(String title,Runnable back) { root.removeAllViews();this.back=back;root.addView(text(title,22)); }
    private Button button(LinearLayout parent,String label,Runnable action) {
        Button b=new Button(activity);b.setText(label);b.setAllCaps(false);b.setTextSize(15);b.setTextColor(palette.text);
        UiBackgroundDrawable bg=new UiBackgroundDrawable(palette.raised);bg.setCornerRadius(dp(5));bg.setStroke(dp(1),palette.edge);b.setBackground(bg);
        b.setOnFocusChangeListener((v,f)->{bg.setStroke(dp(f?2:1),f?palette.text:palette.edge);v.invalidate();});b.setOnClickListener(v->action.run());
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(47));lp.topMargin=dp(4);parent.addView(b,lp);return b;
    }
    private LinearLayout scroll() {
        ScrollView scroll=new ScrollView(activity);LinearLayout list=new LinearLayout(activity);list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));return list;
    }
    private void main() {
        screen(t(R.string.hb_wc_configure),this::dismiss);root.addView(text(t(R.string.hb_wc_draft),14));
        LinearLayout list=scroll();
        button(list,t(R.string.hb_wc_places)+" · "+draft.places.size(),this::places);
        button(list,t(R.string.hb_wc_display),this::display);
        button(list,t(R.string.hb_wc_save),()->{save.accept(draft.copy());dismiss();});
        button(list,t(R.string.hb_fs_back),this::dismiss);
    }
    private void places() {
        screen(t(R.string.hb_wc_places),this::main);LinearLayout list=scroll();
        for(WorldClockSettings.Place p:draft.places) {
            String suffix=p.id.equals(draft.primary)?" · "+t(R.string.hb_wc_primary):"";
            if(p.id.equals(draft.reference))suffix+=" · "+t(R.string.hb_wc_reference);
            button(list,WorldClockLabels.name(activity,p)+suffix,()->place(p));
        }
        Button add=button(list,t(R.string.hb_wc_add_place),()->search(null));add.setEnabled(draft.places.size()<WorldClockSettings.MAX_PLACES);
        Button studio=button(list,t(R.string.hb_wc_studio),()->{WorldClockCatalog.studio(draft);places();});studio.setEnabled(draft.places.size()+5<=WorldClockSettings.MAX_PLACES);
        button(list,t(R.string.hb_fs_back),this::main);
    }
    private void place(WorldClockSettings.Place p) {
        screen(WorldClockLabels.name(activity,p),this::places);LinearLayout list=scroll();
        list.addView(text(p.zone,15));
        button(list,t(R.string.hb_wc_label),()->edit(t(R.string.hb_wc_label),p.label,120,value->{p.label=value;place(p);}));
        button(list,t(R.string.hb_wc_zone),()->search(p));
        button(list,t(R.string.hb_wc_primary),()->{draft.primary=p.id;place(p);});
        button(list,t(R.string.hb_wc_reference),()->{draft.reference=p.id;place(p);});
        LinearLayout moves=new LinearLayout(activity);list.addView(moves);
        Button up=button(moves,t(R.string.hb_wc_move_up),()->move(p,-1));up.setLayoutParams(new LinearLayout.LayoutParams(0,dp(48),1));up.setEnabled(draft.places.indexOf(p)>0);
        Button down=button(moves,t(R.string.hb_wc_move_down),()->move(p,1));down.setLayoutParams(new LinearLayout.LayoutParams(0,dp(48),1));down.setEnabled(draft.places.indexOf(p)<draft.places.size()-1);
        String remove=t(R.string.hb_wc_remove);
        if(p.id.equals(draft.primary)||p.id.equals(draft.reference))remove+=" · "+t(R.string.hb_wc_replacement);
        Button delete=button(list,remove,()->{
            draft.places.remove(p);if(p.id.equals(draft.primary))draft.primary=draft.places.get(0).id;
            if(p.id.equals(draft.reference))draft.reference="";places();
        });delete.setEnabled(draft.places.size()>1);
        button(list,t(R.string.hb_fs_back),this::places);
    }
    private void move(WorldClockSettings.Place p,int delta) {
        draft.ordering="manual";int from=draft.places.indexOf(p);java.util.Collections.swap(draft.places,from,from+delta);place(p);
    }
    private void edit(String title,String value,int max,Consumer<String> action) {
        Runnable previous=back;screen(title,previous);EditText input=new EditText(activity);input.setTextColor(palette.text);input.setSingleLine(true);
        input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(max)});input.setText(value);root.addView(input);
        button(root,t(R.string.hb_wc_done),()->action.accept(input.getText().toString().trim()));button(root,t(R.string.hb_fs_back),previous);input.requestFocus();
    }
    private void display() {
        screen(t(R.string.hb_wc_display),this::main);LinearLayout list=scroll();
        button(list,t(R.string.hb_wc_title)+" · "+draft.title,()->edit(t(R.string.hb_wc_title),draft.title,120,v->{draft.title=v;display();}));
        button(list,t(R.string.hb_wc_format)+" · "+draft.format,()->{
            screen(t(R.string.hb_wc_format),this::display);LinearLayout options=scroll();
            String[] values={"24","12","device"};int[] labels={R.string.hb_wc_24,R.string.hb_wc_12,R.string.hb_wc_follow_device};
            for(int i=0;i<values.length;i++){String value=values[i];button(options,t(labels[i]),()->{draft.format=value;display();});}
        });
        button(list,t(R.string.hb_wc_order)+" · "+t("west".equals(draft.ordering)?R.string.hb_wc_west:R.string.hb_wc_manual),()->{draft.ordering="west".equals(draft.ordering)?"manual":"west";display();});
        button(list,t(R.string.hb_wc_reference)+" · "+t(R.string.hb_wc_local),()->{draft.reference="";display();});
        CheckBox seconds=new CheckBox(activity);seconds.setText(t(R.string.hb_wc_seconds));seconds.setTextColor(palette.text);seconds.setChecked(draft.seconds);seconds.setOnCheckedChangeListener((b,on)->draft.seconds=on);list.addView(seconds);
        list.addView(text(t(R.string.hb_wc_daytime_legend),14));button(list,t(R.string.hb_fs_back),this::main);
    }
    private void search(WorldClockSettings.Place editing) {
        screen(t(R.string.hb_wc_add_place),editing==null?this::places:()->place(editing));
        EditText input=new EditText(activity);input.setTextColor(palette.text);input.setHintTextColor(palette.muted);input.setSingleLine(true);input.setHint(t(R.string.hb_wc_search));input.setContentDescription(t(R.string.hb_wc_search));root.addView(input);
        ListView results=new ListView(activity);root.addView(results,new LinearLayout.LayoutParams(-1,0,1));List<WorldClockCatalog.Entry> matches=new ArrayList<>();
        Runnable filter=()->{
            matches.clear();matches.addAll(WorldClockCatalog.search(input.getText().toString()));
            List<String> labels=new ArrayList<>();labels.add(t(R.string.hb_wc_local));for(WorldClockCatalog.Entry e:matches)labels.add(e.name+" · "+e.zone);
            results.setAdapter(new ArrayAdapter<String>(activity,android.R.layout.simple_list_item_1,labels){
                @Override public View getView(int position,View convert,android.view.ViewGroup parent){TextView v=(TextView)super.getView(position,convert,parent);v.setTextColor(palette.text);v.setTextSize(15);return v;}
            });
        };
        input.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){filter.run();}public void afterTextChanged(Editable e){}});
        results.setOnItemClickListener((parent,v,index,id)->{
            WorldClockSettings.Place chosen=index==0?new WorldClockSettings.Place(UUID.randomUUID().toString(),"",WorldClockSettings.DEVICE):matches.get(index-1).place(UUID.randomUUID().toString());
            if(editing==null)draft.places.add(chosen);
            else {editing.zone=chosen.zone;editing.country=chosen.country;editing.code=chosen.code;editing.latitude=chosen.latitude;editing.longitude=chosen.longitude;}
            places();
        });filter.run();button(root,t(R.string.hb_fs_back),editing==null?this::places:()->place(editing));
    }
}
