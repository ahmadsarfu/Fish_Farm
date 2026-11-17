package com.fishfarm.management.security;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.util.concurrent.ConcurrentHashMap;
@Component
public class LoginThrottle {
    private record Attempts(int count,long until) {}
    private final ConcurrentHashMap<String,Attempts> attempts=new ConcurrentHashMap<>();
    public void check(String key) {
        long now=System.currentTimeMillis();
        if(attempts.size()>10000) attempts.entrySet().removeIf(e->e.getValue().until()<now);
        if(attempts.size()>=20000 && !attempts.containsKey(key))throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Too many attempts. Try again later");
        Attempts value=attempts.get(key);
        if(value!=null && value.until()>now && value.count()>=10) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Too many attempts. Try again in 15 minutes");
    }
    public void failure(String key) { long now=System.currentTimeMillis();attempts.compute(key,(k,v)->v==null||v.until()<now?new Attempts(1,now+900000):new Attempts(v.count()+1,v.until())); }
    public void success(String key) { attempts.remove(key); }
}
