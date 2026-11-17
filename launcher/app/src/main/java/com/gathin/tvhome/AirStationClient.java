package com.gathin.tvhome;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/** All widget instances share one bounded public catalog request and recent snapshot. */
final class AirStationClient {
    private static CompletableFuture<String> pending;
    private static String cached;
    private static long at;
    static synchronized CompletableFuture<String> fetch(){
        if(cached!=null&&System.currentTimeMillis()-at<300_000)return CompletableFuture.completedFuture(cached);
        if(pending!=null)return pending;
        CompletableFuture<String> task=CompletableFuture.supplyAsync(()->{
            HttpURLConnection c=null;
            try{
                c=(HttpURLConnection)new URL(AirQualityCard.ENDPOINT).openConnection();c.setConnectTimeout(10_000);c.setReadTimeout(15_000);c.setInstanceFollowRedirects(false);c.setRequestMethod("POST");c.setDoOutput(true);c.setFixedLengthStreamingMode(0);
                c.setRequestProperty("User-Agent","HomeboardTV (+https://github.com/wbniv/homeboard-tv)");
                try(java.io.OutputStream out=c.getOutputStream()){out.flush();}
                if(c.getResponseCode()!=200)throw new java.io.IOException("Station feed unavailable");
                try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                    byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(out.size()+n>2_000_000)throw new java.io.IOException("Feed too large");out.write(buf,0,n);}return out.toString(StandardCharsets.UTF_8.name());
                }
            }catch(Exception e){throw new java.util.concurrent.CompletionException(e);}finally{if(c!=null)c.disconnect();}
        });
        pending=task;task.whenComplete((body,error)->{synchronized(AirStationClient.class){pending=null;if(error==null){cached=body;at=System.currentTimeMillis();}}});return task;
    }
}
