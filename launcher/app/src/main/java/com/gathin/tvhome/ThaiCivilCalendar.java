package com.gathin.tvhome;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Civil Suriyayatra calendar. Adapted from pythaidate CsDate/LSYear (MIT,
 * Copyright 2023 Mark Hollow), pinned in vendor/pythaidate/provenance.json.
 * This is not the separate Pakkhakhananaa reckoning or an astronomical phase model. */
final class ThaiCivilCalendar {
    static final int FIRST_YEAR = 2024, LAST_YEAR = 2027;
    static final String REVISION = "suriyayatra-f526c4d-java1";
    private static final Map<Integer,Year> YEARS = new ConcurrentHashMap<>();
    private static final int[][] OFFSETS = {
        {0,29,59,88,118,147,177,206,236,265,295,324,354,383},
        {0,29,59,89,119,148,178,207,237,266,296,325,355,384},
        {0,29,59,88,118,148,177,207,236,266,295,325,354,384}
    };
    private static final int[][] MONTHS = {
        {5,6,7,8,9,10,11,12,1,2,3,4,5,6},
        {5,6,7,8,9,10,11,12,1,2,3,4,5,6},
        {5,6,7,8,88,9,10,11,12,1,2,3,4,5}
    };
    static final class Date {
        final int year, month, day;
        Date(int year,int month,int day){this.year=year;this.month=month;this.day=day;}
        boolean waxing(){return day<=15;}
        int halfDay(){return waxing()?day:day-15;}
    }
    private static long horakhun(int y){return Math.floorDiv(292207L*y+373,800)+1;}
    private static final class Year {
        final long h;
        final int tithi;
        final boolean solarLeap;
        int langsak, nyd, next, offsetDays;
        char type;
        boolean offset;
        Year(int y){
            h=horakhun(y);
            long av=Math.floorMod(11*h+650,692), q=Math.floorDiv(11*h+650,692);
            if(av==0)av=692;
            tithi=(int)Math.floorMod(q+h,30)-(av==692?1:0);
            langsak=Math.max(1,tithi);
            int n=langsak<6?langsak+29:langsak;
            nyd=(int)Math.floorMod(Math.floorMod(h,7)-n+36,7);
            solarLeap=800-Math.floorMod(292207L*y+373,800)<=207;
            long nextH=horakhun(y+1);
            int nextT=(int)Math.floorMod(Math.floorDiv(11*nextH+650,692)+nextH,30);
            type=tithi>24||tithi<6?'C':'A';
            if(tithi==25&&nextT==5)type='A';
            if((solarLeap&&av<=126)||(!solarLeap&&av<=137))type=type=='C'?'c':'B';
            next=(nyd+(type=='A'?4:type=='B'?5:6))%7;
        }
    }
    private static Year year(int year){
        // Limit cache growth during full-screen navigation and reference tests.
        if(YEARS.size()>256)YEARS.clear();
        return YEARS.computeIfAbsent(year,ThaiCivilCalendar::calculateYear);
    }
    private static Year calculateYear(int year){
        Year[] y=new Year[5];for(int i=0;i<5;i++)y[i]=new Year(year+i-2);
        if(y[2].tithi==24&&y[3].tithi==6)for(Year v:y){v.type='C';v.next=(v.next+2)%7;}
        for(int i=1;i<=3;i++)if(y[i].type=='c'){
            int j=y[i].nyd==y[i-1].next?1:-1;
            y[i+j].type='B';y[i+j].next=(y[i+j].next+1)%7;
        }
        for(int i=1;i<=3;i++)if(y[i-1].next!=y[i].nyd&&y[i].next!=y[i+1].nyd){
            y[i].offset=true;y[i].langsak++;y[i].nyd=(y[i].nyd+6)%7;y[i].next=(y[i].next+6)%7;
        }
        Year result=y[2];if(result.type=='c')result.type='C';
        result.offsetDays=result.langsak;
        if(result.offsetDays<6+(result.offset?1:0))result.offsetDays+=29;
        return result;
    }
    static boolean supported(LocalDate date){return date.getYear()>=FIRST_YEAR&&date.getYear()<=LAST_YEAR;}
    static Date convert(LocalDate date){
        long hk=date.toEpochDay()+2440588L-1954167L;
        int cs=(int)Math.floorDiv(hk*800-373,292207);
        long days;
        if(Math.floorMod(hk,292207)==95333){cs--;days=365;}else days=hk-year(cs).h;
        Year y=year(cs);
        while(days>365+(y.solarLeap?1:0)){days-=365+(y.solarLeap?1:0);y=year(++cs);}
        int position=(int)days+y.offsetDays, type=y.type=='A'?0:y.type=='B'?1:2;
        int index=OFFSETS[type].length-1;
        while(index>0&&position<=OFFSETS[type][index])index--;
        return new Date(cs,MONTHS[type][index],position-OFFSETS[type][index]);
    }
    static boolean isWanPhra(LocalDate date){
        Date d=convert(date);
        return d.day==8||d.day==15||d.day==23||convert(date.plusDays(1)).day==1;
    }
    private ThaiCivilCalendar(){}
}
