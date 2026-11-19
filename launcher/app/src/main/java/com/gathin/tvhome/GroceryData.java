package com.gathin.tvhome;

import org.json.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/** Household-entered products and current needs. No purchase or inferred consumption semantics. */
final class GroceryData {
    static final String DEFAULT="groceries";
    static JSONObject emptyList(String id,String title)throws JSONException {
        return new JSONObject().put("id",id).put("title",title).put("items",new JSONArray()).put("products",new JSONArray());
    }
    static JSONObject defaults(){try{return new JSONObject().put(DEFAULT,emptyList(DEFAULT,""));}catch(JSONException e){throw new IllegalStateException(e);}}
    static JSONObject copy(JSONObject value){try{return new JSONObject(value.toString());}catch(JSONException e){throw new IllegalStateException(e);}}
    static JSONObject find(JSONArray records,String id){for(int i=0;i<records.length();i++){JSONObject r=records.optJSONObject(i);if(r!=null&&id.equals(r.optString("id")))return r;}return null;}
    static String text(JSONObject item){String q=item.optString("quantity");JSONObject structured=item.optJSONObject("structured");if(structured!=null)q=structured.optString("amount")+" "+structured.optString("unit");return item.optString("name")+(q.isEmpty()?"":" · "+q);}
    /** Familiar display units without rounding the exact household report. */
    static String display(String value,String unit){
        if(value.isEmpty())return "";
        BigDecimal n=decimal(value,false);
        if("ml".equals(unit)||"g".equals(unit))return n.movePointLeft(3).stripTrailingZeros().toPlainString()+" "+("ml".equals(unit)?"L":"kg");
        return n.stripTrailingZeros().toPlainString()+" "+unit;
    }
    static void string(JSONObject o,String key,int max,boolean required)throws JSONException {
        Object v=o.get(key);if(!(v instanceof String)||((String)v).codePointCount(0,((String)v).length())>max||required&&((String)v).trim().isEmpty())throw new PhoneConfiguration.Invalid("groceries",key+": invalid or too long");
    }
    static BigDecimal decimal(String s,boolean positive){
        if(!s.matches("[0-9]{1,9}(\\.[0-9]{1,6})?"))throw new PhoneConfiguration.Invalid("groceries","Use a nonnegative decimal with at most six fractional digits");
        BigDecimal n=new BigDecimal(s);if(positive&&n.signum()==0)throw new PhoneConfiguration.Invalid("groceries","Amount must be greater than zero");return n;
    }
    static String dimension(String unit){return switch(unit){case "g","kg"->"g";case "ml","l"->"ml";case "piece","dozen"->"piece";default->"";};}
    static BigDecimal factor(String unit){return switch(unit){case "kg","l"->new BigDecimal("1000");case "dozen"->new BigDecimal("12");default->BigDecimal.ONE;};}
    static BigDecimal base(JSONObject product,String amount,String unit){
        BigDecimal n=decimal(amount,true);String target=product.optString("baseUnit");
        if(!dimension(unit).isEmpty()&&dimension(unit).equals(target))return n.multiply(factor(unit));
        if(unit.equals(product.optString("packageUnit"))&&!product.optString("packageSize").isEmpty())return n.multiply(decimal(product.optString("packageSize"),true));
        throw new PhoneConfiguration.Invalid("groceries","Incompatible or unknown units");
    }
    /** Suggested purchase amount in package units, or the configured loose-goods increment. */
    static String suggestion(JSONObject p){
        if(!p.optBoolean("staple"))return "";
        if("manual".equals(p.optString("tracking")))return "need".equals(p.optString("stockState"))?p.optString("usualAmount"):"";
        if(!"quantity".equals(p.optString("tracking"))||p.optString("stock").isEmpty())return "";
        BigDecimal stock=decimal(p.optString("stock"),false),threshold=decimal(p.optString("threshold"),false),target=decimal(p.optString("target"),true);
        if(stock.compareTo(threshold)>0)return "";
        BigDecimal step=p.optString("packageSize").isEmpty()?decimal(p.optString("increment"),true):decimal(p.optString("packageSize"),true);
        BigDecimal count=target.subtract(stock).divide(step,0,RoundingMode.CEILING);
        return (p.optString("packageSize").isEmpty()?count.multiply(step):count).stripTrailingZeros().toPlainString();
    }
    static String suggestionUnit(JSONObject p){if("manual".equals(p.optString("tracking")))return p.optString("usualUnit");return p.optString("packageSize").isEmpty()?p.optString("baseUnit"):p.optString("packageUnit");}
    static void validate(JSONObject book)throws JSONException {
        if(book.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>262144)throw new PhoneConfiguration.Invalid("groceries","Grocery data exceeds 256 KiB");
        if(book.length()>32)throw new PhoneConfiguration.Invalid("groceries","At most 32 lists");
        for(Iterator<String> keys=book.keys();keys.hasNext();){String id=keys.next();JSONObject list=book.getJSONObject(id);PhoneConfiguration.keys(list,"groceries","id","title","items","products");string(list,"id",100,true);if(!id.equals(list.getString("id")))throw new PhoneConfiguration.Invalid("groceries","List identity mismatch");string(list,"title",120,false);
            JSONArray products=list.getJSONArray("products"),items=list.getJSONArray("items");if(products.length()>200||items.length()>200)throw new PhoneConfiguration.Invalid("groceries","At most 200 items and products per list");
            Set<String> ids=new HashSet<>();for(int i=0;i<products.length();i++){
                JSONObject p=products.getJSONObject(i);PhoneConfiguration.keys(p,"groceries","id","name","brand","variant","aliases","category","staple","usualQuantity","usualAmount","usualUnit","baseUnit","packageUnit","packageSize","tracking","stockState","stock","reported","threshold","target","increment");
                string(p,"id",100,true);if(!ids.add(p.getString("id")))throw new PhoneConfiguration.Invalid("groceries","Duplicate product");string(p,"name",120,true);
                for(String k:List.of("brand","variant","aliases","category"))string(p,k,k.equals("aliases")?500:120,false);
                for(String k:List.of("usualQuantity","usualAmount","usualUnit","baseUnit","packageUnit","packageSize","tracking","stockState","stock","reported","threshold","target","increment"))string(p,k,40,false);
                if(!(p.get("staple") instanceof Boolean)||!List.of("none","manual","quantity").contains(p.getString("tracking"))||!List.of("unknown","enough","need").contains(p.getString("stockState")))throw new PhoneConfiguration.Invalid("groceries","Invalid stock policy");
                if(!p.getString("baseUnit").isEmpty()&&!List.of("g","ml","piece").contains(p.getString("baseUnit")))throw new PhoneConfiguration.Invalid("groceries","Invalid stock unit");
                if(!p.getString("packageSize").isEmpty()){
                    decimal(p.getString("packageSize"),true);if(p.getString("packageUnit").trim().isEmpty()||p.getString("baseUnit").isEmpty()||!dimension(p.getString("packageUnit")).isEmpty())throw new PhoneConfiguration.Invalid("groceries","Package needs a distinct name and a base unit");
                }
                if(!p.getString("usualAmount").isEmpty())decimal(p.getString("usualAmount"),true);
                if(!p.getString("reported").isEmpty())try{java.time.Instant.parse(p.getString("reported"));}catch(java.time.DateTimeException invalid){throw new PhoneConfiguration.Invalid("groceries","Invalid stock report date");}
                if("quantity".equals(p.getString("tracking"))){
                    if(p.getString("baseUnit").isEmpty())throw new PhoneConfiguration.Invalid("groceries","Stock needs a base unit");
                    if(decimal(p.getString("target"),true).compareTo(decimal(p.getString("threshold"),false))<=0)throw new PhoneConfiguration.Invalid("groceries","Target must exceed threshold");
                    decimal(p.getString("increment"),true);if(!p.getString("stock").isEmpty()){decimal(p.getString("stock"),false);if(p.getString("reported").isEmpty())throw new PhoneConfiguration.Invalid("groceries","Stock report time required");}
                }
            }
            ids.clear();Set<String> linked=new HashSet<>();for(int i=0;i<items.length();i++){
                JSONObject item=items.getJSONObject(i);PhoneConfiguration.keys(item,"groceries","id","name","quantity","note","productId","structured");string(item,"id",100,true);if(!ids.add(item.getString("id")))throw new PhoneConfiguration.Invalid("groceries","Duplicate item");string(item,"name",120,true);string(item,"quantity",40,false);string(item,"note",500,false);string(item,"productId",100,false);
                JSONObject product=find(products,item.getString("productId"));if(!item.getString("productId").isEmpty()&&(product==null||!linked.add(item.getString("productId"))))throw new PhoneConfiguration.Invalid("groceries","Missing product or duplicate product need");
                if(item.has("structured")){JSONObject q=item.getJSONObject("structured");PhoneConfiguration.keys(q,"groceries","amount","unit");string(q,"amount",40,true);string(q,"unit",40,true);decimal(q.getString("amount"),true);if(product==null)throw new PhoneConfiguration.Invalid("groceries","Structured amount needs a product");base(product,q.getString("amount"),q.getString("unit"));}
            }
        }
    }
}
