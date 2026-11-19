package net.biohack.homeboard.widgetfixture;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONObject;

/** Bounded test, not the product host. It takes no grant and releases IDs before reporting success. */
public final class BindActivity extends Activity {
 private AppWidgetHost host;
 private TextView status;
 private FrameLayout content;
 private int id;
 private boolean bound,finished;
 private Bitmap proof;
 private final Runnable inspect=this::inspectRendered;
 private AppWidgetHostView live;
 @Override public void onCreate(Bundle saved) {
  super.onCreate(saved);
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
  root.setPadding(dp(32),dp(24),dp(32),dp(24));root.setBackgroundColor(0xff182332);
  TextView title=new TextView(this);title.setText("Android widget binding test");title.setTextSize(30);title.setTextColor(-1);root.addView(title);
  status=new TextView(this);status.setTextSize(21);status.setTextColor(0xffc2e7ff);status.setText("Trying the separate fixture provider…");root.addView(status);
  content=new FrameLayout(this);root.addView(content,new LinearLayout.LayoutParams(-1,dp(220)));
  TextView footer=new TextView(this);footer.setTextSize(18);footer.setTextColor(-1);footer.setText("Disposable host + separate provider APK\nNo account or network access\nCoordinator restores the permission and previous screen.");root.addView(footer);
  setContentView(root);
  try {
   CleanupReceiver.release(this);
   host=new AppWidgetHost(this,CleanupReceiver.HOST_ID){
    @Override protected AppWidgetHostView onCreateView(android.content.Context context,int widgetId,android.appwidget.AppWidgetProviderInfo info){
     return new AppWidgetHostView(context){
      @Override public void updateAppWidget(android.widget.RemoteViews views){super.updateAppWidget(views);content.post(inspect);}
     };
    }
   };
   id=host.allocateAppWidgetId();
   AppWidgetManager manager=AppWidgetManager.getInstance(this);
   bound=manager.bindAppWidgetIdIfAllowed(id,new ComponentName("net.biohack.homeboard.widgetprovider","net.biohack.homeboard.widgetprovider.ProbeProvider"));
   if(!bound){finishResult(false,"Binding not allowed");return;}
   live=host.createView(this,id,manager.getAppWidgetInfo(id));
   content.addView(live,new FrameLayout.LayoutParams(-1,-1));
   live.updateAppWidgetSize(new Bundle(),300,200,600,220);
   live.getViewTreeObserver().addOnGlobalLayoutListener(this::inspectRendered);
   host.startListening();content.post(inspect);
  } catch(Exception error){finishResult(false,error.getClass().getSimpleName()+": "+error.getMessage());}
 }
 private void inspectRendered(){
  if(finished||live==null||live.getWidth()<=0||live.getHeight()<=0)return;
  try {
   int valueId=getPackageManager().getResourcesForApplication("net.biohack.homeboard.widgetprovider").getIdentifier("probe_value","id","net.biohack.homeboard.widgetprovider");
   View value=live.findViewById(valueId);
   if(!(value instanceof TextView)||!((TextView)value).getText().toString().equals("Widget bound across apps · id "+id))return;
   proof=Bitmap.createBitmap(live.getWidth(),live.getHeight(),Bitmap.Config.ARGB_8888);
   live.draw(new Canvas(proof));
   ImageView image=new ImageView(this);image.setImageBitmap(proof);image.setScaleType(ImageView.ScaleType.FIT_CENTER);
   content.removeAllViews();content.addView(image,new FrameLayout.LayoutParams(-1,-1));
   finishResult(true,"Bound and rendered · test widget released");
  }catch(Exception error){finishResult(false,"Render failed: "+error.getClass().getSimpleName());}
 }
 private void finishResult(boolean rendered,String message){
  if(finished)return;finished=true;
  boolean released=false;
  try {if(host!=null)host.stopListening();released=CleanupReceiver.release(this)==0;}catch(Exception ignored){}
  status.setText(message+(released?"\nOwned widget IDs: 0":"\nWidget cleanup unverified"));
  try {Log.i("HB_WIDGET_BIND","result="+new JSONObject().put("schema",1).put("bound",bound).put("rendered",rendered).put("idsReleased",released).put("widgetId",id).put("provider","net.biohack.homeboard.widgetprovider/.ProbeProvider").put("message",message));}
  catch(org.json.JSONException ignored){}
 }
 @Override protected void onStop(){
  content.removeCallbacks(inspect);
  if(!finished)finishResult(false,"Interrupted before rendering completed");
  super.onStop();
 }
 @Override protected void onDestroy(){if(host!=null)host.stopListening();super.onDestroy();}
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
