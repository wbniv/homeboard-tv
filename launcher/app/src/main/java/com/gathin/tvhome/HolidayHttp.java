package com.gathin.tvhome;

import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;

/** Bounded HTTPS captures with conditional requests. Only validated bodies become cache entries. */
final class HolidayHttp {
    interface Transport { Capture get(String url)throws Exception; void accepted(Capture capture)throws Exception; }
    interface Connections { HttpURLConnection open(String url)throws Exception; }
    static final class Capture {
        final String url,body,etag,modified;final boolean unchanged;
        Capture(String url,String body,String etag,String modified,boolean unchanged){this.url=url;this.body=body;this.etag=etag;this.modified=modified;this.unchanged=unchanged;}
    }
    static String hash(String body)throws Exception{
        byte[] digest=MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8));StringBuilder out=new StringBuilder();
        for(byte b:digest)out.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return out.toString();
    }
    static boolean official(String address)throws Exception{
        URI uri=URI.create(address);String host=uri.getHost();
        if(!"https".equals(uri.getScheme())||host==null||uri.getUserInfo()!=null)return false;
        return host.equals("www.opm.gov")||host.equals("www.boe.es")||host.endsWith(".gencat.cat")||host.endsWith(".transparenciacatalunya.cat")
                ||host.endsWith(".go.th")||host.endsWith(".gov.cn");
    }
    static Transport network(File folder){return network(folder,url->(HttpURLConnection)new URL(url).openConnection());}
    static Transport network(File folder,Connections connections){return new Transport(){
        private Path path(String url)throws Exception{Files.createDirectories(folder.toPath());return folder.toPath().resolve(hash(url)+".json");}
        public Capture get(String url)throws Exception{
            if(!official(url))throw new IOException("Unsupported publisher address");
            JSONObject old=null;try{old=new JSONObject(new String(Files.readAllBytes(path(url)),StandardCharsets.UTF_8));
                if(!old.getString("url").equals(url)||!hash(old.getString("body")).equals(old.getString("hash")))old=null;
            }catch(Exception missing){old=null;}
            String current=url;
            for(int redirects=0;redirects<4;redirects++){
                if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Holiday refresh stopped");
                HttpURLConnection connection=connections.open(current);connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(10_000);connection.setReadTimeout(12_000);connection.setRequestProperty("User-Agent","HomeboardTV/1 Calendar (+https://github.com/wbniv/homeboard-tv)");
                connection.setRequestProperty("Accept","application/json,application/xml,text/calendar,text/html;q=0.8");
                if(old!=null&&current.equals(url)){
                    if(!old.optString("etag").isEmpty())connection.setRequestProperty("If-None-Match",old.getString("etag"));
                    if(!old.optString("modified").isEmpty())connection.setRequestProperty("If-Modified-Since",old.getString("modified"));
                }
                try{
                    int status=connection.getResponseCode();
                    if(status==304){if(old==null)throw new IOException("Publisher returned 304 without a local capture");return new Capture(url,old.getString("body"),old.optString("etag"),old.optString("modified"),true);}
                    if(status>=300&&status<400){String location=connection.getHeaderField("Location");if(location==null)throw new IOException("Missing publisher redirect");
                        current=URI.create(current).resolve(location).toString();if(!official(current))throw new IOException("Unsupported publisher redirect");continue;}
                    if(status!=200)throw new IOException("Publisher HTTP "+status);
                    if(connection.getContentLengthLong()>2_000_000)throw new IOException("Publisher document is too large");
                    ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream input=connection.getInputStream()){
                        byte[] buffer=new byte[8192];int size;while((size=input.read(buffer))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();bytes.write(buffer,0,size);if(bytes.size()>2_000_000)throw new IOException("Publisher document is too large");}
                    }
                    String charset="UTF-8",type=connection.getContentType();if(type!=null){java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?i)charset=[\"']?([\\w-]+)").matcher(type);if(m.find())charset=m.group(1);}
                    return new Capture(url,bytes.toString(charset),value(connection,"ETag"),value(connection,"Last-Modified"),false);
                }finally{connection.disconnect();}
            }
            throw new IOException("Too many publisher redirects");
        }
        public void accepted(Capture capture)throws Exception{
            JSONObject value=new JSONObject().put("url",capture.url).put("body",capture.body).put("hash",hash(capture.body)).put("etag",capture.etag).put("modified",capture.modified);
            Path target=path(capture.url),tmp=target.resolveSibling(target.getFileName()+".tmp");
            try(FileOutputStream output=new FileOutputStream(tmp.toFile())){output.write(value.toString().getBytes(StandardCharsets.UTF_8));output.getFD().sync();}
            Files.move(tmp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        }
    };}
    private static String value(HttpURLConnection c,String key){String value=c.getHeaderField(key);return value==null?"":value;}
    private HolidayHttp(){}
}
