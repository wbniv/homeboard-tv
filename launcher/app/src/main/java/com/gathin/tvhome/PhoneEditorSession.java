package com.gathin.tvhome;

import java.security.SecureRandom;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.LongSupplier;

/** One temporary editor; pending clients have no configuration access. No credentials are persisted. */
final class PhoneEditorSession {
    static final long IDLE=15*60_000L,PENDING=120_000L;
    private final LongSupplier clock;
    private String invitation=random(),token="",pending="",code="";
    private long lastActivity,pendingAt,lastRequest=-10_000;
    private boolean enabled=true;
    PhoneEditorSession(LongSupplier clock){this.clock=clock;lastActivity=clock.getAsLong();}
    static String random(){byte[] bytes=new byte[24];new SecureRandom().nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
    synchronized String invitation(){return invitation;}
    synchronized String request(String invite){
        long now=clock.getAsLong();if(!running()||now-lastRequest<10_000)throw new IllegalArgumentException("Please wait before requesting another connection");
        if(!invite.isEmpty()&&!equal(invitation,invite))throw new IllegalArgumentException("Invitation expired; scan the current QR code");
        lastRequest=now;pending=random();pendingAt=now;code=String.format(java.util.Locale.US,"%06d",new SecureRandom().nextInt(1_000_000));
        return pending;
    }
    synchronized String code(){return code;}
    synchronized boolean pending(){return running()&&!pending.isEmpty()&&clock.getAsLong()-pendingAt<PENDING;}
    synchronized void approve(String request,boolean allow){
        if(!pending()||!equal(pending,request))return;
        if(allow){token=pending;lastActivity=clock.getAsLong();invitation=random();}
        pending="";code="";
    }
    synchronized boolean authorized(String candidate,boolean touch){
        boolean ok=running()&&!token.isEmpty()&&equal(token,candidate);
        if(ok&&touch)lastActivity=clock.getAsLong();return ok;
    }
    synchronized String status(String candidate){if(authorized(candidate,false))return "approved";return pending()&&equal(pending,candidate)?"pending":"expired";}
    synchronized long remaining(){return Math.max(0,IDLE-(clock.getAsLong()-lastActivity));}
    synchronized boolean running(){return enabled&&clock.getAsLong()-lastActivity<IDLE;}
    synchronized void close(){enabled=false;token="";pending="";invitation="";}
    static boolean equal(String a,String b){return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),b.getBytes(StandardCharsets.UTF_8));}
}
