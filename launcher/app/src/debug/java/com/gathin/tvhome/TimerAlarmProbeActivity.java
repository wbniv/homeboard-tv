package com.gathin.tvhome;
import android.app.Activity;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.nio.file.Files;
import java.io.File;

/** Explicit device acceptance: only runs when no household timers exist. */
public final class TimerAlarmProbeActivity extends Activity {
    private final Handler handler=new Handler(Looper.getMainLooper());private String before;private boolean started,ownsRuntime;private TextView text;private long baseline;
    @Override protected void onCreate(Bundle state){super.onCreate(state);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(40,40,40,40);root.setBackgroundColor(0xff101a23);text=new TextView(this);text.setTextColor(-1);text.setTextSize(24);root.addView(text);Button allow=new Button(this);allow.setText("Allow test alarms on this TV");allow.setOnClickListener(v->KitchenTimerUi.access(this));root.addView(allow);setContentView(root);}
    @Override protected void onResume(){super.onResume();if(started)return;if(!KitchenTimerStore.allowed(this)){text.setText("Alarm sound proof needs Alarms & reminders access. No test timer has started.");report("blocked","Alarms & reminders access required");return;}started=true;run();}
    private void run(){try{
        // Recover only the fixture created by this session's interrupted J-e8bf13f86582.
        // New runs also retain their fixture ID, so a killed probe is recoverable.
        KitchenTimers recovered=KitchenTimerStore.load(this);String owned=getSharedPreferences("timer-sound-proof",0).getString("id","");boolean cleaned=false;
        for(int i=recovered.items().length()-1;i>=0;i--){JSONObject item=recovered.items().getJSONObject(i);boolean fixture=item.optString("id").equals(owned)||("Homeboard sound test".equals(item.optString("name"))&&"timer".equals(item.optString("kind"))&&item.optLong("duration")==5000&&item.optInt("boot")==KitchenTimerStore.boot(this)&&item.optLong("deadline")<SystemClock.elapsedRealtime()&&SystemClock.elapsedRealtime()-item.optLong("deadline")<3600000);if(fixture){recovered.items().remove(i);cleaned=true;}}
        if(cleaned){recovered.data.put("revision",recovered.revision()+1);getSharedPreferences("kitchen-timers",0).edit().putString("runtime",recovered.data.toString()).commit();}

        if(KitchenTimerStore.load(this).items().length()!=0){text.setText("Proof refused: existing household timers are preserved.");report("blocked","Existing timers preserved");return;}
        SharedPreferences p=getSharedPreferences("kitchen-timers",0);before=p.getString("runtime",null);baseline=p.getLong("last-sound-started",0);ownsRuntime=true;
        JSONObject result=KitchenTimerStore.command(this,KitchenTimerUi.command(this,"create").put("kind","timer").put("name","Homeboard sound test").put("duration",5000));getSharedPreferences("timer-sound-proof",0).edit().putString("id",result.getJSONArray("items").getJSONObject(0).getString("id")).commit();text.setText("Five-second sound test. Settings will remain on screen while the alarm plays.");report("running","Background sound test started");
        handler.postDelayed(()->startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS)),1200);
        handler.postDelayed(()->{try{
            KitchenTimers m=KitchenTimerStore.load(this);boolean done="done".equals(m.items().getJSONObject(0).getString("state"));long sound=p.getLong("last-sound-started",0);boolean ok=done&&sound>baseline;
            report(ok?"pass":"fail","Settings foreground; done="+done+", MediaPlayer started="+(sound>baseline)+". This proves audio API start, not speaker audibility.");KitchenTimerStore.silence(this);restore();
            startActivity(new Intent(this,TimerAlarmProbeActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));text.setText(ok?"PASS: alarm delivered while Settings was open; audio playback started. Check speaker audibility separately.":"FAIL: inspect wf.log");
        }catch(Exception e){report("fail",e.toString());restore();}},11000);
    }catch(Exception e){report("fail",e.toString());restore();}}
    private void restore(){if(!ownsRuntime)return;ownsRuntime=false;if(before==null)getSharedPreferences("kitchen-timers",0).edit().remove("runtime").commit();else getSharedPreferences("kitchen-timers",0).edit().putString("runtime",before).commit();before=null;KitchenTimerStore.reconcile(this);}
    private void report(String status,String detail){try{Files.write(new File(getExternalFilesDir(null),"wf.log").toPath(),new JSONObject().put("status",status).put("detail",detail).put("homeboardVersion",getPackageManager().getPackageInfo(getPackageName(),0).versionName).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}catch(Exception ignored){}}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);if(ownsRuntime){KitchenTimerStore.silence(this);restore();}super.onDestroy();}
}
