package com.gathin.tvhome;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.File;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;
import org.json.*;

/**
 * Android widget lifecycle, survivor and resource acceptance against the fixture provider.
 * The activity alias's "mode" meta-data selects the run (docs/plans/2026-11-19-android-app-widgets.md,
 * Close-out). Every mode has its own preferences namespace and host ID, so reference cleanup in one
 * run can never release a binding owned by the household dashboard (8723), the older probe (8724) or
 * another mode. Results go to wf.log and logcat tag HomeboardWidgetProbe.
 */
public final class AndroidWidgetLifecycleProbeActivity extends MainActivity {
    private static final String TAG="HomeboardWidgetProbe";
    private static final ComponentName FIXTURE=new ComponentName("net.biohack.homeboard.widgetprovider","net.biohack.homeboard.widgetprovider.ProbeProvider");
    private static final int COLUMNS=12,ROWS=8;
    /** Survives activity recreation inside one process; survivor modes persist through preferences instead. */
    private static final Set<String> cleared=new HashSet<>();
    private static Map<String,?> household;
    private static String keyA,keyB;private static int idA,idB;private static boolean recreated;
    private static final JSONObject facts=new JSONObject();
    private final Handler steps=new Handler(Looper.getMainLooper());
    private String mode;private boolean restarting;private int operation;

    private String mode(){
        if(mode!=null)return mode;
        try{Bundle meta=getPackageManager().getActivityInfo(getComponentName(),PackageManager.GET_META_DATA).metaData;mode=meta==null?"lifecycle":meta.getString("mode","lifecycle");}
        catch(Exception e){mode="lifecycle";}
        return mode;
    }
    private boolean survivor(){return mode().startsWith("survivor");}
    @Override int androidWidgetHostId(){return mode().equals("resource")?8726:survivor()?8727:8725;}
    @Override public SharedPreferences getSharedPreferences(String name,int mode){
        if(name.equals("widget-home")||name.startsWith("rain-")||name.startsWith("air-quality-widget-")){
            String group=survivor()?"survivor":mode();name="android-widget-"+group+"-"+name;
            boolean fresh=!survivor()||mode().equals("survivor-keep");
            if(fresh&&cleared.add(name))super.getSharedPreferences(name,mode).edit().clear().commit();
        }return super.getSharedPreferences(name,mode);
    }
    @Override protected void onCreate(Bundle state){
        boolean continuing=state!=null&&recreated;
        // A fresh launch starts from empty probe preferences even if this process ran the probe before.
        if(state==null){cleared.clear();recreated=false;}
        if(household==null)household=new LinkedHashMap<>(super.getSharedPreferences("widget-home",0).getAll());
        if(state==null&&(!survivor()||mode().equals("survivor-keep"))){HomeLayout seed=new HomeLayout();seed.homeColumns=COLUMNS;seed.homeRows=ROWS;seed.favoritesContainer=true;
            getSharedPreferences("widget-home",0).edit().putString("layout",seed.encode()).putBoolean("single-screen-default-v1",true).putBoolean("favorites-container-v1",true).commit();}
        super.onCreate(state);
        put("pid",android.os.Process.myPid());
        report("running",mode()+(continuing?" after recreation":""));
        switch(mode()){
            case "resource":resource();break;
            case "survivor-keep":keep();break;
            case "survivor-reopen":reopen();break;
            case "survivor-reinstalled":reinstalled();break;
            default:if(continuing)afterRecreation();else lifecycle();
        }
    }

    // ── lifecycle ────────────────────────────────────────────────────────────────────────────────
    private void lifecycle(){
        run(2000,()->add(key->{keyA=key;idA=spec(key).id;put("idA",idA);
            awaitText(()->hostText(dashboard(),idA),"Widget bound across apps · id "+idA,()->add(key2->{keyB=key2;idB=spec(key2).id;put("idB",idB);
                require(idB!=idA,"Second instance reused the first ID");place();
                awaitTrue("both instances painted",()->hostText(dashboard(),idA).contains("id "+idA)&&hostText(dashboard(),idB).contains("id "+idB),this::sizeResponse);}));}));
    }
    /** Stack A over B at the minimum span so A can widen, then prove both paint their own content. */
    private void place()throws Exception{
        int[] min=controller().minimum(spec(keyA));put("minimumSpan",min[0]+"x"+min[1]);put("grid",controllerColumns()+"x"+controllerRows());
        require(2*min[1]<=controllerRows()&&min[0]+2<=controllerColumns(),"Grid too small for the stacked layout: "+min[0]+"x"+min[1]+" on "+controllerColumns()+"x"+controllerRows());
        JSONObject s=phoneState(),d=s.getJSONObject("draft");JSONArray w=d.getJSONArray("widgets");
        for(int i=0;i<w.length();i++){JSONObject o=w.getJSONObject(i);String k=o.getString("key");
            if(k.equals(keyA))o.put("page",0).put("column",0).put("row",0).put("width",min[0]).put("height",min[1]);
            else if(k.equals(keyB))o.put("page",0).put("column",0).put("row",min[1]).put("width",min[0]).put("height",min[1]);}
        apply(s,d);show(keyA);
    }
    /** Host arithmetic beside what the provider view actually got, at the minimum span. */
    private JSONObject geometry(String key)throws Exception{
        AndroidWidgetSpec s=spec(key);HomeLayout.PanelSpan span=layout().panelSpan(key);AndroidWidgetHostController c=controller();
        AppWidgetHostView v=host(dashboard(),s.id);View page=(View)field("surface");
        android.graphics.Rect pad=AppWidgetHostView.getDefaultPaddingForWidget(this,ComponentName.unflattenFromString(s.provider),null);
        Bundle o=manager().getAppWidgetOptions(s.id);
        JSONObject g=new JSONObject().put("density",getResources().getDisplayMetrics().density).put("screenPx",getResources().getDisplayMetrics().widthPixels+"x"+getResources().getDisplayMetrics().heightPixels)
            .put("rootPx",call("dashboardWidthPixels",new Class<?>[0])+"x"+call("dashboardHeightPixels",new Class<?>[0])).put("pagePx",page==null?"none":page.getWidth()+"x"+page.getHeight())
            .put("gridDp",call("androidGridWidth",new Class<?>[0])+"x"+call("androidGridHeight",new Class<?>[0])).put("cells",controllerColumns()+"x"+controllerRows()).put("scale",call("dashboardScale",new Class<?>[0]))
            .put("span",span.width+"x"+span.height).put("hostContentDp",c.contentWidth(s,span.width)+"x"+c.contentHeight(s,span.height)).put("padPx",pad.toShortString())
            .put("viewPx",v==null?"none":v.getWidth()+"x"+v.getHeight()).put("viewDensity",v==null?0:v.getResources().getDisplayMetrics().density)
            .put("wrapperPx",v==null||v.getParent()==null?"none":((View)v.getParent()).getWidth()+"x"+((View)v.getParent()).getHeight())
            .put("optionsDp",o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)+"x"+o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)+" max "+o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)+"x"+o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT))
            .put("providerMinDp",s.minWidth+"x"+s.minHeight);
        View t=v;String chain="";for(int i=0;i<4&&t!=null;i++){chain+=t.getClass().getSimpleName()+" "+t.getWidth()+"x"+t.getHeight()+" scale "+t.getScaleX()+"; ";t=t.getParent() instanceof View?(View)t.getParent():null;}
        return g.put("chain",chain);
    }
    private void sizeResponse(){
        step(()->{
            put("geometry",geometry(keyA));
            String before=hostText(dashboard(),idA);int updates=number(before,"Updates ");String size=sizeOf(before);put("beforeResize",line(before));
            HomeLayout.PanelSpan span=layout().panelSpan(keyA);
            JSONObject s=phoneState(),d=s.getJSONObject("draft");widget(d,keyA).put("width",span.width+2);apply(s,d);show(keyA);
            awaitTrue("provider re-rendered at the new size",()->{String t=hostText(dashboard(),idA);return number(t,"Updates ")>updates&&!sizeOf(t).equals(size)&&!sizeOf(t).isEmpty();},()->{
                put("afterResize",line(hostText(dashboard(),idA)));interact();});
        });
    }
    private void interact(){
        step(()->{
            show(keyA);press(KeyEvent.KEYCODE_DPAD_CENTER);
            awaitTrue("expanded provider view",()->expanded()!=null&&expandedView()!=null&&hostText(expandedView(),idA).contains("id "+idA),()->{
                View button=find(expandedView(),getString(R.string.hb_aw_interact));require(button!=null,"Interact action missing");button.performClick();
                require(controller().hasControls(keyA),"Fixture Press button not focusable");
                keyTo(expanded(),KeyEvent.KEYCODE_DPAD_CENTER);
                awaitText(()->hostText(expandedView(),idA),"Presses 1",()->{
                    put("afterPress",line(hostText(expandedView(),idA)));
                    // Back is handled synchronously on key-up: the first leaves Interact, the second restores Home.
                    keyTo(expanded(),KeyEvent.KEYCODE_BACK);
                    require(expanded()!=null,"First Back left full screen instead of leaving Interact");
                    keyTo(expanded(),KeyEvent.KEYCODE_BACK);
                    awaitTrue("Back restores the dashboard",()->expanded()==null,()->{require(keyA.equals(field("focused")),"Focus not restored to the widget");illegalResize();});
                });
            });
        });
    }
    private void illegalResize(){
        step(()->{
            String before=layout().encode();HomeLayout.PanelSpan span=layout().panelSpan(keyA);
            require(!controller().legalResize(keyA,span.width,span.height,1,1),"Native resize would accept 1×1");
            JSONObject s=phoneState(),d=s.getJSONObject("draft");widget(d,keyA).put("width",1).put("height",1);
            boolean rejected=false;try{apply(s,d);}catch(PhoneConfiguration.Invalid expected){rejected=true;put("illegalResize",expected.getMessage());}
            require(rejected,"Browser 1×1 resize was accepted");require(before.equals(layout().encode()),"Rejected resize changed the layout");
            gridChange();
        });
    }
    private void gridChange(){
        step(()->{
            HomeLayout.PanelSpan span=layout().panelSpan(keyA);
            JSONObject s=phoneState(),d=s.getJSONObject("draft");d.put("columns",24).put("rows",24);apply(s,d);show(keyA);
            AndroidWidgetSpec a=spec(keyA);
            require(a.id==idA&&layout().panelSpan(keyA).width==span.width&&layout().panelSpan(keyA).height==span.height,"Grid change moved, resized or rebound the widget");
            require(!a.fits(controller().contentWidth(a,span.width),controller().contentHeight(a,span.height)),"Precondition: 24×24 still fits the provider minimum");
            // Too small is decided per tile: either a placeholder where the provider view would be, or the message in the label row.
            awaitTrue("Too small in A's tile, no live view",()->host(dashboard(),idA)==null&&text(dashboard()).contains(getString(R.string.hb_aw_too_small)),()->{
                put("gridPlaceholder",AndroidWidgetGeometry.presentation(a,controller().contentWidth(a,span.width),controller().contentHeight(a,span.height)).name());
                JSONObject s2=phoneState(),d2=s2.getJSONObject("draft");d2.put("columns",COLUMNS).put("rows",ROWS);apply(s2,d2);show(keyA);
                awaitTrue("live provider view after the grid is legal again",()->{String t=hostText(dashboard(),idA);return t.contains("id "+idA)&&!t.contains(getString(R.string.hb_aw_too_small));},this::lostBinding);
            });
        });
    }
    private void lostBinding(){
        step(()->{
            new AppWidgetHost(this,androidWidgetHostId()).deleteAppWidgetId(idB);show(keyB);
            awaitTrue("Widget unavailable in B's place, A unaffected",()->tileText(keyB).contains(getString(R.string.hb_aw_unavailable))&&hostText(dashboard(),idA).contains("id "+idA),()->{
                require(layout().widgets.contains(keyB),"Lost binding removed the tile");
                JSONObject s=phoneState(),d=s.getJSONObject("draft");remove(d,keyB);JSONObject removed=apply(s,d);
                undo(removed);require(layout().widgets.contains(keyB)&&spec(keyB).id==idB,"Undo did not restore the unavailable tile");
                JSONObject s2=phoneState(),d2=s2.getJSONObject("draft");remove(d2,keyB);apply(s2,d2);
                require(!layout().widgets.contains(keyB),"Unavailable tile not removed");
                recreated=true;restarting=true;report("running","recreating");recreate();
            });
        });
    }
    private void afterRecreation(){
        run(1500,()->{
            require(keyA!=null&&layout().widgets.contains(keyA)&&spec(keyA).id==idA,"Recreation lost the saved widget or its ID");show(keyA);
            awaitText(()->hostText(dashboard(),idA),"id "+idA,()->{
                put("afterRecreation",line(hostText(dashboard(),idA)));
                JSONObject s=phoneState(),d=s.getJSONObject("draft");remove(d,keyA);JSONObject removed=apply(s,d);
                require(manager().getAppWidgetInfo(idA)!=null,"Remove deleted the binding Undo needs");
                undo(removed);require(spec(keyA).id==idA,"Undo replaced the original binding");
                JSONObject s2=phoneState(),d2=s2.getJSONObject("draft");remove(d2,keyA);apply(s2,d2);
                int edits=0;boolean retainedLate=false;
                for(;edits<25&&ids().contains(idA);edits++){
                    if(edits==17)retainedLate=true;
                    JSONObject s3=phoneState(),d3=s3.getJSONObject("draft");d3.put("columns",edits%2==0?COLUMNS+1:COLUMNS);apply(s3,d3);
                }
                require(retainedLate,"Binding released before history eviction (after "+edits+" edits)");
                require(!ids().contains(idA),"Binding still retained after "+edits+" later edits");put("releasedAfterEdits",edits);
                JSONObject s4=phoneState(),d4=s4.getJSONObject("draft");d4.put("columns",COLUMNS);apply(s4,d4);
                cap();
            });
        });
    }
    private void cap()throws Exception{
        List<String> keys=new ArrayList<>();
        addMany(5,keys,()->{
            int[] min=controller().minimum(spec(keys.get(0)));
            require(3*min[0]<=controllerColumns()&&2*min[1]<=controllerRows(),"Grid cannot hold five minimum tiles on one page");
            JSONObject s=phoneState(),d=s.getJSONObject("draft");
            for(int i=0;i<5;i++)widget(d,keys.get(i)).put("page",0).put("column",(i%3)*min[0]).put("row",(i/3)*min[1]).put("width",min[0]).put("height",min[1]);
            apply(s,d);show(keys.get(0));
            awaitTrue("four live views and one Open placeholder",()->liveViews(dashboard())==4&&count(text(dashboard()),getString(R.string.hb_aw_open))==1,()->{
                put("liveViews",liveViews(dashboard()));finish("pass","two instances; provider update and size response; Interact Press and nested Back; illegal resize rejected; 24×24 placeholder and recovery; lost binding placeholder, Remove/Undo; recreation keeps ID; Remove/Undo same ID; release after history eviction; four-view cap");
            });
        });
    }

    // ── survivor: across jobs, provider update and reinstall ────────────────────────────────────
    private void keep(){
        run(2000,()->add(key->{
            awaitText(()->hostText(dashboard(),spec(key).id),"id "+spec(key).id,()->{
                put("key",key);put("id",spec(key).id);put("provider",providerVersion());put("content",line(hostText(dashboard(),spec(key).id)));
                restarting=true;report("pass","kept bound widget for the next job");
            });
        }));
    }
    private void reopen(){
        run(2500,()->{
            String key=androidKeys().isEmpty()?null:androidKeys().get(0);require(key!=null,"No saved widget from the keep job");
            int id=spec(key).id;put("key",key);put("id",id);put("provider",providerVersion());show(key);
            awaitText(()->hostText(dashboard(),id),"id "+id,()->{
                put("content",line(hostText(dashboard(),id)));restarting=true;report("pass","same binding rendered in this process");
            });
        });
    }
    private void reinstalled(){
        run(2500,()->{
            String key=androidKeys().isEmpty()?null:androidKeys().get(0);require(key!=null,"No saved widget from the keep job");
            int old=spec(key).id;HomeLayout.PanelSpan span=layout().panelSpan(key);put("key",key);put("oldId",old);put("provider",providerVersion());show(key);
            awaitTrue("Widget unavailable after uninstall",()->tileText(key).contains(getString(R.string.hb_aw_unavailable)),()->{
                put("unavailable",true);controller().repair(key);
                awaitTrue("Set up again review",()->field("activeMenu")!=null,()->{
                    choose(0);AndroidWidgetSpec now=spec(key);
                    require(now!=null&&now.id!=old,"Set up again kept the lost ID");
                    require(layout().panelSpan(key).width==span.width&&layout().panelSpan(key).height==span.height,"Set up again changed the saved size");
                    put("newId",now.id);show(key);
                    awaitText(()->hostText(dashboard(),now.id),"id "+now.id,()->finish("pass","lost after uninstall; Set up again kept key and size with a new binding"));
                });
            });
        });
    }

    // ── resource: phases timed for the coordinator's PSS timeline ───────────────────────────────
    private void resource(){
        List<String> keys=new ArrayList<>();
        phase("zero");
        run(60_000,()->addMany(1,keys,()->{phase("one");
            run(60_000,()->addMany(3,keys,()->{
                int[] min=controller().minimum(spec(keys.get(0)));
                JSONObject s=phoneState(),d=s.getJSONObject("draft");
                for(int i=0;i<4;i++)widget(d,keys.get(i)).put("page",0).put("column",(i%2)*min[0]).put("row",(i/2)*min[1]).put("width",min[0]).put("height",min[1]);
                apply(s,d);show(keys.get(0));
                awaitTrue("four live views",()->liveViews(dashboard())==4,()->{phase("four");
                    run(60_000,()->{phase("cycles");cycle(keys,0);});
                });
            }));
        }));
    }
    private void cycle(List<String> keys,int n){
        if(n==50){phase("after-cycles");run(60_000,()->{phase("end");finish("pass","zero, one and four views held 60 s each; 50 expand/restore cycles; 60 s after");});return;}
        step(()->{String key=keys.get(n%4);show(key);press(KeyEvent.KEYCODE_DPAD_CENTER);
            awaitTrue("expanded "+n,()->expanded()!=null&&expandedView()!=null&&find(expandedView(),getString(R.string.hb_fs_restore))!=null,()->{
                find(expandedView(),getString(R.string.hb_fs_restore)).performClick();
                awaitTrue("restored "+n,()->expanded()==null,()->cycle(keys,n+1));
            });
        });
    }
    private void phase(String name){
        Debug.MemoryInfo m=new Debug.MemoryInfo();Debug.getMemoryInfo(m);
        try{JSONArray p=facts.optJSONArray("phases");if(p==null){p=new JSONArray();facts.put("phases",p);}
            p.put(new JSONObject().put("phase",name).put("at",Instant.now().toString()).put("selfPssKiB",m.getTotalPss()).put("liveViews",liveViews(dashboard())));}
        catch(JSONException ignored){}
        report("running","phase "+name);
    }

    // ── shared steps ────────────────────────────────────────────────────────────────────────────
    private interface Stage{void run()throws Exception;}
    private interface Text{String get()throws Exception;}
    private interface Check{boolean get()throws Exception;}
    private interface Added{void run(String key)throws Exception;}
    private void run(long ms,Stage s){steps.postDelayed(()->guard(s),ms);}
    private void step(Stage s){run(400,s);}
    private void guard(Stage s){try{s.run();}catch(Throwable e){steps.removeCallbacksAndMessages(null);finish("fail",e.toString());}}
    /** Polls the condition; the deadline only turns a missing signal into a failure. */
    private void awaitTrue(String what,Check c,Stage next){long deadline=SystemClock.uptimeMillis()+20_000;poll(what,c,next,deadline);}
    private void poll(String what,Check c,Stage next,long deadline){
        steps.postDelayed(()->guard(()->{if(c.get())next.run();else if(SystemClock.uptimeMillis()>deadline)throw new IllegalStateException("Timed out waiting for "+what);else poll(what,c,next,deadline);}),250);
    }
    private void awaitText(Text t,String expected,Stage next){awaitTrue("text \""+expected+"\"",()->t.get().contains(expected),next);}
    private void add(Added done)throws Exception{
        Set<String> before=new HashSet<>(androidKeys());beginAndroidWidgetTest(FIXTURE);
        awaitTrue("Add review",()->field("activeMenu")!=null,()->{choose(0);
            String key=null;for(String k:androidKeys())if(!before.contains(k))key=k;require(key!=null&&spec(key).id>0,"Add did not save a bound widget");done.run(key);});
    }
    private void addMany(int n,List<String> keys,Stage done)throws Exception{
        if(n==0){done.run();return;}
        add(key->{keys.add(key);run(600,()->addMany(n-1,keys,done));});
    }
    private JSONObject apply(JSONObject state,JSONObject draft)throws Exception{
        return phoneApply(new JSONObject().put("revision",state.getLong("revision")).put("operation","widget-lifecycle-"+mode()+"-"+(++operation)+"-"+SystemClock.uptimeMillis()).put("draft",draft));
    }
    private void undo(JSONObject after)throws Exception{phoneUndo(new JSONObject().put("revision",after.getLong("revision")).put("operation","widget-lifecycle-undo-"+(++operation)+"-"+SystemClock.uptimeMillis()));}
    private static JSONObject widget(JSONObject draft,String key)throws JSONException{JSONArray w=draft.getJSONArray("widgets");for(int i=0;i<w.length();i++)if(key.equals(w.getJSONObject(i).getString("key")))return w.getJSONObject(i);throw new IllegalStateException("Draft lacks "+key);}
    private static void remove(JSONObject draft,String key)throws JSONException{JSONArray w=draft.getJSONArray("widgets"),kept=new JSONArray();for(int i=0;i<w.length();i++)if(!key.equals(w.getJSONObject(i).getString("key")))kept.put(w.getJSONObject(i));draft.put("widgets",kept);}
    private void show(String key)throws Exception{set("homePage",homePage(key));set("focused",key);set("homeFocus",key);call("render",new Class<?>[]{int.class},0);}
    private int homePage(String key)throws Exception{Method m=MainActivity.class.getDeclaredMethod("homePageOf",String.class,List.class);m.setAccessible(true);return (Integer)m.invoke(this,key,layout().widgets);}
    private List<String> androidKeys(){List<String> keys=new ArrayList<>();for(String k:layout().widgets)if(AndroidWidgetSpec.key(k))keys.add(k);return keys;}
    private AndroidWidgetSpec spec(String key){return layout().androidWidget(key);}
    private AndroidWidgetHostController controller(){return (AndroidWidgetHostController)field("androidWidgets");}
    private int controllerColumns()throws Exception{return (Integer)call("homeGridColumns",new Class<?>[0]);}
    private int controllerRows()throws Exception{return (Integer)call("homeGridRows",new Class<?>[0]);}
    private AppWidgetManager manager(){return AppWidgetManager.getInstance(this);}
    private Set<Integer> ids(){Set<Integer> s=new HashSet<>();for(int id:new AppWidgetHost(this,androidWidgetHostId()).getAppWidgetIds())s.add(id);return s;}
    private String providerVersion(){try{return getPackageManager().getPackageInfo(FIXTURE.getPackageName(),0).versionName;}catch(Exception e){return "missing";}}
    private Object field(String name){try{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(this);}catch(Exception e){throw new IllegalStateException(e);}}
    private void set(String name,Object value)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(this,value);}
    private Object call(String name,Class<?>[] types,Object...args)throws Exception{Method m=MainActivity.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(this,args);}
    private void choose(int index)throws Exception{Object menu=field("activeMenu");require(menu!=null,"Menu missing");call("selectMenuChoice",new Class<?>[]{menu.getClass(),int.class,int.class},menu,0,index);}
    private HomeLayout layout(){return (HomeLayout)field("layout");}
    private WidgetExpansion expanded(){return (WidgetExpansion)field("expandedWidget");}
    /** The native renderer hosts the dashboard and its modal plane in an embedded root, not this window. */
    private View dashboard(){View plane=dashboardOverlayRoot();return plane!=null?plane:getWindow().getDecorView();}
    private View expandedView(){WidgetExpansion e=expanded();if(e==null)return null;FrameLayout plane=dashboardOverlayRoot();return plane==null?(e.getWindow()==null?null:e.getWindow().getDecorView()):plane.getChildAt(plane.getChildCount()-1);}
    private void press(int code){dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,code));dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,code));}
    private static void keyTo(WidgetExpansion e,int code){e.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,code));e.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,code));}
    private static AppWidgetHostView host(View v,int id){if(v instanceof AppWidgetHostView&&((AppWidgetHostView)v).getAppWidgetId()==id)return (AppWidgetHostView)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){AppWidgetHostView f=host(((ViewGroup)v).getChildAt(i),id);if(f!=null)return f;}return null;}
    private static String hostText(View root,int id){AppWidgetHostView h=root==null?null:host(root,id);return h==null?"":text(h);}
    /** Text of the tile wrapper for a key: its label row plus provider view or placeholder. */
    private String tileText(String key){View v=findDescription(dashboard(),spec(key)==null?getString(R.string.hb_aw_unavailable):spec(key).label,key);return v==null?"":text(v);}
    private View findDescription(View v,String description,String key){
        if(v instanceof ViewGroup&&description.contentEquals(String.valueOf(v.getContentDescription()))&&text(v).contains(getString(R.string.hb_aw_unavailable)))return v;
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View f=findDescription(((ViewGroup)v).getChildAt(i),description,key);if(f!=null)return f;}return null;
    }
    private static int liveViews(View v){if(v==null)return 0;if(v instanceof AppWidgetHostView)return text(v).contains("Widget bound across apps")?1:0;int n=0;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)n+=liveViews(((ViewGroup)v).getChildAt(i));return n;}
    private static View find(View v,String label){if(v==null)return null;if(v instanceof TextView&&label.contentEquals(((TextView)v).getText()))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View f=find(((ViewGroup)v).getChildAt(i),label);if(f!=null)return f;}return null;}
    private static String text(View v){StringBuilder b=new StringBuilder();collect(v,b);return b.toString();}
    private static void collect(View v,StringBuilder b){if(v instanceof TextView)b.append(((TextView)v).getText()).append('\n');if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)collect(((ViewGroup)v).getChildAt(i),b);}
    private static int count(String haystack,String needle){int n=0,i=0;while((i=haystack.indexOf(needle,i))>=0){n++;i+=needle.length();}return n;}
    private static int number(String text,String prefix){int i=text.indexOf(prefix);if(i<0)return -1;int j=i+prefix.length(),k=j;while(k<text.length()&&Character.isDigit(text.charAt(k)))k++;return k>j?Integer.parseInt(text.substring(j,k)):-1;}
    private static String sizeOf(String text){java.util.regex.Matcher m=java.util.regex.Pattern.compile("(\\d+×\\d+) dp").matcher(text);return m.find()?m.group(1):"";}
    private static String line(String text){return text.replace('\n',' ').trim();}
    private void require(boolean value,String reason){if(!value)throw new IllegalStateException(reason);}
    private static void put(String k,Object v){try{facts.put(k,v);}catch(JSONException ignored){}}
    private void finish(String status,String detail){
        release();
        if(!household.equals(super.getSharedPreferences("widget-home",0).getAll())){status="fail";detail="Household settings changed; "+detail;}
        report(status,detail);
    }
    private void release(){
        AppWidgetHost h=new AppWidgetHost(this,androidWidgetHostId());for(int id:h.getAppWidgetIds())h.deleteAppWidgetId(id);
        put("idsAfterRelease",h.getAppWidgetIds().length);
        if(survivor())super.getSharedPreferences("android-widget-survivor-widget-home",0).edit().clear().commit();
    }
    private void report(String status,String detail){
        try{JSONObject r=new JSONObject(facts.toString()).put("status",status).put("mode",mode()).put("detail",detail).put("at",Instant.now().toString())
                .put("homeboardVersion",getPackageManager().getPackageInfo(getPackageName(),0).versionName).put("provider",providerVersion());
            android.util.Log.i(TAG,r.toString());
            Files.write(new File(getExternalFilesDir(null),"wf.log").toPath(),r.toString().getBytes(StandardCharsets.UTF_8));}
        catch(Exception ignored){}
    }
    /** Survivor keep/reopen leave their binding for the next job; recreation must not release it either. */
    @Override protected void onDestroy(){
        steps.removeCallbacksAndMessages(null);super.onDestroy();
        if(!restarting&&!survivor())release();
    }
}
