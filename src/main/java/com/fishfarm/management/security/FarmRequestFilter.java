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
    private final com.fishfarm.management.repository.AppAccountRepository accounts;
    private final com.fishfarm.management.platform.SubscriptionAccess subscriptions;
    public FarmRequestFilter(FarmMemberRepository members,com.fishfarm.management.repository.AppAccountRepository accounts,com.fishfarm.management.platform.SubscriptionAccess subscriptions) { this.members=members;this.accounts=accounts;this.subscriptions=subscriptions; }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        try {
            if(request.getRequestURI().startsWith("/api/"))response.setHeader("Cache-Control","no-store, private");
            var auth=SecurityContextHolder.getContext().getAuthentication();
            if(auth!=null && auth.getPrincipal() instanceof FarmPrincipal principal) {
                if("SUPER_ADMIN".equals(principal.role())) {
                    var account=accounts.findById(principal.userId());
                    if(account.isEmpty() || !account.get().isPlatformAdmin() || !account.get().isActive() || account.get().getSessionVersion()!=principal.sessionVersion()) {
                        SecurityContextHolder.clearContext();if(request.getSession(false)!=null)request.getSession(false).invalidate();
                        response.setStatus(401);response.setContentType("application/json");response.getWriter().write("{\"message\":\"Admin session expired. Log in again.\"}");return;
                    }
                    chain.doFilter(request,response);return;
                }
                var member=members.findByAccountIdAndFarmIdAndActiveTrue(principal.userId(),principal.farmId());
                if(member.isEmpty() || !member.get().getAccount().isActive() || !member.get().getFarm().isActive() || member.get().getAccount().getSessionVersion()!=principal.sessionVersion()) {
                    SecurityContextHolder.clearContext(); if(request.getSession(false)!=null) request.getSession(false).invalidate();
                    response.setStatus(401); response.setContentType("application/json"); response.getWriter().write("{\"message\":\"Session access has been revoked. Log in again.\"}"); return;
                }
                if(request.getRequestURI().startsWith("/api/") && !request.getRequestURI().startsWith("/api/auth/") && !subscriptions.allowed(member.get().getFarm())) {
                    response.setStatus(403);response.setContentType("application/json");response.getWriter().write("{\"message\":\"Your farm plan is paused or expired. Contact your provider to renew access. Your data is preserved.\"}");return;
                }
                var fresh=new FarmPrincipal(principal.userId(),principal.username(),principal.farmId(),member.get().getRole(),principal.sessionVersion());
                SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(fresh,null,List.of(new SimpleGrantedAuthority("ROLE_"+fresh.role()))));
                FarmContext.set(fresh.farmId(),fresh.userId(),fresh.role());
            }
            chain.doFilter(request,response);
        } finally { FarmContext.clear(); }
    }
}
