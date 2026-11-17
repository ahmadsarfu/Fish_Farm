package com.fishfarm.management.security;
import com.fishfarm.management.repository.FarmMemberRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

public class FarmRequestFilter extends OncePerRequestFilter {
    private final FarmMemberRepository members;
    public FarmRequestFilter(FarmMemberRepository members) { this.members=members; }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        try {
            if(request.getRequestURI().startsWith("/api/"))response.setHeader("Cache-Control","no-store, private");
            var auth=SecurityContextHolder.getContext().getAuthentication();
            if(auth!=null && auth.getPrincipal() instanceof FarmPrincipal principal) {
                var member=members.findByAccountIdAndFarmIdAndActiveTrue(principal.userId(),principal.farmId());
                if(member.isEmpty() || !member.get().getAccount().isActive() || !member.get().getFarm().isActive() || member.get().getAccount().getSessionVersion()!=principal.sessionVersion()) {
                    SecurityContextHolder.clearContext(); if(request.getSession(false)!=null) request.getSession(false).invalidate();
                    response.setStatus(401); response.setContentType("application/json"); response.getWriter().write("{\"message\":\"Session access has been revoked. Log in again.\"}"); return;
                }
                var fresh=new FarmPrincipal(principal.userId(),principal.username(),principal.farmId(),member.get().getRole(),principal.sessionVersion());
                SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(fresh,null,List.of(new SimpleGrantedAuthority("ROLE_"+fresh.role()))));
                FarmContext.set(fresh.farmId(),fresh.userId(),fresh.role());
            }
            chain.doFilter(request,response);
        } finally { FarmContext.clear(); }
    }
}
