package com.fishfarm.management.controller;

import com.fishfarm.management.dto.PondRequest;
import com.fishfarm.management.dto.PondResponse;
import com.fishfarm.management.service.PondService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ponds")
@RequiredArgsConstructor
public class PondController {

    private final PondService pondService;

    @PostMapping
    public PondResponse create(@Valid @RequestBody PondRequest request) {

        return pondService.create(request);
    }

    @GetMapping
    public List<PondResponse> getAll() {
        return pondService.getAll();
    }

    @GetMapping("/{id}")
    public PondResponse getById(@PathVariable Long id) {
        return pondService.getById(id);
    }

    @PutMapping("/{id}")
    public PondResponse update(@PathVariable Long id, @Valid @RequestBody PondRequest request) {

        return pondService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFishType(@PathVariable Long id) {
        pondService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Deleted successfully"));
    }
}
