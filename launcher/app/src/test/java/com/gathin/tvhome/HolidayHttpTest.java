package com.gathin.tvhome;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

public class HolidayHttpTest {
    @Rule public TemporaryFolder files=new TemporaryFolder();
    private static final String URL="https://www.opm.gov/fixture";
    private static class Response extends HttpURLConnection {
        int status;String body,redirect;Response(int status,String body)throws Exception{super(new URL(URL));this.status=status;this.body=body;}
        public int getResponseCode(){return status;}public String getHeaderField(String name){return name.equals("ETag")?"fixture-etag":name.equals("Location")?redirect:null;}
        public InputStream getInputStream(){return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));}
        public void connect(){}public void disconnect(){}public boolean usingProxy(){return false;}
    }
    @Test public void conditionalResponsesReuseOnlyPreviouslyAcceptedCaptures()throws Exception{
        AtomicReference<Response> response=new AtomicReference<>(new Response(200,"valid fixture"));
        HolidayHttp.Transport transport=HolidayHttp.network(files.newFolder(),address->response.get());
        HolidayHttp.Capture first=transport.get(URL);assertFalse(first.unchanged);transport.accepted(first);
        Response unchanged=new Response(304,"");response.set(unchanged);HolidayHttp.Capture next=transport.get(URL);
        assertTrue(next.unchanged);assertEquals("valid fixture",next.body);assertEquals("fixture-etag",unchanged.getRequestProperty("If-None-Match"));
    }
    @Test public void rejectedDocumentNeverPoisonsConditionalCache()throws Exception{
        AtomicReference<Response> response=new AtomicReference<>(new Response(200,"unparsed fixture"));
        HolidayHttp.Transport transport=HolidayHttp.network(files.newFolder(),address->response.get());transport.get(URL);
        response.set(new Response(304,""));try{transport.get(URL);fail();}catch(IOException expected){}
    }
    @Test public void errorsAndUntrustedRedirectsAreRefused()throws Exception{
        for(int status:new int[]{403,500,302}){
            Response response=new Response(status,"");response.redirect="http://untrusted.example/";
            HolidayHttp.Transport transport=HolidayHttp.network(files.newFolder(),address->response);
            try{transport.get(URL);fail();}catch(IOException expected){}
        }
    }
}
