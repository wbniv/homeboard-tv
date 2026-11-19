package com.gathin.tvhome;

import android.os.*;
import android.view.*;
import android.widget.*;
import android.text.InputType;
import org.json.*;
import java.time.*;
import java.util.*;

/** Stable focus while ticking, plus direct cooking and dated-alarm entry. */
final class KitchenTimerExpanded implements WidgetExpansion.Content {
    private final WidgetExpansion h;private final LinearLayout root,rows;private final TextView status;
    private final Handler handler=new Handler(Looper.getMainLooper());private final Map<String,TextView> remaining=new HashMap<>();private long revision=-1;private boolean running;
    private final EditText name,minutes,seconds,date,time;private final Button modeButton,start;private boolean event;
    private final Runnable tick=new Runnable(){public void run(){refresh();if(running)handler.postDelayed(this,500);}};
    KitchenTimerExpanded(WidgetExpansion h,Runnable phone,Bundle saved){this.h=h;event=saved.getBoolean("event");root=h.column();
        LinearLayout bar=h.row();bar.addView(h.button(R.string.hb_timer_access,()->KitchenTimerUi.access(h.activity)));bar.addView(h.button(R.string.hb_timer_phone,phone));bar.addView(h.button(R.string.hb_timer_silence,()->act("silence",null)));root.addView(bar);status=h.text("",14);root.addView(status);
        ScrollView scroll=new ScrollView(h.activity);scroll.setVerticalScrollBarEnabled(false);rows=h.column();scroll.addView(rows);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout form=h.column();modeButton=h.button("",()->{event=!event;mode();});form.addView(modeButton);
        LinearLayout presets=h.row();for(int n:new int[]{1,3,5,10,15,20,30})presets.addView(h.button(h.activity.getString(R.string.hb_timer_minutes)+" "+n,()->preset(n)));form.addView(presets);
        LinearLayout fields=h.row();name=input(fields,R.string.hb_timer_name,InputType.TYPE_CLASS_TEXT,saved.getString("name",""),2);minutes=input(fields,R.string.hb_timer_minutes,InputType.TYPE_CLASS_NUMBER,saved.getString("minutes","5"),1);seconds=input(fields,R.string.hb_timer_seconds,InputType.TYPE_CLASS_NUMBER,saved.getString("seconds","0"),1);date=input(fields,R.string.hb_timer_date,InputType.TYPE_CLASS_DATETIME,saved.getString("date",LocalDateTime.now().plusMinutes(15).toLocalDate().toString()),2);time=input(fields,R.string.hb_timer_time,InputType.TYPE_CLASS_DATETIME,saved.getString("time",LocalTime.now().plusMinutes(15).withSecond(0).withNano(0).toString()),1);form.addView(fields);start=h.button("",this::create);form.addView(start);root.addView(form);mode();refresh();}
    private void preset(int n){minutes.setText(String.valueOf(n));seconds.setText("0");event=false;mode();create();}
    private EditText input(LinearLayout row,int label,int type,String value,int weight){LinearLayout field=h.column();field.addView(h.text(h.activity.getString(label),13));EditText edit=new EditText(h.activity);edit.setTextColor(h.palette.text);edit.setTextSize(18);edit.setSingleLine();edit.setInputType(type);edit.setText(value);edit.setContentDescription(h.activity.getString(label));edit.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(label==R.string.hb_timer_name?80:16)});field.addView(edit,new LinearLayout.LayoutParams(-1,-2));row.addView(field,new LinearLayout.LayoutParams(0,-2,weight));return edit;}
    private void mode(){modeButton.setText(h.activity.getString(event?R.string.hb_timer_event:R.string.hb_timer_cooking)+" ▾");start.setText(event?R.string.hb_timer_set:R.string.hb_timer_start);((View)minutes.getParent()).setVisibility(event?View.GONE:View.VISIBLE);((View)seconds.getParent()).setVisibility(event?View.GONE:View.VISIBLE);((View)date.getParent()).setVisibility(event?View.VISIBLE:View.GONE);((View)time.getParent()).setVisibility(event?View.VISIBLE:View.GONE);}
    private void create(){try{
        JSONObject command=KitchenTimerUi.command(h.activity,"create").put("kind",event?"event":"timer").put("name",name.getText().toString());
        if(event)command.put("local",date.getText()+"T"+time.getText()).put("zone",ZoneId.systemDefault().getId());else{long min=Long.parseLong(minutes.getText().toString()),sec=Long.parseLong(seconds.getText().toString());if(min<0||min>1440||sec<0||sec>59)throw new IllegalArgumentException();command.put("duration",min*60_000+sec*1000);}
        KitchenTimerStore.command(h.activity,command);name.setText("");refresh();
    }catch(Exception e){Toast.makeText(h.activity,KitchenTimerStore.allowed(h.activity)?R.string.hb_timer_invalid:R.string.hb_timer_permission,Toast.LENGTH_LONG).show();}}
    private void act(String action,String id){try{JSONObject command=KitchenTimerUi.command(h.activity,action);if(id!=null)command.put("id",id);KitchenTimerStore.command(h.activity,command);refresh();}catch(Exception e){Toast.makeText(h.activity,KitchenTimerStore.allowed(h.activity)?R.string.hb_timer_invalid:R.string.hb_timer_permission,Toast.LENGTH_LONG).show();}}
    private void refresh(){JSONObject snapshot=KitchenTimerStore.snapshot(h.activity);String error=!snapshot.optBoolean("alarmAccess")?h.activity.getString(R.string.hb_timer_permission):snapshot.optBoolean("alertError")?h.activity.getString(R.string.hb_timer_audio_error):h.activity.getString(R.string.hb_timer_ready);status.setText(error+" · "+h.activity.getString(R.string.hb_timer_zone,ZoneId.systemDefault().getId()));
        JSONArray items=snapshot.optJSONArray("items");long next=snapshot.optLong("revision");
        if(next!=revision){String focus="";View prior=rows.findFocus();if(prior!=null&&prior.getTag() instanceof String)focus=(String)prior.getTag();revision=next;rows.removeAllViews();remaining.clear();
            if(items.length()==0)rows.addView(h.text(h.activity.getString(R.string.hb_timer_empty),22));
            for(JSONObject t:KitchenTimerUi.ordered(items)){String id=t.optString("id"),state=t.optString("state");boolean timer="timer".equals(t.optString("kind"));LinearLayout card=h.column();card.setBackground(h.background(false));TextView title=h.text(KitchenTimerUi.name(h.activity,t),23);card.addView(title);TextView detail=h.text("",22);card.addView(detail);remaining.put(id,detail);if(timer)card.addView(h.text(h.activity.getString(R.string.hb_timer_original,KitchenTimers.clock(t.optLong("duration"))),14));LinearLayout buttons=h.row();
                if(timer&&"running".equals(state))action(buttons,R.string.hb_timer_pause,"pause",id);
                if(timer&&"paused".equals(state))action(buttons,R.string.hb_timer_resume,"resume",id);
                if(timer&&("running".equals(state)||"paused".equals(state)))action(buttons,R.string.hb_timer_add,"add",id);
                if(timer&&!"running".equals(state))action(buttons,R.string.hb_timer_restart,"restart",id);
                if(!"stopped".equals(state))action(buttons,R.string.hb_timer_stop,"stop",id);else action(buttons,R.string.hb_timer_remove,"remove",id);
                card.addView(buttons);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=h.dp(8);rows.addView(card,lp);
            }if(!focus.isEmpty()){View target=rows.findViewWithTag(focus);if(target!=null)target.requestFocus();}
        }
        for(int i=0;i<items.length();i++){JSONObject t=items.optJSONObject(i);TextView detail=remaining.get(t.optString("id"));if(detail!=null)detail.setText(KitchenTimerUi.detail(h.activity,t));}}
    private void action(LinearLayout row,int label,String action,String id){Button b=h.button(label,()->act(action,id));b.setTag(id+":"+action);row.addView(b);}
    public View view(){return root;}
    public Bundle state(){Bundle b=new Bundle();b.putBoolean("event",event);for(var p:new Object[][]{{"name",name},{"minutes",minutes},{"seconds",seconds},{"date",date},{"time",time}})b.putString((String)p[0],((EditText)p[1]).getText().toString());return b;}
    public void active(boolean value){running=value;handler.removeCallbacks(tick);if(value)handler.post(tick);}
}
