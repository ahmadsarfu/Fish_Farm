package com.fishfarm.management.service;

import com.fishfarm.management.dto.InventoryResponse;

import java.util.List;

public interface InventoryService {

    List<InventoryResponse> getAll();

    InventoryResponse getByFishType(Long fishTypeId);
}
