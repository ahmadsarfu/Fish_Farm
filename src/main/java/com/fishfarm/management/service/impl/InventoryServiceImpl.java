package com.fishfarm.management.service.impl;

import com.fishfarm.management.dto.InventoryResponse;
import com.fishfarm.management.entity.Inventory;
import com.fishfarm.management.repository.InventoryRepository;
import com.fishfarm.management.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;

    @Override
    public List<InventoryResponse> getAll() {

        return inventoryRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public InventoryResponse getByFishType(Long fishTypeId) {

        Inventory inventory = inventoryRepository.findAll()
                .stream()
                .filter(i -> i.getFishType().getId().equals(fishTypeId))
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException("Inventory not found"));

        return mapToResponse(inventory);
    }

    private InventoryResponse mapToResponse(Inventory inventory) {

        return InventoryResponse.builder()
                .fishTypeId(inventory.getFishType().getId())
                .fishTypeName(inventory.getFishType().getName())
                .currentStockKg(inventory.getCurrentStockKg())
                .build();
    }
}