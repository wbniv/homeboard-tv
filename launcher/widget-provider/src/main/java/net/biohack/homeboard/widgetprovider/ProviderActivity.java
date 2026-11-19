package net.biohack.homeboard.widgetprovider;
public final class ProviderActivity extends android.app.Activity {
 @Override public void onCreate(android.os.Bundle state) {
  super.onCreate(state);
  android.widget.TextView view=new android.widget.TextView(this);
  view.setText("Disposable Android widget provider\nNo account or network access\nBack returns to the previous app");
  view.setTextSize(26);view.setGravity(android.view.Gravity.CENTER);setContentView(view);
 }
}
