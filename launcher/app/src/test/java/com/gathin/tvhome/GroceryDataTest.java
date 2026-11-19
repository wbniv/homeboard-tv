package com.gathin.tvhome;

import org.junit.Test;
import org.json.*;
import java.util.Set;
import static org.junit.Assert.*;

public class GroceryDataTest {
    static JSONObject product(String id)throws Exception {
        JSONObject p=new JSONObject().put("id",id).put("name","Oat milk").put("staple",true);
        for(String k:new String[]{"brand","variant","aliases","category","usualQuantity","usualAmount","usualUnit","baseUnit","packageUnit","packageSize","stock","reported","threshold","target"})p.put(k,"");
        return p.put("tracking","none").put("stockState","unknown").put("increment","1");
    }
    static JSONObject milk()throws Exception{return product("milk").put("baseUnit","ml").put("packageUnit","carton").put("packageSize","1000").put("tracking","quantity").put("stock","500").put("reported","2026-11-19T02:10:00Z").put("threshold","1000").put("target","3000");}
    static JSONObject item(String id,String product)throws Exception{return new JSONObject().put("id",id).put("name","นม · Milk").put("quantity","2 cartons").put("note","Unsweetened").put("productId",product);}
    @Test public void exactDimensionsPackagesAndNoDensityGuesses()throws Exception {
        JSONObject p=milk();assertEquals("1500",GroceryData.base(p,"1.5","l").stripTrailingZeros().toPlainString());assertEquals("2000",GroceryData.base(p,"2","carton").toPlainString());
        assertThrows(PhoneConfiguration.Invalid.class,()->GroceryData.base(p,"1","kg"));assertThrows(PhoneConfiguration.Invalid.class,()->GroceryData.base(p,"1","bag"));
        JSONObject eggs=product("eggs").put("baseUnit","piece");assertEquals("6",GroceryData.base(eggs,"0.5","dozen").stripTrailingZeros().toPlainString());
    }
    @Test public void thresholdsRoundingLooseIncrementsUnknownAndManual()throws Exception {
        JSONObject p=milk();assertEquals("3",GroceryData.suggestion(p));p.put("stock","1000");assertEquals("2",GroceryData.suggestion(p));p.put("stock","1000.000001");assertEquals("",GroceryData.suggestion(p));
        p.put("stock","");assertEquals("",GroceryData.suggestion(p));p.put("stock","500").put("packageSize","").put("increment","250");assertEquals("2500",GroceryData.suggestion(p));
        p.put("tracking","manual").put("stockState","need").put("usualAmount","2");assertEquals("2",GroceryData.suggestion(p));p.put("stockState","enough");assertEquals("",GroceryData.suggestion(p));
    }
    @Test public void removingWidgetAndNeedRetainsStapleAndRoundTrip()throws Exception {
        HomeLayout h=new HomeLayout();h.addPanel(HomeLayout.GROCERY);JSONObject list=h.grocery(HomeLayout.GROCERY);list.getJSONArray("products").put(milk());list.getJSONArray("items").put(item("a","milk"));
        GroceryData.validate(h.groceries);h.delete(HomeLayout.GROCERY,true);assertEquals(1,h.groceries.getJSONObject("groceries").getJSONArray("products").length());
        h.addPanel(HomeLayout.GROCERY+":2");assertEquals("Unsweetened",h.grocery(HomeLayout.GROCERY+":2").getJSONArray("items").getJSONObject(0).getString("note"));
        HomeLayout parsed=HomeLayout.parse(h.encode());assertEquals(h.encode(),parsed.encode());parsed.grocery(HomeLayout.GROCERY+":2").getJSONArray("items").remove(0);assertEquals(1,parsed.grocery(HomeLayout.GROCERY+":2").getJSONArray("products").length());
    }
    @Test public void phoneSnapshotValidatesWholeBookAndReferencesAtomically()throws Exception {
        HomeLayout h=new HomeLayout();h.addPanel(HomeLayout.GROCERY);h.grocery(HomeLayout.GROCERY).getJSONArray("products").put(milk());JSONObject snapshot=PhoneConfiguration.snapshot(h,new JSONObject(),8,4);
        snapshot.getJSONObject("groceries").getJSONObject("groceries").getJSONArray("items").put(item("a","milk"));HomeLayout result=PhoneConfiguration.validate(snapshot,h,new JSONObject(),Set.of()).layout;assertEquals(1,result.grocery(HomeLayout.GROCERY).getJSONArray("items").length());assertEquals(0,h.grocery(HomeLayout.GROCERY).getJSONArray("items").length());
        snapshot.getJSONArray("widgets").getJSONObject(0).getJSONObject("settings").put("listId","missing");assertThrows(PhoneConfiguration.Invalid.class,()->PhoneConfiguration.validate(snapshot,h,new JSONObject(),Set.of()));
    }
    @Test public void rejectsBadStockMissingProductDuplicateNeedAndUnknownFields()throws Exception {
        JSONObject book=GroceryData.defaults(),l=book.getJSONObject("groceries"),p=milk();l.getJSONArray("products").put(p);GroceryData.validate(book);
        p.put("target","1000");assertThrows(PhoneConfiguration.Invalid.class,()->GroceryData.validate(book));p.put("target","3000");p.put("stock","-1");assertThrows(PhoneConfiguration.Invalid.class,()->GroceryData.validate(book));p.put("stock","500");
        l.getJSONArray("items").put(item("a","milk")).put(item("b","milk"));assertThrows(PhoneConfiguration.Invalid.class,()->GroceryData.validate(book));l.getJSONArray("items").remove(1);
        l.getJSONArray("items").getJSONObject(0).put("productId","absent");assertThrows(PhoneConfiguration.Invalid.class,()->GroceryData.validate(book));l.getJSONArray("items").getJSONObject(0).put("productId","milk").put("purchased",true);assertThrows(PhoneConfiguration.Invalid.class,()->GroceryData.validate(book));
    }
    @Test public void unicodeLimitsAndUnstructuredQuantitiesStayIntact()throws Exception {
        JSONObject book=GroceryData.defaults(),l=book.getJSONObject("groceries"),i=item("a","");i.put("name","🥛".repeat(120)).put("quantity","a few cartons");l.getJSONArray("items").put(i);GroceryData.validate(book);assertFalse(i.has("structured"));
        i.put("name","🥛".repeat(121));assertThrows(PhoneConfiguration.Invalid.class,()->GroceryData.validate(book));
        assertThrows(PhoneConfiguration.Invalid.class,()->GroceryData.decimal("1e3",true));assertThrows(PhoneConfiguration.Invalid.class,()->GroceryData.decimal("0.1234567",true));assertThrows(PhoneConfiguration.Invalid.class,()->GroceryData.decimal("0",true));
    }
}
