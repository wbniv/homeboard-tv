package net.biohack.homeboard.widgetprovider;
public final class ProbeProvider extends android.appwidget.AppWidgetProvider {
 @Override public void onUpdate(android.content.Context context, android.appwidget.AppWidgetManager manager, int[] ids) {
  for (int id:ids) {
   android.widget.RemoteViews views = new android.widget.RemoteViews(context.getPackageName(),R.layout.probe_widget);
   views.setTextViewText(R.id.probe_value,"Widget bound across apps · id " + id);
   manager.updateAppWidget(id,views);
  }
 }
}
