package com.fishfarm.management.controller;

import com.fishfarm.management.dto.InventoryResponse;
import com.fishfarm.management.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public List<InventoryResponse> getAll() {

        return inventoryService.getAll();
    }

    @GetMapping("/{fishTypeId}")
    public InventoryResponse getByFishType(
            @PathVariable Long fishTypeId) {

        return inventoryService.getByFishType(fishTypeId);
    }
}
