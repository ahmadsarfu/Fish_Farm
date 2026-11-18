package com.fishfarm.management.controller;
import com.fishfarm.management.dto.AuthRequests.*;
import com.fishfarm.management.security.*;
import com.fishfarm.management.service.AuthService;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor
public class AuthController {
    private final AuthService service;
    private final CookieCsrfTokenRepository tokens;
    private final LoginThrottle throttle;
    private final com.fishfarm.management.service.PlatformCustomerService provisioning;
    @org.springframework.beans.factory.annotation.Value("${app.public-registration-enabled:true}") private boolean publicRegistration;
    @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken token) { return Map.of("token",token.getToken()); }
    @GetMapping("/setup-status") public Map<String,Boolean> setup() { return Map.of("legacyClaimAvailable",service.legacyClaimAvailable(),"legacyDataWaiting",service.legacyDataWaiting(),"publicRegistrationEnabled",publicRegistration); }
    @PostMapping("/login") public Map<String,Object> login(@Valid @RequestBody Login body,HttpServletRequest request,HttpServletResponse response) {
        String key="login:"+request.getRemoteAddr()+":"+body.username().toLowerCase(Locale.ROOT);throttle.check(key);
        try { FarmPrincipal p=service.login(body);throttle.success(key);return signIn(p,request,response); }
        catch(org.springframework.web.server.ResponseStatusException e) {throttle.failure(key);throw e;}
    }
    @PostMapping("/register") public Map<String,Object> register(@Valid @RequestBody Register body,HttpServletRequest request,HttpServletResponse response) {
        String key="register:"+request.getRemoteAddr();throttle.check(key);throttle.failure(key);
        return signIn(service.register(body),request,response);
    }
    @PostMapping("/invitation") public Map<String,Object> invitation(@Valid @RequestBody com.fishfarm.management.dto.PlatformRequests.InvitationToken body,HttpServletRequest request) {
        String key="invitation:"+request.getRemoteAddr();throttle.check(key);throttle.failure(key);return provisioning.invitation(body.token());
    }
    @PostMapping("/activate") public Map<String,Object> activate(@Valid @RequestBody com.fishfarm.management.dto.PlatformRequests.Activate body,HttpServletRequest request,HttpServletResponse response) {
        String key="activate:"+request.getRemoteAddr();throttle.check(key);throttle.failure(key);return signIn(provisioning.activate(body),request,response);
    }
    @GetMapping("/me") public Map<String,Object> me(org.springframework.security.core.Authentication auth) { return service.me((FarmPrincipal)auth.getPrincipal()); }
    @PostMapping("/select-farm") public Map<String,Object> select(@Valid @RequestBody SelectFarm body,org.springframework.security.core.Authentication auth,HttpServletRequest request,HttpServletResponse response) {
        return signIn(service.selectFarm(((FarmPrincipal)auth.getPrincipal()).userId(),body.farmId()),request,response);
    }
    @PostMapping("/farms") public List<Map<String,Object>> farm(@Valid @RequestBody NewFarm body,org.springframework.security.core.Authentication auth) {
        long id=((FarmPrincipal)auth.getPrincipal()).userId();service.createFarm(id,body.name());return service.farms(id);
    }
    @PostMapping("/password") public void password(@Valid @RequestBody ChangePassword body,org.springframework.security.core.Authentication auth,HttpServletRequest request,HttpServletResponse response) {
        service.changePassword(((FarmPrincipal)auth.getPrincipal()).userId(),body);
        if(request.getSession(false)!=null)request.getSession(false).invalidate();
        SecurityContextHolder.clearContext();tokens.saveToken(null,request,response);
    }
    private Map<String,Object> signIn(FarmPrincipal principal,HttpServletRequest request,HttpServletResponse response) {
        request.getSession(true);request.changeSessionId();
        var context=SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal,null,List.of(new SimpleGrantedAuthority("ROLE_"+principal.role()))));
        SecurityContextHolder.setContext(context);new HttpSessionSecurityContextRepository().saveContext(context,request,response);
        tokens.saveToken(null,request,response);
        return service.me(principal);
    }
}
