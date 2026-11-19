package com.fishfarm.management.controller;
import com.fishfarm.management.dto.TrialRequests.*;
import com.fishfarm.management.security.FarmPrincipal;
import com.fishfarm.management.service.TrialRequestService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequiredArgsConstructor
public class TrialRequestController {
    private final TrialRequestService service;
    @PostMapping("/api/auth/trial-requests") public Map<String,String> submit(@Valid @RequestBody Submit body,HttpServletRequest request) {
        service.submit(body,request.getRemoteAddr());
        return Map.of("message","Request received. We will contact you on the number provided and share an activation link after approval. If you already have an account, log in or contact your provider.");
    }
    @GetMapping("/api/platform/trial-requests") public Object list(@RequestParam(defaultValue="PENDING") String status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.list(status,page,size);}
    @PostMapping("/api/platform/trial-requests/{id}/approve") public Object approve(@PathVariable Long id,@Valid @RequestBody Approve body,Authentication auth){return service.approve(id,body,((FarmPrincipal)auth.getPrincipal()).userId());}
    @PostMapping("/api/platform/trial-requests/{id}/reject") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void reject(@PathVariable Long id,Authentication auth){service.reject(id,((FarmPrincipal)auth.getPrincipal()).userId());}
}
