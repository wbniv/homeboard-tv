package net.biohack.homeboard.widgetfixture;

/** Exported only in the disposable diagnostic APK; can release only this app's fixed host. */
public final class CleanupReceiver extends android.content.BroadcastReceiver {
 static final int HOST_ID=8751;
 static int release(android.content.Context context) {
  android.appwidget.AppWidgetHost host=new android.appwidget.AppWidgetHost(context,HOST_ID);
  host.deleteHost();
  return host.getAppWidgetIds().length;
 }
 @Override public void onReceive(android.content.Context context,android.content.Intent intent) {
  try {int remaining=release(context);setResultCode(remaining==0?1:0);setResultData("ids="+remaining);}
  catch(Exception failure){setResultCode(0);setResultData("cleanup-failed:"+failure.getClass().getSimpleName());}
 }
}
