package com.gathin.tvhome;

import fi.iki.elonen.NanoHTTPD;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Bounded HTTP transport: exact same origin, fixed assets, JSON-only API, no file server. */
final class PhoneEditorServer extends NanoHTTPD {
    interface Host {JSONObject command(String path,JSONObject body,String credential)throws Exception;void pairing(String request,String code);default String name(){return "TV";}default String id(){return "";}}
    private final Host host;
    final PhoneEditorSession session;
    private final Map<String,byte[]> assets;
    private final Set<ClientHandler> clients=Collections.synchronizedSet(new HashSet<>());
    private final ThreadPoolExecutor workers=new ThreadPoolExecutor(4,4,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(8));
    private String authority;
    PhoneEditorServer(String address,int port,Host host,PhoneEditorSession session,Map<String,byte[]> assets){
        super(address,port);this.host=host;this.session=session;this.assets=assets;
        setAsyncRunner(new AsyncRunner(){
            public void exec(ClientHandler client){clients.add(client);try{workers.execute(client);}catch(RejectedExecutionException full){clients.remove(client);client.close();}}
            public void closed(ClientHandler client){clients.remove(client);}
            public void closeAll(){synchronized(clients){for(ClientHandler client:clients)client.close();clients.clear();}workers.shutdownNow();}
        });
    }
    void open()throws IOException{start(5000,false);authority=getHostname()+":"+getListeningPort();}
    String url(){return "http://"+authority;}
    @Override public Response serve(IHTTPSession request){
        try{
            if(!session.running())return json(403,"Session expired; reconnect on the TV");
            String origin=request.getHeaders().get("origin"),hostHeader=request.getHeaders().get("host");
            if(!authority.equals(hostHeader)||origin!=null&&!url().equals(origin))return json(403,"Wrong origin");
            String path=request.getUri();
            if(request.getMethod()==Method.GET&&path.equals("/shortcut.webmanifest")){
                String label=shortcutName(request);
                JSONObject manifest=new JSONObject().put("id","/?tv="+host.id()).put("name",label).put("short_name",label)
                    .put("start_url","/?tv="+host.id()+"&name="+java.net.URLEncoder.encode(label,"UTF-8")).put("display","browser")
                    .put("theme_color","#101a23").put("background_color","#101a23")
                    .put("icons",new JSONArray().put(new JSONObject().put("src","/tv-icon.png").put("sizes","192x192").put("type","image/png")));
                return secure(newFixedLengthResponse(Response.Status.OK,"application/manifest+json; charset=utf-8",manifest.toString()));
            }
            if(request.getMethod()==Method.GET&&assets.containsKey(path)){
                byte[] body=assets.get(path);
                if(path.equals("/")){String title=escape(shortcutName(request));body=new String(body,StandardCharsets.UTF_8).replace("Homeboard · Your dashboard",title)
                    .replace("name=\"apple-mobile-web-app-title\" content=\"Homeboard\"","name=\"apple-mobile-web-app-title\" content=\""+title+"\"").getBytes(StandardCharsets.UTF_8);}
                String mime=path.endsWith(".png")?"image/png":path.endsWith(".js")?"text/javascript; charset=utf-8":path.endsWith(".css")?"text/css; charset=utf-8":"text/html; charset=utf-8";
                return secure(newFixedLengthResponse(Response.Status.OK,mime,new ByteArrayInputStream(body),body.length));
            }
            if(request.getMethod()!=Method.POST||!url().equals(origin)||!request.getHeaders().getOrDefault("content-type","").split(";")[0].trim().equals("application/json"))return json(403,"JSON requests from this editor required");
            if(request.getHeaders().containsKey("transfer-encoding"))return json(400,"Chunked requests unsupported");
            int length=Integer.parseInt(request.getHeaders().getOrDefault("content-length","-1"));
            if(length<2||length>524288)return json(413,"Request is too large");
            byte[] bytes=new byte[length];int offset=0;
            while(offset<length){int read=request.getInputStream().read(bytes,offset,length-offset);if(read<0)return json(400,"Incomplete request");offset+=read;}
            JSONObject body=new JSONObject(new String(bytes,StandardCharsets.UTF_8));
            if(path.equals("/api/pair")){
                if(!body.optString("tvId","").isEmpty()&&!host.id().equals(body.getString("tvId")))return error(422,"tvId","This shortcut belongs to another TV. Scan this TV's QR code.");
                String id=session.request(body.optString("invitation",""));host.pairing(id,session.code());
                return response(new JSONObject().put("token",id).put("code",session.code()).put("status","pending").put("tv",host.name()).put("tvId",host.id()));
            }
            String token=request.getHeaders().getOrDefault("x-homeboard-session","");
            if(path.equals("/api/status"))return response(new JSONObject().put("status",session.status(token)).put("remaining",session.remaining()));
            if(!session.authorized(token,!path.equals("/api/operation")))return json(403,"Approval expired or revoked; reconnect on TV");
            if(!Set.of("/api/timers","/api/timer-state","/api/touch","/api/icon","/api/state","/api/apply","/api/undo","/api/preview","/api/cancel-preview","/api/lookup","/api/operation","/api/stop").contains(path))return json(404,"Unknown action");
            return response(host.command(path,body,token));
        }catch(PhoneConfiguration.Invalid invalid){return error(422,invalid.field,invalid.getMessage());}
        catch(IllegalArgumentException|JSONException bad){return error(422,"request",bad.getMessage());}
        catch(Exception failure){return json(503,"TV unavailable or busy; keep your draft and retry");}
    }
    private String shortcutName(IHTTPSession request){String name=request.getParms().get("name");return name==null||name.isBlank()||name.length()>60?host.name():name;}
    private static String escape(String value){return value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
    private static Response response(JSONObject body){return secure(newFixedLengthResponse(Response.Status.OK,"application/json; charset=utf-8",body.toString()));}
    private static Response json(int code,String message){return error(code,"request",message);}
    private static Response error(int code,String field,String message){
        JSONObject body=new JSONObject();try{body.put("error",message).put("field",field);}catch(JSONException ignored){}
        Response.IStatus status=new Response.IStatus(){public int getRequestStatus(){return code;}public String getDescription(){return code+" Error";}};
        return secure(newFixedLengthResponse(status,"application/json; charset=utf-8",body.toString()));
    }
    private static Response secure(Response r){
        r.addHeader("Cache-Control","no-store");r.addHeader("Referrer-Policy","no-referrer");r.addHeader("X-Content-Type-Options","nosniff");
        r.addHeader("Content-Security-Policy","default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'");
        r.addHeader("X-Frame-Options","DENY");r.addHeader("Connection","close");r.setKeepAlive(false);return r;
    }
    @Override public void stop(){session.close();super.stop();}
}
