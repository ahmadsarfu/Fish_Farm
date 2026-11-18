package com.fishfarm.management.controller;
import com.fishfarm.management.dto.PlatformRequests.*;
import com.fishfarm.management.security.FarmPrincipal;
import com.fishfarm.management.service.PlatformCustomerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import java.util.Map;
@RestController @RequestMapping("/api/platform") @RequiredArgsConstructor @Validated
public class PlatformCustomerController {
    private final PlatformCustomerService service;
    @GetMapping("/customers") public Map<String,Object> customers(@RequestParam(defaultValue="") @Size(max=120) String search,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.customers(search,page,size);}
    @PostMapping("/customers") public Map<String,Object> invite(@Valid @RequestBody Invite body,Authentication auth){return service.invite(body,((FarmPrincipal)auth.getPrincipal()).userId());}
    @PostMapping("/customers/{id}/activation-link") public Map<String,Object> reissue(@PathVariable Long id,Authentication auth){return service.reissue(id,((FarmPrincipal)auth.getPrincipal()).userId());}
    @PostMapping("/customers/{id}/password-reset") public Map<String,Object> reset(@PathVariable Long id,Authentication auth){return service.passwordReset(id,((FarmPrincipal)auth.getPrincipal()).userId());}
    @PutMapping("/customers/{id}/subscription") public Map<String,Object> update(@PathVariable Long id,@Valid @RequestBody Subscription body,Authentication auth){return service.update(id,body,((FarmPrincipal)auth.getPrincipal()).userId());}
    @GetMapping("/activity") public Object activity(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.activity(page,size);}
}
