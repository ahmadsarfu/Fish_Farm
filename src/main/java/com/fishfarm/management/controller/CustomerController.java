package com.fishfarm.management.controller;

import com.fishfarm.management.dto.CustomerRequest;
import com.fishfarm.management.dto.CustomerResponse;
import com.fishfarm.management.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService service;

    @PostMapping
    public CustomerResponse create(
            @Valid @RequestBody CustomerRequest request
    ) {
        return service.create(request);
    }

    @GetMapping
    public List<CustomerResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public CustomerResponse getById(
            @PathVariable Long id
    ) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public CustomerResponse update(
            @PathVariable Long id,
            @Valid @RequestBody CustomerRequest request
    ) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id
    ) {

        service.delete(id);

        return ResponseEntity.ok(
                Map.of("message", "Customer deleted successfully")
        );
    }
}