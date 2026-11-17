package com.fishfarm.management.controller;
import com.fishfarm.management.dto.AuthRequests.*;
import com.fishfarm.management.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/team") @RequiredArgsConstructor
public class TeamController {
    private final AuthService service;
    @GetMapping public List<Map<String,Object>> team() { return service.team(); }
    @PostMapping public void create(@Valid @RequestBody NewMember request) { service.createMember(request); }
    @PutMapping("/{id}") public void update(@PathVariable Long id,@Valid @RequestBody UpdateMember request) { service.updateMember(id,request); }
}
