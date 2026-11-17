package com.fishfarm.management.controller;

import com.fishfarm.management.dto.FishStockEntryRequest;
import com.fishfarm.management.dto.FishStockEntryResponse;
import com.fishfarm.management.service.FishStockEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fish-stock")
@RequiredArgsConstructor
public class FishStockEntryController {

    private final FishStockEntryService service;

    @PostMapping
    public FishStockEntryResponse create(
            @Valid @RequestBody FishStockEntryRequest request
    ) {
        return service.create(request);
    }

    @GetMapping
    public List<FishStockEntryResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public FishStockEntryResponse getById(
            @PathVariable Long id
    ) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public FishStockEntryResponse update(
            @PathVariable Long id,
            @Valid @RequestBody FishStockEntryRequest request
    ) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id
    ) {

        service.delete(id);

        return ResponseEntity.ok(
                Map.of("message", "Stock entry deleted successfully")
        );
    }
}