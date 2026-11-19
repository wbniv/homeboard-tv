package com.gathin.tvhome;

import org.json.*;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

/** Container command-line entry for the same pure Java parsers/repository as Android. */
public final class CalendarCli {
    public static void main(String[] args)throws Exception{
        if(args[0].equals("parse")){
            String kind=args[1];int year=Integer.parseInt(args[2]);String url=args[3],body=read(args[4]);
            List<HolidayParsers.Update> updates;
            switch(kind){case "boe":updates=Collections.singletonList(HolidayParsers.boe(body,year,url));break;
                case "china":updates=Collections.singletonList(HolidayParsers.china(body,year,url));break;
                case "tivissa":updates=Collections.singletonList(HolidayParsers.tivissa(body,year,url));break;
                case "thai":updates=Collections.singletonList(HolidayParsers.thai(body,year,url));break;
                case "opm":updates=HolidayParsers.opm(body,url);break;default:throw new IllegalArgumentException("Unknown parser");}
            JSONArray values=new JSONArray();for(HolidayParsers.Update update:updates)values.put(new JSONObject().put("year",update.year).put("scopes",new JSONArray(update.scopes)).put("events",update.events));
            Files.write(Paths.get(args[5]),values.toString(2).getBytes(StandardCharsets.UTF_8));
            System.out.println(new JSONObject().put("parser",kind).put("years",values.length()).toString());
        }else if(args[0].equals("refresh")){
            HolidayStore store=new HolidayStore(new File(args[2]),read(args[1]));
            HolidayUpdater updater=new HolidayUpdater(store,HolidayHttp.network(new File(args[2],"captures")));
            updater.run(Instant.parse(args[4]),new HashSet<>(Arrays.asList(args[3].split(","))),true,key->System.out.println("Checking "+key));
            System.out.println(store.checks().toString(2));
        }else throw new IllegalArgumentException("Use parse or refresh");
    }
    private static String read(String path)throws Exception{return new String(Files.readAllBytes(Paths.get(path)),StandardCharsets.UTF_8);}
}
