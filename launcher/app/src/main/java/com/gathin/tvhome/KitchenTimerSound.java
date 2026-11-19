package com.gathin.tvhome;

import android.app.*;
import android.content.*;
import android.media.*;
import android.os.*;
import org.json.*;

/** Bounded alarm audio across apps. Never launches an activity or changes device volume. */
public final class KitchenTimerSound extends Service {
    private final Handler handler=new Handler(Looper.getMainLooper());private MediaPlayer player;private AudioManager audio;private AudioFocusRequest focus;
    private static final int NOTICE=9103;private static final String CHANNEL="kitchen-timer-alerts";
    private final Runnable tick=new Runnable(){public void run(){try{if(!KitchenTimerStore.sounding(KitchenTimerStore.load(KitchenTimerSound.this),SystemClock.elapsedRealtime())){stopSelf();return;}}catch(JSONException e){stopSelf();return;}handler.postDelayed(this,500);}};
    @Override protected void attachBaseContext(Context base){super.attachBaseContext(AppLanguage.wrap(base));}
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public int onStartCommand(Intent intent,int flags,int id){
        NotificationManager nm=getSystemService(NotificationManager.class);NotificationChannel channel=new NotificationChannel(CHANNEL,getString(R.string.hb_timer_title),NotificationManager.IMPORTANCE_HIGH);channel.setSound(null,null);nm.createNotificationChannel(channel);
        PendingIntent stop=PendingIntent.getService(this,NOTICE,new Intent(this,KitchenTimerSound.class).setAction("silence"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent open=PendingIntent.getActivity(this,NOTICE,new Intent(this,MainActivity.class).setAction("homeboard.timers"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        StringBuilder names=new StringBuilder();try{KitchenTimers model=KitchenTimerStore.load(this);for(int i=0;i<model.items().length();i++){JSONObject t=model.items().getJSONObject(i);if(t.optLong("soundDeadline")>SystemClock.elapsedRealtime()){if(names.length()>0)names.append(" · ");names.append(t.optString("name").isEmpty()?getString(R.string.hb_timer_title):t.optString("name"));}}}catch(JSONException ignored){}
        Notification notification=new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle(getString(R.string.hb_timer_done)).setContentText(names.toString()).setCategory(Notification.CATEGORY_ALARM).setOngoing(true).setContentIntent(open).addAction(new Notification.Action.Builder(null,getString(R.string.hb_timer_silence),stop).build()).build();
        startForeground(NOTICE,notification);
        if(intent!=null&&"silence".equals(intent.getAction())){KitchenTimerStore.silence(this);stopSelf();return START_NOT_STICKY;}
        if(player==null)try{
            audio=getSystemService(AudioManager.class);AudioAttributes attributes=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
            focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).setAudioAttributes(attributes).setOnAudioFocusChangeListener(change->{if(player!=null){if(change==AudioManager.AUDIOFOCUS_LOSS||change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT){player.pause();KitchenTimerStore.error(this,true);}else if(change==AudioManager.AUDIOFOCUS_GAIN)player.start();}}).build();
            if(audio.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED)throw new IllegalStateException("Audio focus unavailable");
            player=new MediaPlayer();player.setWakeMode(this,PowerManager.PARTIAL_WAKE_LOCK);player.setAudioAttributes(attributes);
            try(android.content.res.AssetFileDescriptor tone=getResources().openRawResourceFd(R.raw.homeboard_alarm)){
                player.setDataSource(tone.getFileDescriptor(),tone.getStartOffset(),tone.getLength());player.setLooping(true);player.prepare();
            }
            player.start();getSharedPreferences("kitchen-timers",MODE_PRIVATE).edit().putLong("last-sound-started",System.currentTimeMillis()).commit();KitchenTimerStore.error(this,false);
        }catch(Exception failure){android.util.Log.w("HomeboardAlarm","Unable to play alarm",failure);KitchenTimerStore.error(this,true);stopSelf();return START_NOT_STICKY;}
        handler.removeCallbacks(tick);handler.post(tick);return START_NOT_STICKY;
    }
    @Override public void onDestroy(){handler.removeCallbacks(tick);if(player!=null){player.release();player=null;}if(audio!=null&&focus!=null)audio.abandonAudioFocusRequest(focus);stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();}
}
