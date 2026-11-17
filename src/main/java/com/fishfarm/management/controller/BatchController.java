package com.fishfarm.management.controller;
import com.fishfarm.management.dto.BatchRequests.*;
import com.fishfarm.management.service.BatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class BatchController {
    private final BatchService service;
    private final com.fishfarm.management.service.AuditService audit;
    @GetMapping("/batches") public List<Map<String,Object>> list(){return service.list();}
    @GetMapping("/batches/{id}") public Map<String,Object> details(@PathVariable Long id){return service.details(id);}
    @PostMapping("/batches") public Map<String,Object> create(@Valid @RequestBody Create request){return service.create(request);}
    @PostMapping("/batch-stages/{id}/mortality") public Map<String,Object> mortality(@PathVariable Long id,@Valid @RequestBody Mortality request){return service.mortality(id,request);}
    @PostMapping("/batch-stages/{id}/transfers") public Map<String,Object> transfer(@PathVariable Long id,@Valid @RequestBody Transfer request){return service.transfer(id,request);}
    @PostMapping("/batch-stages/{id}/expenses") public Map<String,Object> expense(@PathVariable Long id,@Valid @RequestBody Expense request){return service.expense(id,request);}
    @PostMapping("/batch-stages/{id}/harvests") public Map<String,Object> harvest(@PathVariable Long id,@Valid @RequestBody Harvest request){return service.harvest(id,request);}
    @PostMapping("/batch-stages/{id}/settlements") public Map<String,Object> settlement(@PathVariable Long id,@Valid @RequestBody com.fishfarm.management.dto.BatchSettlementRequest request){return service.settlement(id,request);}
    @GetMapping("/audit") public Object audit(){return audit.list();}
}
