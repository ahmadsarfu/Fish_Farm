package com.fishfarm.management.config;

import com.fishfarm.management.repository.FarmMemberRepository;
import com.fishfarm.management.security.FarmRequestFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.csrf.*;
import org.springframework.web.cors.*;
import java.util.*;

@Configuration
public class SecurityConfig {
    @Bean org.springframework.security.core.userdetails.UserDetailsService noDefaultUsers() {
        return username -> { throw new org.springframework.security.core.userdetails.UsernameNotFoundException("Use farm login"); };
    }
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean CookieCsrfTokenRepository csrfTokens(@Value("${app.cookie-secure:false}") boolean secure) {
        var repository=CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> cookie.path("/").sameSite("Lax").secure(secure));
        return repository;
    }
    @Bean CorsConfigurationSource corsSource(@Value("${app.allowed-origins:http://localhost:5173,http://localhost:4174}") String origins) {
        var config=new CorsConfiguration(); config.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).toList());
        config.setAllowCredentials(true); config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type","X-XSRF-TOKEN")); config.setMaxAge(3600L);
        var source=new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/**",config); return source;
    }
    @Bean SecurityFilterChain api(HttpSecurity http, CookieCsrfTokenRepository tokens, FarmMemberRepository members, CorsConfigurationSource corsSource, com.fishfarm.management.repository.AppAccountRepository accounts, com.fishfarm.management.platform.SubscriptionAccess subscriptions) throws Exception {
        http.cors(cors -> cors.configurationSource(corsSource)).csrf(csrf -> csrf.csrfTokenRepository(tokens).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .formLogin(form -> form.disable()).httpBasic(basic -> basic.disable())
            .requestCache(cache -> cache.disable())
            .addFilterBefore(new FarmRequestFilter(members,accounts,subscriptions), AuthorizationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS,"/**").permitAll()
                .requestMatchers("/api/auth/trial-requests","/api/auth/csrf","/api/auth/login","/api/auth/register","/api/auth/setup-status","/api/auth/invitation","/api/auth/activate").permitAll()
                .requestMatchers("/api/platform/**").hasRole("SUPER_ADMIN")
                .requestMatchers(HttpMethod.POST,"/api/auth/farms").hasRole("OWNER")
                .requestMatchers("/api/auth/**").authenticated()
                .requestMatchers("/api/team/**").hasRole("OWNER")
                .requestMatchers(HttpMethod.GET,"/api/feeding-dashboard","/api/ponds","/api/feed-types").hasAnyRole("OWNER","MANAGER","LABOUR")
                .requestMatchers(HttpMethod.PUT,"/api/feeding-dashboard/*/slots/*").hasAnyRole("OWNER","MANAGER","LABOUR")
                .requestMatchers(HttpMethod.POST,"/api/daily-feeds","/api/daily-feeds/*/review").hasRole("OWNER")
                .requestMatchers(HttpMethod.PUT,"/api/daily-feeds/*").denyAll()
                .requestMatchers(HttpMethod.DELETE,"/api/daily-feeds/*").denyAll()
                .requestMatchers("/api/**").hasAnyRole("OWNER","MANAGER")
                .anyRequest().denyAll())
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((req,res,error)->{res.setStatus(401);res.setContentType("application/json");res.getWriter().write("{\"message\":\"Please log in\"}");})
                .accessDeniedHandler((req,res,error)->{res.setStatus(403);res.setContentType("application/json");res.getWriter().write("{\"message\":\"You do not have permission, or the security token expired. Refresh and try again.\"}");}))
            .logout(logout -> logout.logoutUrl("/api/auth/logout").invalidateHttpSession(true).clearAuthentication(true).deleteCookies("JSESSIONID").logoutSuccessHandler((req,res,auth)->res.setStatus(204)));
        return http.build();
    }
}
