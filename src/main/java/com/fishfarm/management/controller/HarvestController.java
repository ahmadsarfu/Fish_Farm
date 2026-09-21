package com.fishfarm.management.controller;

import com.fishfarm.management.dto.HarvestRequest;
import com.fishfarm.management.dto.HarvestResponse;
import com.fishfarm.management.service.HarvestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/harvests")
@RequiredArgsConstructor
public class HarvestController {

    private final HarvestService harvestService;

    @PostMapping
    public HarvestResponse create(@Valid @RequestBody HarvestRequest request) {

        return harvestService.create(request);
    }

    @GetMapping
    public List<HarvestResponse> getAll() {
        return harvestService.getAll();
    }

    @GetMapping("/{id}")
    public HarvestResponse getById(@PathVariable Long id) {

        return harvestService.getById(id);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id) {

        harvestService.delete(id);

        return Map.of("message", "Harvest deleted successfully");
    }
}