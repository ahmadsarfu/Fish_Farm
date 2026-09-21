package com.fishfarm.management.controller;

import com.fishfarm.management.dto.WeighingSessionSummaryResponse;
import com.fishfarm.management.dto.WeightEntryRequest;
import com.fishfarm.management.dto.WeightEntryResponse;
import com.fishfarm.management.service.WeightEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/weight-entries")
@RequiredArgsConstructor
public class WeightEntryController {

    private final WeightEntryService service;

    @PostMapping
    public WeightEntryResponse create(@Valid @RequestBody WeightEntryRequest request) {

        return service.create(request);
    }

    @GetMapping("/{id}")
    public WeightEntryResponse getById(@PathVariable Long id) {

        return service.getById(id);
    }

    @GetMapping("/session/{sessionId}")
    public List<WeightEntryResponse> getBySession(@PathVariable Long sessionId) {

        return service.getBySession(sessionId);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {

        service.delete(id);
    }

    @GetMapping("/{id}/summary")
    public WeighingSessionSummaryResponse getSummary(@PathVariable Long id) {

        return service.getSummary(id);
    }
}