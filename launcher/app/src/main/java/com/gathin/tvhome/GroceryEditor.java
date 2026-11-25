package com.gathin.tvhome;

import android.app.Activity;
import android.app.Dialog;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.UUID;
import java.util.function.BiConsumer;

/** Remote fallback. Draft mutations are committed together, or discarded. */
final class GroceryEditor extends DashboardDialog {
    private final Activity a;private final PaletteColors palette;private final JSONObject draft;private final String original;private String listId;
    private final BiConsumer<JSONObject,String> save;private final LinearLayout root;private boolean editing;
    GroceryEditor(Activity a,JSONObject book,String listId,PaletteColors palette,BiConsumer<JSONObject,String> save){
        super(a);this.a=a;this.palette=palette;this.save=save;draft=GroceryData.copy(book);original=book.toString();this.listId=listId;
        requestWindowFeature(Window.FEATURE_NO_TITLE);root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(20,16,20,16);root.setBackgroundColor(palette.surface);setContentView(root);
        setOnKeyListener((d,k,e)->{if(k==KeyEvent.KEYCODE_BACK||k==KeyEvent.KEYCODE_ESCAPE){if(e.getAction()==KeyEvent.ACTION_UP){if(editing)main();else leave();}return true;}return false;});main();
    }
    private String t(int id){return a.getString(id);}
    private TextView text(String s){TextView v=new TextView(a);v.setText(s);v.setTextColor(palette.text);v.setTextSize(18);return v;}
    private void button(LinearLayout p,String label,Runnable action){Button b=new Button(a);b.setText(label);b.setAllCaps(false);b.setOnClickListener(v->action.run());p.addView(b,new LinearLayout.LayoutParams(-1,-2));}
    private EditText input(LinearLayout p,String label,String value){p.addView(text(label));EditText e=new EditText(a);e.setTextColor(palette.text);e.setText(value);e.setContentDescription(label);e.setSingleLine(true);p.addView(e);return e;}
    private JSONObject list(){return draft.optJSONObject(listId);}
    private LinearLayout screen(){root.removeAllViews();root.addView(text(t(R.string.hb_grocery_draft)));ScrollView scroll=new ScrollView(a);LinearLayout body=new LinearLayout(a);body.setOrientation(LinearLayout.VERTICAL);scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));return body;}
    private void main(){editing=false;LinearLayout p=screen();JSONObject list=list();if(list==null){p.addView(text(t(R.string.hb_grocery_unavailable)));button(p,t(R.string.hb_fs_back),this::dismiss);return;}
        p.addView(text(list.optString("title").isEmpty()?t(R.string.hb_grocery_title):list.optString("title")));
        button(p,t(R.string.hb_grocery_lists),()->{LinearLayout choices=screen();for(java.util.Iterator<String> i=draft.keys();i.hasNext();){String id=i.next();String label=draft.optJSONObject(id).optString("title");button(choices,label.isEmpty()?t(R.string.hb_grocery_title):label,()->{listId=id;main();});}});
        button(p,t(R.string.hb_grocery_add),()->item(null));JSONArray items=list.optJSONArray("items");for(int i=0;i<items.length();i++){JSONObject row=items.optJSONObject(i);button(p,GroceryData.text(row),()->item(row));}
        button(p,t(R.string.hb_wc_save),()->{try{GroceryData.validate(draft);save.accept(GroceryData.copy(draft),listId);dismiss();}catch(Exception error){Toast.makeText(a,error.getMessage(),Toast.LENGTH_LONG).show();}});
        button(p,t(R.string.hb_cancel),this::leave);
    }
    private void item(JSONObject saved){editing=true;LinearLayout p=screen();EditText name=input(p,t(R.string.hb_grocery_name),saved==null?"":saved.optString("name")),quantity=input(p,t(R.string.hb_grocery_quantity),saved==null?"":saved.optString("quantity")),note=input(p,t(R.string.hb_grocery_note),saved==null?"":saved.optString("note"));
        if(saved!=null&&saved.has("structured")){quantity.setText(GroceryData.text(saved));quantity.setEnabled(false);p.addView(text(t(R.string.hb_grocery_phone_hint)));}
        button(p,t(R.string.hb_wc_done),()->{try{JSONObject row=saved==null?new JSONObject().put("id",UUID.randomUUID().toString()).put("productId",""):GroceryData.copy(saved);row.put("name",name.getText().toString().trim()).put("note",note.getText().toString());if(!row.has("structured"))row.put("quantity",quantity.getText().toString());
            JSONObject candidate=GroceryData.copy(draft);JSONArray items=candidate.getJSONObject(listId).getJSONArray("items");if(saved==null)items.put(row);else for(int i=0;i<items.length();i++)if(items.getJSONObject(i).getString("id").equals(saved.optString("id")))items.put(i,row);GroceryData.validate(candidate);draft.put(listId,candidate.getJSONObject(listId));main();
        }catch(Exception e){Toast.makeText(a,e.getMessage(),Toast.LENGTH_LONG).show();}});
        if(saved!=null)button(p,t(R.string.hb_grocery_remove),()->{JSONArray items=list().optJSONArray("items");for(int i=0;i<items.length();i++)if(saved.optString("id").equals(items.optJSONObject(i).optString("id"))){items.remove(i);break;}main();});
        button(p,t(R.string.hb_cancel),this::main);name.requestFocus();
    }
    private void leave(){if(original.equals(draft.toString())){dismiss();return;}LinearLayout p=screen();p.addView(text(t(R.string.hb_grocery_unsaved)));button(p,t(R.string.hb_wc_save),()->{try{GroceryData.validate(draft);save.accept(GroceryData.copy(draft),listId);dismiss();}catch(Exception e){Toast.makeText(a,e.getMessage(),Toast.LENGTH_LONG).show();}});button(p,t(R.string.hb_grocery_discard),this::dismiss);button(p,t(R.string.hb_grocery_keep_editing),this::main);}
    void open(){show();if(getWindow()!=null)setDashboardLayout(Math.min((int)(820*a.getResources().getDisplayMetrics().density),a.getResources().getDisplayMetrics().widthPixels-40),a.getResources().getDisplayMetrics().heightPixels-40);}
}
