package com.gathin.tvhome;

import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.time.*;
import java.util.*;

/** Durable provider revisions; display ranges never trim the offline annual cache. */
class HolidayStore {
    private final Path directory;
    private JSONObject state;
    volatile boolean storageFailed;
    HolidayStore(File directory,String seed) throws Exception {
        this.directory=directory.toPath();try{Files.createDirectories(this.directory);}catch(IOException failure){storageFailed=true;}
        state=load("active.json");if(state==null)state=load("previous.json");
        if(state==null){state=new JSONObject().put("format",1).put("snapshot",new JSONObject(seed)).put("checks",new JSONObject());validate(state);try{persist(state);}catch(IOException failure){storageFailed=true;}}
    }
    private JSONObject load(String name){try{
        JSONObject value=new JSONObject(new String(Files.readAllBytes(directory.resolve(name)),StandardCharsets.UTF_8));validate(value);return value;
    }catch(Exception invalid){return null;}}
    private static void validate(JSONObject value)throws Exception{
        if(value.getInt("format")!=1)throw new IOException("Unknown holiday store format");
        CalendarSnapshot.parse(value.getJSONObject("snapshot").toString());value.getJSONObject("checks");
    }
    synchronized String snapshot()throws JSONException{return state.getJSONObject("snapshot").toString();}
    synchronized JSONObject checks()throws JSONException{return new JSONObject(state.getJSONObject("checks").toString());}
    synchronized void check(String key,long attempted,boolean success,String error)throws Exception{
        JSONObject next=new JSONObject(state.toString()),checks=next.getJSONObject("checks");
        JSONObject prior=checks.optJSONObject(key),entry=prior==null?new JSONObject():new JSONObject(prior.toString());
        int failures=success?0:Math.min(8,entry.optInt("failures",0)+1);
        entry.put("attempt",attempted).put("error",success?"":error).put("failures",failures);
        if(success)entry.put("success",attempted);
        entry.put("retryAt",attempted+(success?86_400_000L:Math.min(86_400_000L,3_600_000L*(1L<<Math.max(0,failures-1)))));
        checks.put(key,entry);publish(next);
    }
    synchronized boolean due(String key,long now,long interval)throws JSONException{
        JSONObject check=state.getJSONObject("checks").optJSONObject(key);
        if(check==null)return true;
        long attempt=check.optLong("attempt",0);
        // A clock correction must not postpone updates indefinitely.
        if(now<attempt)return true;
        return now>=(check.optInt("failures",0)>0?check.optLong("retryAt",0):Math.min(check.optLong("retryAt",0),attempt+interval));
    }
    synchronized void accept(String id,int year,Set<String> scopes,JSONArray events,String source,String revision,boolean complete)throws Exception{
        if(CalendarSnapshot.calculated(id)||!Arrays.asList("thai","spain","usa","china").contains(id)||year<1900||year>2200||scopes.isEmpty())throw new IOException("Invalid holiday update target");
        JSONObject next=new JSONObject(state.toString()),root=next.getJSONObject("snapshot");JSONArray providers=root.getJSONArray("providers");JSONObject provider=null;
        if(root.has("inputHashes"))root.put("seedInputHashes",root.remove("inputHashes"));root.put("stage","runtime").put("producerVersion","calendar-java-1");
        for(int i=0;i<providers.length();i++)if(providers.getJSONObject(i).getString("id").equals(id))provider=providers.getJSONObject(i);
        if(provider==null)throw new IOException("Missing provider seed");
        Map<String,JSONObject> merged=new LinkedHashMap<>();JSONArray old=provider.getJSONArray("events");int replaced=0;
        for(int i=0;i<old.length();i++){
            JSONObject e=old.getJSONObject(i);boolean target=LocalDate.parse(e.getString("date")).getYear()==year&&scopes.contains(e.getString("scope"));
            if(target)replaced++;
            if(!target||!complete)merged.put(e.getString("id"),e);
        }
        if(events.length()==0||(complete&&replaced>0&&events.length()*4<replaced*3))throw new IOException("Unexpected empty or reduced publication; retained previous calendar");
        for(int i=0;i<events.length();i++){
            JSONObject e=events.getJSONObject(i);
            if(LocalDate.parse(e.getString("date")).getYear()!=year||!scopes.contains(e.getString("scope")))throw new IOException("Update year/scope mismatch");
            merged.put(e.getString("id"),e);
        }
        JSONArray combined=new JSONArray();for(JSONObject e:merged.values())combined.put(e);
        provider.put("events",combined).put("status","offline");
        LocalDate start=LocalDate.parse(provider.getString("coverageStart")),end=LocalDate.parse(provider.getString("coverageEnd"));
        if(start.getYear()>year)provider.put("coverageStart",LocalDate.of(year,1,1).toString());
        if(end.getYear()<year)provider.put("coverageEnd",LocalDate.of(year,12,31).toString());
        JSONObject coverage=provider.optJSONObject("coverage");if(coverage==null){coverage=new JSONObject();provider.put("coverage",coverage);}
        JSONObject annual=coverage.optJSONObject(String.valueOf(year));if(annual==null){annual=new JSONObject();coverage.put(String.valueOf(year),annual);}
        for(String scope:scopes)if(complete||!annual.optString(scope).equals("complete"))annual.put(scope,complete?"complete":"partial");
        provider.put("revision",revision).put("updatedFrom",source);
        // US holidays use the publisher's civil year, not the TV's time zone.
        if(id.equals("usa"))provider.put("timeZone","America/New_York");
        validate(next);publish(next);
    }
    synchronized void prune(Instant now,Set<String> enabled)throws Exception{
        JSONObject next=new JSONObject(state.toString());JSONArray providers=next.getJSONObject("snapshot").getJSONArray("providers");boolean changed=false;
        for(int i=0;i<providers.length();i++){
            JSONObject provider=providers.getJSONObject(i);if(!enabled.contains(provider.getString("id")))continue;
            int year=now.atZone(ZoneId.of(provider.getString("timeZone"))).getYear();JSONObject coverage=provider.optJSONObject("coverage");if(coverage==null)continue;
            Set<String> scopes=new HashSet<>();Iterator<String> years=coverage.keys();
            while(years.hasNext()){JSONObject annual=coverage.getJSONObject(years.next());Iterator<String> keys=annual.keys();while(keys.hasNext()){String key=keys.next();if(annual.optString(key).equals("complete"))scopes.add(key);}}
            boolean ready=!scopes.isEmpty();for(int y=year;y<=year+1;y++)for(String scope:scopes){JSONObject annual=coverage.optJSONObject(String.valueOf(y));if(annual==null||!annual.optString(scope).equals("complete"))ready=false;}
            if(!ready)continue;
            JSONArray old=provider.getJSONArray("events"),events=new JSONArray();for(int n=0;n<old.length();n++){JSONObject e=old.getJSONObject(n);if(LocalDate.parse(e.getString("date")).getYear()>=year)events.put(e);}
            if(events.length()==old.length())continue;
            provider.put("events",events).put("coverageStart",LocalDate.of(year,1,1).toString());
            List<String> expired=new ArrayList<>();Iterator<String> keys=coverage.keys();while(keys.hasNext()){String key=keys.next();if(Integer.parseInt(key)<year)expired.add(key);}for(String key:expired)coverage.remove(key);changed=true;
        }
        if(changed)publish(next);
    }
    private void publish(JSONObject next)throws Exception{validate(next);try{persist(next);storageFailed=false;}catch(IOException failure){storageFailed=true;throw failure;}state=next;}
    private void persist(JSONObject next)throws IOException{
        if(state!=null&&Files.exists(directory.resolve("active.json")))try{
            if(!state.getJSONObject("snapshot").toString().equals(next.getJSONObject("snapshot").toString()))write("previous.json",state.toString());
        }catch(JSONException invalid){throw new IOException(invalid);}
        write("active.json",next.toString());
    }
    protected void write(String name,String value)throws IOException{
        Path tmp=directory.resolve(name+".tmp"),target=directory.resolve(name);
        try(FileOutputStream out=new FileOutputStream(tmp.toFile())){out.write(value.getBytes(StandardCharsets.UTF_8));out.getFD().sync();}
        Files.move(tmp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        try(FileChannel channel=FileChannel.open(directory,StandardOpenOption.READ)){channel.force(true);}
    }
}
