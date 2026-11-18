package com.fishfarm.management.platform;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Locale;

@Component
public class PlatformMfa {
    private final String secret;
    private final Clock clock;
    public PlatformMfa(@Value("${app.platform-totp-secret:}") String secret,Clock clock) {this.secret=secret;this.clock=clock;}
    public boolean configured() {return secret.matches("[A-Za-z2-7]{32,104}");}
    public boolean verify(String code) {
        if(!configured() || code==null || !code.matches("[0-9]{6}")) return false;
        try {
            byte[] key=decode(secret.toUpperCase(Locale.ROOT));
            for(int offset=-1;offset<=1;offset++) {
                Mac mac=Mac.getInstance("HmacSHA1");mac.init(new SecretKeySpec(key,"HmacSHA1"));
                byte[] hash=mac.doFinal(ByteBuffer.allocate(8).putLong(clock.instant().getEpochSecond()/30+offset).array());
                int index=hash[hash.length-1]&15;
                int value=ByteBuffer.wrap(hash,index,4).getInt()&0x7fffffff;
                String expected=String.format(Locale.ROOT,"%06d",value%1000000);
                if(MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),code.getBytes(StandardCharsets.US_ASCII))) return true;
            }
            return false;
        } catch(java.security.GeneralSecurityException failure) {throw new IllegalStateException("Admin authenticator is unavailable",failure);}
    }
    private byte[] decode(String text) {
        java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();int buffer=0,bits=0;
        for(char c:text.toCharArray()) {buffer=(buffer<<5)|"ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".indexOf(c);bits+=5;if(bits>=8){bits-=8;bytes.write((buffer>>bits)&255);}}
        return bytes.toByteArray();
    }
}
