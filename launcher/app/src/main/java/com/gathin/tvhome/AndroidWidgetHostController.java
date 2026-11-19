package com.gathin.tvhome;

import android.app.Activity;
import android.appwidget.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.function.IntConsumer;

/** One platform host, durable pending setup, and reference-based ID cleanup. */
final class AndroidWidgetHostController {
    private static final String TAG="HomeboardWidgets";
    private void trace(String message){if((activity.getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0)android.util.Log.i(TAG,message);}
    static final int HOST_ID=8723,REQUEST_BIND=7411,REQUEST_CONFIGURE=7412;
    static final String PENDING=AndroidWidgetPending.KEY;
    interface Ui {
        HomeLayout layout(); int columns(); int rows(); int gridWidth(); int gridHeight();
        void menu(String title,String message,String[] choices,IntConsumer selected);
        void added(String key,AndroidWidgetSpec spec,int width,int height);
        void redraw(); boolean busy();
    }
    final Activity activity; final SharedPreferences preferences; final AppWidgetManager manager; final Ui ui;
    private final Host host;
    private final Map<String,SizedView> views=new LinkedHashMap<>();
    private final Set<String> shown=new LinkedHashSet<>();
    private String expandedKey;
    private boolean closed,offering;
    private final class Host extends AppWidgetHost {
        Host(){super(activity,activity instanceof MainActivity?((MainActivity)activity).androidWidgetHostId():HOST_ID);}
        @Override protected AppWidgetHostView onCreateView(Context c,int id,AppWidgetProviderInfo info){
            for(SizedView v:views.values())if(v.id==id)return v;
            return new SizedView(id,info);
        }
        void forget(){clearViews();}
    }
    AndroidWidgetHostController(Activity activity,Ui ui){
        this.activity=activity;this.ui=ui;preferences=PhoneConfigurationStore.preferences(activity);
        manager=activity.getSystemService(AppWidgetManager.class);host=new Host();
    }
    String text(int id){return activity.getString(id);}
    int dp(float value){return Math.round(value*activity.getResources().getDisplayMetrics().density);}
    int dip(int pixels){return (int)Math.ceil(pixels/activity.getResources().getDisplayMetrics().density);}
    private AndroidWidgetSpec spec(int id,AppWidgetProviderInfo p){
        String label=String.valueOf(p.loadLabel(activity.getPackageManager()));if(label.length()>512)label=label.substring(0,512);
        return new AndroidWidgetSpec(id,p.provider.flattenToString(),label,dip(p.minWidth),dip(p.minHeight),dip(p.minResizeWidth>0?p.minResizeWidth:p.minWidth),dip(p.minResizeHeight>0?p.minResizeHeight:p.minHeight),Build.VERSION.SDK_INT>=31?dip(p.maxResizeWidth):0,Build.VERSION.SDK_INT>=31?dip(p.maxResizeHeight):0,p.resizeMode);
    }
    private AppWidgetProviderInfo info(AndroidWidgetSpec s){
        if(manager==null||s==null)return null;
        AppWidgetProviderInfo p=manager.getAppWidgetInfo(s.id);
        return p!=null&&s.provider.equals(p.provider.flattenToString())?p:null;
    }
    boolean reconfigurable(String key){
        AppWidgetProviderInfo p=info(ui.layout().androidWidget(key));
        return p!=null&&Build.VERSION.SDK_INT>=28&&(p.widgetFeatures&AppWidgetProviderInfo.WIDGET_FEATURE_RECONFIGURABLE)!=0&&p.configure!=null;
    }
    private Rect padding(AndroidWidgetSpec s){return AppWidgetHostView.getDefaultPaddingForWidget(activity,ComponentName.unflattenFromString(s.provider),null);}
    int contentWidth(AndroidWidgetSpec s,int span){Rect p=padding(s);return Math.max(0,ui.gridWidth()*span/ui.columns()-AppGridSizing.COLUMN_GAP-32-dip(p.left+p.right));}
    int contentHeight(AndroidWidgetSpec s,int span){Rect p=padding(s);return Math.max(0,ui.gridHeight()*span/ui.rows()-AppGridSizing.ROW_GAP-50-dip(p.top+p.bottom));}
    int[] minimum(AndroidWidgetSpec s){
        int w=1,h=1;
        while(w<=ui.columns()&&contentWidth(s,w)<s.minWidth)w++;
        while(h<=ui.rows()&&contentHeight(s,h)<s.minHeight)h++;
        return new int[]{w,h};
    }
    boolean legalResize(String key,int oldW,int oldH,int w,int h){
        AndroidWidgetSpec s=ui.layout().androidWidget(key);return s==null||s.legalResize(oldW,oldH,w,h)&&s.fits(contentWidth(s,w),contentHeight(s,h));
    }
    JSONObject presentation(JSONObject draft)throws JSONException {
        JSONArray widgets=draft.getJSONArray("widgets");
        for(int i=0;i<widgets.length();i++){
            JSONObject widget=widgets.getJSONObject(i);String key=widget.getString("key");AndroidWidgetSpec s=ui.layout().androidWidget(key);if(s==null)continue;
            int minW=1,minH=1;while(minW<=ui.columns()&&contentWidth(s,minW)<s.minimumWidth())minW++;while(minH<=ui.rows()&&contentHeight(s,minH)<s.minimumHeight())minH++;
            widget.put("presentation",new JSONObject().put("label",s.label).put("minWidth",minW).put("minHeight",minH).put("horizontal",(s.resizeMode&1)!=0).put("vertical",(s.resizeMode&2)!=0).put("width",widget.getInt("width")).put("height",widget.getInt("height")).put("columns",draft.getInt("columns")).put("rows",draft.getInt("rows")).put("available",info(s)!=null));
        }return draft;
    }
    void validateDraft(HomeLayout before,HomeLayout next){
        boolean gridChanged=before.homeColumns!=next.homeColumns||before.homeRows!=next.homeRows;
        for(String key:next.widgets){AndroidWidgetSpec s=before.androidWidget(key);if(s==null)continue;HomeLayout.PanelSpan a=before.panelSpan(key),b=next.panelSpan(key);if(a==null||b==null||a.width==b.width&&a.height==b.height)continue;
            if(!s.legalResize(a.width,a.height,b.width,b.height)||!gridChanged&&!s.fits(contentWidth(s,b.width),contentHeight(s,b.height)))throw new PhoneConfiguration.Invalid(key,text(R.string.hb_aw_too_small));
        }
    }
    void picker(){
        if(manager==null||!activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_APP_WIDGETS)){notice(text(R.string.hb_aw_unsupported));return;}
        if(!pending().isEmpty()){recover();return;}
        List<AppWidgetProviderInfo> providers=new ArrayList<>(manager.getInstalledProviders());
        providers.removeIf(p->(p.widgetCategory!=0&&(p.widgetCategory&AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN)==0)||Build.VERSION.SDK_INT>=28&&(p.widgetFeatures&AppWidgetProviderInfo.WIDGET_FEATURE_HIDE_FROM_PICKER)!=0);
        providers.sort(Comparator.comparing(p->String.valueOf(p.loadLabel(activity.getPackageManager())),String.CASE_INSENSITIVE_ORDER));
        List<String> labels=new ArrayList<>();
        for(AppWidgetProviderInfo p:providers){String app;try{app=String.valueOf(activity.getPackageManager().getApplicationLabel(activity.getPackageManager().getApplicationInfo(p.provider.getPackageName(),0)));}catch(Exception e){app=p.provider.getPackageName();}labels.add(app+" · "+p.loadLabel(activity.getPackageManager()));}
        labels.add(text(R.string.hb_aw_setup));
        ui.menu(text(R.string.hb_aw_title),providers.isEmpty()?text(R.string.hb_aw_empty):text(R.string.hb_aw_installed),labels.toArray(new String[0]),i->{if(i==providers.size())setup();else begin(providers.get(i),null);});
    }
    void setup(){ui.menu(text(R.string.hb_aw_setup),text(R.string.hb_aw_setup_help)+"\n\nadb shell appwidget grantbind --package "+activity.getPackageName()+" --user 0",new String[]{text(R.string.hb_aw_retry),text(R.string.hb_back)},i->{if(i==0)picker();});}
    void notice(String detail){ui.menu(text(R.string.hb_aw_title),detail,new String[]{text(R.string.hb_back)},i->{});}
    void beginProvider(ComponentName component){
        if(manager!=null)for(AppWidgetProviderInfo p:manager.getInstalledProviders())if(p.provider.equals(component)){begin(p,null);return;}
        notice(text(R.string.hb_aw_empty));
    }
    private void begin(AppWidgetProviderInfo p,String existingKey){
        trace("begin provider="+p.provider.flattenToShortString()+" pending="+!pending().isEmpty());
        if(!pending().isEmpty()){recover();return;}
        int id=-1;
        try{
            id=host.allocateAppWidgetId();
            trace("allocated id="+id);
            AndroidWidgetSpec s=spec(id,p);int[] minimum=minimum(s);
            if(minimum[0]>ui.columns()||minimum[1]>ui.rows()){host.deleteAppWidgetId(id);notice(text(R.string.hb_aw_no_fit));return;}
            JSONObject record=new JSONObject().put("id",id).put("provider",s.provider).put("key",existingKey==null?HomeLayout.ANDROID+":"+UUID.randomUUID():existingKey).put("phase","binding");
            writePending(record);
            if(manager.bindAppWidgetIdIfAllowed(id,p.provider)){trace("bound id="+id);configureAdded(p);}
            else {
                Intent consent=new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER,p.provider);
                if(consent.resolveActivity(activity.getPackageManager())==null){cancel();setup();}
                else activity.startActivityForResult(consent,REQUEST_BIND);
            }
        }catch(Exception failure){android.util.Log.e(TAG,"Widget setup failed id="+id,failure);if(pending().isEmpty()){if(id>0)host.deleteAppWidgetId(id);}else cancel();notice(text(R.string.hb_aw_failed)+"\n"+failure.getClass().getSimpleName());}
    }
    void configure(String key){
        AndroidWidgetSpec s=ui.layout().androidWidget(key);AppWidgetProviderInfo p=info(s);
        if(p==null){repair(key);return;}
        if(!reconfigurable(key)){notice(text(R.string.hb_aw_no_config));return;}
        try{writePending(new JSONObject().put("id",s.id).put("key",key).put("provider",s.provider).put("phase","reconfigure"));launchConfiguration(s.id);}
        catch(Exception failure){cancel();notice(text(R.string.hb_aw_failed));}
    }
    void repair(String key){
        AndroidWidgetSpec s=ui.layout().androidWidget(key);
        if(s!=null&&manager!=null)for(AppWidgetProviderInfo p:manager.getInstalledProviders())if(s.provider.equals(p.provider.flattenToString())){begin(p,key);return;}
        notice(text(R.string.hb_aw_unavailable));
    }
    private void configureAdded(AppWidgetProviderInfo p)throws JSONException {
        JSONObject record=readPending();
        boolean optional=Build.VERSION.SDK_INT>=31&&(p.widgetFeatures&AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL)!=0&&(p.widgetFeatures&AppWidgetProviderInfo.WIDGET_FEATURE_RECONFIGURABLE)!=0;
        trace("configure id="+record.getInt("id")+" activity="+p.configure+" optional="+optional);
        if(p.configure!=null&&!optional){record.put("phase","configuration");writePending(record);launchConfiguration(record.getInt("id"));}
        else{record.put("phase","ready");writePending(record);review();}
    }
    private void launchConfiguration(int id){
        Bundle options=null;
        if(Build.VERSION.SDK_INT>=34){
            // Android's widget service creates this IntentSender with creator BAL denied.
            // Opt in as the visible sender for this explicit provider-setup action only.
            options=android.app.ActivityOptions.makeBasic().setPendingIntentBackgroundActivityStartMode(
                android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED).toBundle();
        }
        host.startAppWidgetConfigureActivityForResult(activity,id,0,REQUEST_CONFIGURE,options);
    }
    boolean result(int request,int result,Intent data){
        if(request!=REQUEST_BIND&&request!=REQUEST_CONFIGURE)return false;
        trace("result request="+request+" result="+result+" id="+(data==null?-1:data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1))+" pending="+!pending().isEmpty());
        try{
            if(pending().isEmpty())return true;
            JSONObject r=readPending();int id=r.getInt("id");String phase=r.getString("phase");
            if(!AndroidWidgetPending.matchingResult(r,request,data==null?-1:data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1)))return true;
            AppWidgetProviderInfo p=manager.getAppWidgetInfo(id);
            if(result!=Activity.RESULT_OK||p==null||!r.getString("provider").equals(p.provider.flattenToString())){cancel();return true;}
            if("reconfigure".equals(phase)){cancel();ui.redraw();return true;}
            if(request==REQUEST_BIND)configureAdded(p);else{r.put("phase","ready");writePending(r);review();}
        }catch(Exception failure){cancel();notice(text(R.string.hb_aw_failed));}
        return true;
    }
    private void review(){
        try{
            JSONObject r=readPending();AppWidgetProviderInfo p=manager.getAppWidgetInfo(r.getInt("id"));
            if(p==null||!r.getString("provider").equals(p.provider.flattenToString())){cancel();notice(text(R.string.hb_aw_unavailable));return;}
            AndroidWidgetSpec s=spec(r.getInt("id"),p);int[] span=minimum(s);
            if(span[0]>ui.columns()||span[1]>ui.rows()){cancel();notice(text(R.string.hb_aw_no_fit));return;}
            offering=true;
            ui.menu(s.label,text(R.string.hb_aw_review)+"\n"+span[0]+" × "+span[1],new String[]{text(R.string.hb_add_to_home),text(android.R.string.cancel)},i->{offering=false;if(i==0)ui.added(r.optString("key"),s,span[0],span[1]);else cancel();});
        }catch(Exception failure){cancel();notice(text(R.string.hb_aw_failed));}
    }
    String pending(){return preferences.getString(PENDING,"");}
    private JSONObject readPending()throws JSONException{return new JSONObject(pending());}
    private void writePending(JSONObject value){AndroidWidgetPending.write(preferences,value);}
    void cancel(){trace("cancel pending="+!pending().isEmpty());offering=false;try{AndroidWidgetPending.write(preferences,null);}catch(IllegalStateException failure){Toast.makeText(activity,text(R.string.hb_configuration_save_failed),Toast.LENGTH_LONG).show();return;}reconcile();}
    void menuDismissed(){if(offering){offering=false;cancel();}}
    boolean busy(){return !pending().isEmpty();}
    void recover(){
        if(offering||pending().isEmpty()||closed)return;
        try{JSONObject r=readPending();if("ready".equals(r.getString("phase"))){review();return;}}
        catch(Exception invalid){notice(text(R.string.hb_aw_failed));return;}
        offering=true;
        ui.menu(text(R.string.hb_aw_resume),text(R.string.hb_aw_resume_help),new String[]{text(R.string.hb_aw_retry),text(android.R.string.cancel)},i->{offering=false;JSONObject record;try{record=readPending();}catch(Exception invalid){cancel();return;}cancel();if(i==0){String key=record.optString("key");if("reconfigure".equals(record.optString("phase")))configure(key);else if(ui.layout().widgets.contains(key))repair(key);else beginProvider(ComponentName.unflattenFromString(record.optString("provider")));}});
    }
    void resume(){if(manager!=null)host.startListening();reconcile();if(!pending().isEmpty()&&!ui.busy())recover();}
    void pause(){host.stopListening();offering=false;}
    void close(){closed=true;host.stopListening();host.forget();views.clear();}
    void reconcile(){
        if(manager==null)return;
        try{Set<Integer> retained=AndroidWidgetReferences.collect(preferences.getString("layout","{\"widgets\":[]}"),preferences.getString("undo_history","[]"),pending());for(int id:host.getAppWidgetIds())if(!retained.contains(id))host.deleteAppWidgetId(id);}
        catch(Exception unreadable){android.util.Log.w("HomeboardWidgets","Widget cleanup deferred: unreadable retained state");}
    }
    void beginRender(){shown.clear();}
    void endRender(){
        host.forget();Set<String> keep=new HashSet<>(shown);if(expandedKey!=null)keep.add(expandedKey);
        views.keySet().retainAll(keep);
        for(String key:keep){AndroidWidgetSpec s=ui.layout().androidWidget(key);AppWidgetProviderInfo p=info(s);if(p!=null&&views.containsKey(key))host.createView(activity,s.id,p);}
    }
    View tile(String key,boolean preview){
        AndroidWidgetSpec s=ui.layout().androidWidget(key);
        LinearLayout wrapper=new LinearLayout(activity){@Override public boolean onInterceptTouchEvent(android.view.MotionEvent e){return true;}};
        wrapper.setOrientation(LinearLayout.VERTICAL);wrapper.setDescendantFocusability(android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        wrapper.setClipChildren(true);wrapper.setContentDescription(s==null?text(R.string.hb_aw_unavailable):s.label);
        TextView label=new TextView(activity);label.setText(s==null?text(R.string.hb_aw_unavailable):s.label);label.setTextColor(-1);label.setTextSize(13);label.setMaxLines(1);label.setEllipsize(android.text.TextUtils.TruncateAt.END);wrapper.addView(label,new LinearLayout.LayoutParams(-1,dp(26)));
        if(s==null||info(s)==null)placeholder(wrapper,text(R.string.hb_aw_unavailable));
        else if(preview||key.equals(expandedKey)||shown.size()>=4)placeholder(wrapper,text(R.string.hb_aw_open));
        else{shown.add(key);SizedView view=view(key,s);detach(view);wrapper.addView(view,new LinearLayout.LayoutParams(-1,0,1));}
        return wrapper;
    }
    private void placeholder(LinearLayout parent,String text){TextView v=new TextView(activity);v.setText(text);v.setTextColor(-1);v.setGravity(Gravity.CENTER);v.setTextSize(16);parent.addView(v,new LinearLayout.LayoutParams(-1,0,1));}
    private SizedView view(String key,AndroidWidgetSpec s){
        SizedView v=views.get(key);if(v==null||v.id!=s.id){views.remove(key);v=(SizedView)host.createView(activity,s.id,info(s));views.put(key,v);}return v;
    }
    static void detach(View v){if(v.getParent() instanceof ViewGroup)((ViewGroup)v.getParent()).removeView(v);}
    View expand(String key){
        expandedKey=key;host.forget();for(Map.Entry<String,SizedView> e:views.entrySet())detach(e.getValue());views.keySet().retainAll(Set.of(key));
        AndroidWidgetSpec s=ui.layout().androidWidget(key);
        if(s==null||info(s)==null){TextView v=new TextView(activity);v.setText(text(R.string.hb_aw_unavailable));v.setTextColor(-1);return v;}
        SizedView v=view(key,s);v.interaction=true;v.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);host.createView(activity,s.id,info(s));return v;
    }
    void restoreExpanded(){expandedKey=null;}
    void interact(String key,boolean enabled){SizedView v=views.get(key);if(v!=null){v.interaction=enabled;v.setDescendantFocusability(enabled?ViewGroup.FOCUS_AFTER_DESCENDANTS:ViewGroup.FOCUS_BLOCK_DESCENDANTS);if(enabled)v.requestFocus(View.FOCUS_DOWN);}}
    boolean hasControls(String key){SizedView v=views.get(key);if(v==null)return false;ArrayList<View> focus=new ArrayList<>();v.addFocusables(focus,View.FOCUS_DOWN);return !focus.isEmpty();}
    private final class SizedView extends AppWidgetHostView {
        final int id;final AppWidgetProviderInfo provider;boolean interaction,tooSmall;View error;
        SizedView(int id,AppWidgetProviderInfo provider){super(activity);this.id=id;this.provider=provider;setClipChildren(true);}
        @Override public boolean onInterceptTouchEvent(android.view.MotionEvent event){return super.onInterceptTouchEvent(event);}
        @Override protected void onMeasure(int widthSpec,int heightSpec){
            AndroidWidgetSpec s=spec(id,provider);Rect pad=padding(s);
            int maxW=(s.resizeMode&1)==0?s.minWidth:s.maxWidth,maxH=(s.resizeMode&2)==0?s.minHeight:s.maxHeight;
            if(maxW>0)widthSpec=MeasureSpec.makeMeasureSpec(Math.min(MeasureSpec.getSize(widthSpec),dp(maxW)+pad.left+pad.right),MeasureSpec.EXACTLY);
            if(maxH>0)heightSpec=MeasureSpec.makeMeasureSpec(Math.min(MeasureSpec.getSize(heightSpec),dp(maxH)+pad.top+pad.bottom),MeasureSpec.EXACTLY);
            super.onMeasure(widthSpec,heightSpec);
        }
        @Override protected void onSizeChanged(int w,int h,int oldW,int oldH){super.onSizeChanged(w,h,oldW,oldH);post(()->size(w,h));}
        private void size(int width,int height){
            if(closed||width<=0||height<=0)return;
            Rect pad=AppWidgetHostView.getDefaultPaddingForWidget(activity,provider.provider,null);
            AndroidWidgetSpec s=spec(id,provider);boolean bad=!s.fits((int)Math.floor((width-pad.left-pad.right)/activity.getResources().getDisplayMetrics().density),(int)Math.floor((height-pad.top-pad.bottom)/activity.getResources().getDisplayMetrics().density));
            if(bad){if(!tooSmall){tooSmall=true;super.updateAppWidget(null);TextView message=new TextView(activity);message.setText(text(R.string.hb_aw_too_small));message.setTextSize(16);message.setTextColor(-1);message.setGravity(Gravity.CENTER);message.setBackgroundColor(0xff182332);addView(message,new LayoutParams(-1,-1));error=message;}return;}
            if(tooSmall){tooSmall=false;if(error!=null){removeView(error);error=null;}// Re-registering obtains the actual provider view, not invented content.
                host.createView(activity,id,provider);
            }
            updateAppWidgetSize(new Bundle(),dip(width),dip(height),dip(width),dip(height));
        }
        @Override public void updateAppWidget(RemoteViews views){if(!tooSmall)super.updateAppWidget(views);}
    }
}
