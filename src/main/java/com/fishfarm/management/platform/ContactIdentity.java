package com.fishfarm.management.platform;

import com.fishfarm.management.repository.AppAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.util.Locale;

@Component @RequiredArgsConstructor
public class ContactIdentity {
    private final AppAccountRepository accounts;
    public String email(String value) {
        if(value==null || value.isBlank()) return null;
        return value.trim().toLowerCase(Locale.ROOT);
    }
    public String mobile(String value) {
        if(value==null || value.isBlank()) return null;
        String number=value.trim().replaceAll("[\\s()\\-]", "");
        if(number.matches("[6-9][0-9]{9}")) number="+91"+number;
        if(!number.matches("\\+[1-9][0-9]{7,14}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Enter a valid mobile with country code, for example +919876543210");
        return number;
    }
    public void requireAvailable(String email,String mobile) {
        if(email!=null && (accounts.existsByEmail(email)||accounts.existsByUsername(email))) conflict();
        if(mobile!=null && (accounts.existsByMobile(mobile)||accounts.existsByUsername(mobile)||accounts.existsByUsername(mobile.substring(1))||
                (mobile.startsWith("+91") && accounts.existsByUsername(mobile.substring(3))))) conflict();
    }
    public void requireUsernameAvailable(String name) {
        if(accounts.existsByUsername(name)||accounts.existsByEmail(name)||accounts.existsByMobile(name)) conflict();
        try {String phone=mobile(name); if(phone!=null && accounts.existsByMobile(phone)) conflict();}
        catch(ResponseStatusException failure) {if(failure.getStatusCode()==HttpStatus.CONFLICT) throw failure;}
    }
    private void conflict() {throw new ResponseStatusException(HttpStatus.CONFLICT,"This mobile, email or username is already in use. Use the existing account or a different contact.");}
}
