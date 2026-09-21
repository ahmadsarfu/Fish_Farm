package com.fishfarm.management.controller;

import com.fishfarm.management.dto.WeighingSessionRequest;
import com.fishfarm.management.dto.WeighingSessionResponse;
import com.fishfarm.management.dto.WeighingSessionSummaryResponse;
import com.fishfarm.management.service.WeighingSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/weighing-sessions")
@RequiredArgsConstructor
public class WeighingSessionController {

    private final WeighingSessionService service;

    @PostMapping
    public WeighingSessionResponse create(@Valid @RequestBody WeighingSessionRequest request) {

        return service.create(request);
    }

    @GetMapping
    public List<WeighingSessionResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public WeighingSessionResponse getById(@PathVariable Long id) {

        return service.getById(id);
    }

    @PutMapping("/{id}/close")
    public WeighingSessionResponse closeSession(@PathVariable Long id, @RequestBody Map<String, BigDecimal> body) {
        BigDecimal deductionPct = body.getOrDefault("deductionPct", new BigDecimal("3")); // default 3%
        return service.closeSession(id, body.get("ratePerKg"), deductionPct);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFishType(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(Map.of("message", "Deleted successfully"));
    }

}