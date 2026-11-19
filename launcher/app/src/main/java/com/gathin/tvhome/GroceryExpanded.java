package com.gathin.tvhome;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import org.json.*;
import java.util.function.Supplier;

/** Full names, quantities, product/stock details and notes beyond the compact glance. */
final class GroceryExpanded implements WidgetExpansion.Content {
    private final WidgetExpansion h;
    private final Supplier<JSONObject> source;
    private final LinearLayout root,rows;
    private final TextView detail;
    private String selected="";
    private boolean staples;
    GroceryExpanded(WidgetExpansion host,Supplier<JSONObject> source,Runnable configure,Runnable phone,Bundle state){
        h=host;this.source=source;selected=state.getString("selected","");staples=state.getBoolean("staples");root=h.column();LinearLayout actions=h.row();
        actions.addView(h.button(R.string.hb_grocery_list,()->{staples=false;selected="";refresh();}));actions.addView(h.button(R.string.hb_grocery_staples,()->{staples=true;selected="";refresh();}));
        actions.addView(h.button(R.string.hb_grocery_phone,phone));
        LinearLayout body=h.row();root.addView(body,new LinearLayout.LayoutParams(-1,0,1));ScrollView scroll=new ScrollView(h.activity);rows=h.column();scroll.addView(rows);body.addView(scroll,new LinearLayout.LayoutParams(0,-1,1));detail=h.detailPane(body,1);root.addView(actions);refresh();
    }
    private String t(int id){return h.activity.getString(id);}
    private void refresh(){
        rows.removeAllViews();JSONObject list=source.get();if(list==null){detail.setText(t(R.string.hb_grocery_unavailable));return;}
        JSONArray records=list.optJSONArray(staples?"products":"items");boolean any=false;JSONObject chosen=null;
        for(int i=0;i<records.length();i++){JSONObject item=records.optJSONObject(i);if(staples&&!item.optBoolean("staple"))continue;any=true;
            if(chosen==null||selected.equals(item.optString("id")))chosen=item;
            Button row=h.button(staples?item.optString("name"):GroceryData.text(item),()->h.details(item.optString("name"),describe(list,item)));
            row.setOnFocusChangeListener((v,f)->{row.setBackground(h.background(f));if(f){selected=item.optString("id");detail.setText(describe(list,item));}});rows.addView(row,new LinearLayout.LayoutParams(-1,-2));
        }
        if(!any){rows.addView(h.text(t(staples?R.string.hb_grocery_no_staples:R.string.hb_grocery_empty),20));detail.setText(t(R.string.hb_grocery_phone_hint));}
        else {selected=chosen.optString("id");detail.setText(describe(list,chosen));}
    }
    private String describe(JSONObject list,JSONObject row){
        JSONObject p=staples?row:GroceryData.find(list.optJSONArray("products"),row.optString("productId"));StringBuilder s=new StringBuilder(staples?row.optString("name"):GroceryData.text(row));
        if(!staples&&!row.optString("note").isEmpty())s.append("\n\n").append(row.optString("note"));
        if(p!=null){s.append("\n\n").append(p.optString("brand")).append(" ").append(p.optString("variant"));if(!p.optString("category").isEmpty())s.append("\n").append(p.optString("category"));
            if(!p.optString("packageSize").isEmpty())s.append("\n1 ").append(p.optString("packageUnit")).append(" = ").append(GroceryData.display(p.optString("packageSize"),p.optString("baseUnit")));
            if(!staples&&row.has("structured"))try{JSONObject q=row.getJSONObject("structured");s.append("\n= ").append(GroceryData.base(p,q.getString("amount"),q.getString("unit")).stripTrailingZeros().toPlainString()).append(" ").append(p.optString("baseUnit"));}catch(JSONException ignored){}
            if(p.optBoolean("staple")){s.append("\n\n").append(t(R.string.hb_grocery_staples));String mode=p.optString("tracking");
                if("quantity".equals(mode)){s.append("\n").append(t(R.string.hb_grocery_stock)).append(": ").append(p.optString("stock").isEmpty()?t(R.string.hb_grocery_unknown):GroceryData.display(p.optString("stock"),p.optString("baseUnit")));s.append("\n").append(t(R.string.hb_grocery_threshold)).append(": ").append(GroceryData.display(p.optString("threshold"),p.optString("baseUnit"))).append(" · ").append(t(R.string.hb_grocery_target)).append(": ").append(GroceryData.display(p.optString("target"),p.optString("baseUnit")));}
                else s.append("\n").append(t("manual".equals(mode)?("need".equals(p.optString("stockState"))?R.string.hb_grocery_need:"enough".equals(p.optString("stockState"))?R.string.hb_grocery_enough:R.string.hb_grocery_unknown):R.string.hb_grocery_not_tracked));
                if(!p.optString("reported").isEmpty())s.append("\n").append(t(R.string.hb_grocery_reported)).append(": ").append(java.time.Instant.parse(p.optString("reported")).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                String suggestion=GroceryData.suggestion(p);if(!suggestion.isEmpty())s.append("\n").append(t(R.string.hb_grocery_suggested)).append(": ").append(suggestion).append(" ").append(GroceryData.suggestionUnit(p));
            }
        }return s.toString();
    }
    public View view(){return root;}
    public Bundle state(){Bundle b=new Bundle();b.putString("selected",selected);b.putBoolean("staples",staples);return b;}
}
