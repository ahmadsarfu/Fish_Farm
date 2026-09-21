package com.fishfarm.management.controller;

import com.fishfarm.management.dto.DispatchRequest;
import com.fishfarm.management.dto.DispatchResponse;
import com.fishfarm.management.service.DispatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dispatches")
@RequiredArgsConstructor
public class DispatchController {

    private final DispatchService dispatchService;

    @PostMapping
    public DispatchResponse create(@Valid @RequestBody DispatchRequest request) {

        return dispatchService.create(request);
    }

    @GetMapping
    public List<DispatchResponse> getAll() {

        return dispatchService.getAll();
    }

    @GetMapping("/{id}")
    public DispatchResponse getById(@PathVariable Long id) {

        return dispatchService.getById(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFishType(@PathVariable Long id) {
        dispatchService.delete(id);

        return ResponseEntity.ok(
                Map.of("message", "Deleted successfully")
        );
    }

    @PutMapping("/{id}/dispatch")
    public DispatchResponse markAsDispatched(@PathVariable Long id) {
        return dispatchService.markAsDispatched(id);
    }
}
