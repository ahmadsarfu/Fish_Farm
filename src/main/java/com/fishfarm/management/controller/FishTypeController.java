package com.fishfarm.management.controller;

import com.fishfarm.management.dto.FishTypeRequest;
import com.fishfarm.management.dto.FishTypeResponse;
import com.fishfarm.management.service.FishTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fish-types")
@RequiredArgsConstructor
public class FishTypeController {

    private final FishTypeService fishTypeService;

    @PostMapping
    public FishTypeResponse create(
            @Valid @RequestBody FishTypeRequest request) {

        return fishTypeService.create(request);
    }

    @GetMapping
    public List<FishTypeResponse> getAll() {
        return fishTypeService.getAll();
    }

    @GetMapping("/{id}")
    public FishTypeResponse getById(
            @PathVariable Long id) {

        return fishTypeService.getById(id);
    }

    @PutMapping("/{id}")
    public FishTypeResponse update(
            @PathVariable Long id,
            @Valid @RequestBody FishTypeRequest request) {

        return fishTypeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFishType(@PathVariable Long id) {
        fishTypeService.delete(id);

        return ResponseEntity.ok(
                Map.of("message", "Deleted successfully")
        );
    }
}