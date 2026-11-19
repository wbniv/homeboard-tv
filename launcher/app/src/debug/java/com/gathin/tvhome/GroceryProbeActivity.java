package com.gathin.tvhome;

import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.io.File;
import java.util.*;
import org.json.*;

/** Debug-only real native input/restoration proof in isolated preferences. */
public final class GroceryProbeActivity extends MainActivity {
    private final Handler steps=new Handler();private final Set<String> temporary=new HashSet<>();private Map<String,?> household;private String before;
    @Override public SharedPreferences getSharedPreferences(String name,int mode){
        if(name.equals("widget-home")||name.startsWith("rain-")||name.startsWith("air-quality-widget-")){
            name="grocery-proof-"+name;if(temporary.add(name))super.getSharedPreferences(name,mode).edit().clear().apply();
        }return super.getSharedPreferences(name,mode);
    }
    @Override protected void onCreate(Bundle state){
        household=new LinkedHashMap<>(super.getSharedPreferences("widget-home",0).getAll());
        try{
            HomeLayout seed=new HomeLayout();seed.homeColumns=8;seed.homeRows=4;seed.favoritesContainer=true;seed.addPanel(HomeLayout.GROCERY);seed.setPanelSpan(HomeLayout.GROCERY,4,2);seed.homePositions.put(HomeLayout.GROCERY,new HomeLayout.HomePosition(1,0,0));
            JSONObject list=seed.grocery(HomeLayout.GROCERY);list.put("title","Groceries / ของชำ");JSONObject p=new JSONObject().put("id","milk").put("name","Oat milk").put("staple",true);
            for(String k:new String[]{"brand","variant","aliases","category","usualQuantity","usualAmount","usualUnit","baseUnit","packageUnit","packageSize","stock","reported","threshold","target"})p.put(k,"");
            p.put("brand","Household brand").put("baseUnit","ml").put("packageUnit","carton").put("packageSize","1000").put("tracking","quantity").put("stockState","unknown").put("increment","1").put("stock","500").put("reported","2026-11-19T02:10:00Z").put("threshold","1000").put("target","3000");list.getJSONArray("products").put(p);
            list.getJSONArray("items").put(new JSONObject().put("id","milk-need").put("name","Oat milk").put("productId","milk").put("quantity","").put("structured",new JSONObject().put("amount","3").put("unit","carton")).put("note","Unsweetened · For breakfast / ไม่หวาน"));
            for(int i=0;i<9;i++)list.getJSONArray("items").put(new JSONObject().put("id","item-"+i).put("name",i==0?"Eggs / ไข่":"Produce "+i).put("productId","").put("quantity",i==0?"6":"a few").put("note","Sample detail "+i));
            GroceryData.validate(seed.groceries);
            getSharedPreferences("widget-home",0).edit().putString("layout",seed.encode()).putBoolean("single-screen-default-v1",true).putBoolean("favorites-container-v1",true).putInt("nav_home_page",1).putString("nav_focus",HomeLayout.GROCERY).putString("nav_home_focus",HomeLayout.GROCERY).putBoolean("nav_all_apps",false).commit();
        }catch(Exception e){throw new IllegalStateException(e);}super.onCreate(state);report("running","Starting isolated grocery acceptance");
        step(2500,()->{
            set("homePage",1);set("focused",HomeLayout.GROCERY);call("render",new Class<?>[]{int.class},0);
            JSONObject snapshot=phoneState(),body=new JSONObject().put("revision",snapshot.getLong("revision")).put("operation","grocery-native-proof-apply").put("draft",snapshot.getJSONObject("draft"));
            body.getJSONObject("draft").getJSONObject("groceries").getJSONObject("groceries").getJSONArray("items").getJSONObject(0).put("note","Updated from phone draft · For breakfast / ไม่หวาน");
            JSONObject applied=phoneApply(body);long revision=applied.getLong("revision");require(phoneApply(body).getLong("revision")==revision,"Receipt retry wrote twice");
            require(layout().grocery(HomeLayout.GROCERY).getJSONArray("items").getJSONObject(0).getString("note").startsWith("Updated"),"Phone aggregate did not save list");
            phoneUndo(new JSONObject().put("revision",revision).put("operation","grocery-native-proof-undo"));require(layout().grocery(HomeLayout.GROCERY).getJSONArray("items").getJSONObject(0).getString("note").startsWith("Unsweetened"),"Composite Undo lost note");
            set("homePage",1);set("focused",HomeLayout.GROCERY);call("render",new Class<?>[]{int.class},0);before=layout().encode();
        });
        step(4500,()->press(KeyEvent.KEYCODE_DPAD_CENTER));
        step(6500,()->whenExpanded(this::checkDetails,50));
    }
    private void checkDetails()throws Exception{
        View item=find(expanded().getWindow().getDecorView(),"Oat milk · 3 carton");require(item!=null,"Quantity row missing");item.performClick();
        step(700,()->{
            require(text(expanded().getWindow().getDecorView()).contains("3000"),"Product quantity breakdown missing");back(expanded());
            step(700,()->{back(expanded());whenRestored(this::checkHold,50);});
        });
    }
    private void checkHold(){
        dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_CENTER));
        step(900,()->{
            dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_CENTER));
            step(500,()->{
                require(field("overlay")!=null,"Hold OK did not open actions");require(expanded()==null,"Hold release also expanded");press(KeyEvent.KEYCODE_BACK);
                step(500,()->{press(KeyEvent.KEYCODE_DPAD_CENTER);whenExpanded(this::checkRestore,50);});
            });
        });
    }
    private void checkRestore()throws Exception{
        View restore=find(expanded().getWindow().getDecorView(),getString(R.string.hb_fs_restore));require(restore!=null,"Missing visible Restore dashboard");restore.performClick();
        whenRestored(()->{press(KeyEvent.KEYCODE_DPAD_CENTER);whenExpanded(this::checkStock,50);},50);
    }
    private void checkStock()throws Exception{
        View staples=find(expanded().getWindow().getDecorView(),getString(R.string.hb_grocery_staples));require(staples!=null,"Missing Staples tab");staples.performClick();
        require(text(expanded().getWindow().getDecorView()).contains("500"),"Reported stock missing");require(text(expanded().getWindow().getDecorView()).contains("3 carton"),"Rounded suggestion missing");
        require(household.equals(super.getSharedPreferences("widget-home",0).getAll()),"Household data changed");
        report("pass","Short/hold OK, nested Back, visible Restore, page/focus/size and data unchanged; native phone aggregate Apply/retry/Undo; staple stock detail");
    }
    private void whenRestored(Stage action,int remaining)throws Exception{
        if(expanded()==null){restored();action.run();return;}
        require(remaining>0,"Timed out waiting for dashboard restoration");step(200,()->whenRestored(action,remaining-1));
    }

    private void whenExpanded(Stage action,int remaining)throws Exception{
        WidgetExpansion e=expanded();if(e!=null&&text(e.getWindow().getDecorView()).contains("Unsweetened")){action.run();return;}
        require(remaining>0,"Timed out waiting for expanded grocery content");step(200,()->whenExpanded(action,remaining-1));
    }
    private void restored()throws Exception{require(expanded()==null,"Expansion retained");require(HomeLayout.GROCERY.equals(field("focused")),"Focus lost");require((int)field("homePage")==1,"Page lost");require(before.equals(layout().encode()),"Browsing changed saved data or geometry");}
    private WidgetExpansion expanded()throws Exception{return (WidgetExpansion)field("expandedWidget");}
    private HomeLayout layout()throws Exception{return (HomeLayout)field("layout");}
    private Object field(String key)throws Exception{Field f=MainActivity.class.getDeclaredField(key);f.setAccessible(true);return f.get(this);}
    private void set(String key,Object v)throws Exception{Field f=MainActivity.class.getDeclaredField(key);f.setAccessible(true);f.set(this,v);}
    private Object call(String key,Class<?>[] types,Object...args)throws Exception{Method m=MainActivity.class.getDeclaredMethod(key,types);m.setAccessible(true);return m.invoke(this,args);}
    private void press(int key){dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,key));dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,key));}
    private void back(WidgetExpansion e){e.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BACK));e.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BACK));}
    private View find(View v,String label){if(v instanceof TextView&&((TextView)v).getText().toString().equals(label))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View found=find(((ViewGroup)v).getChildAt(i),label);if(found!=null)return found;}return null;}
    private String text(View v){String s=v instanceof TextView?((TextView)v).getText().toString():"";if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)s+="\n"+text(((ViewGroup)v).getChildAt(i));return s;}
    private interface Stage{void run()throws Exception;}
    private void step(long ms,Stage stage){steps.postDelayed(()->{try{stage.run();}catch(Exception e){steps.removeCallbacksAndMessages(null);report("fail",e.toString());}},ms);}
    private void require(boolean yes,String message){if(!yes)throw new IllegalStateException(message);}
    private void report(String status,String message){try{JSONObject r=new JSONObject().put("status",status).put("detail",message).put("homeboardVersion",getPackageManager().getPackageInfo(getPackageName(),0).versionName);Files.write(new File(getExternalFilesDir(null),"wf.log").toPath(),r.toString().getBytes(StandardCharsets.UTF_8));}catch(Exception ignored){}}
    @Override protected void onDestroy(){steps.removeCallbacksAndMessages(null);super.onDestroy();}
}
