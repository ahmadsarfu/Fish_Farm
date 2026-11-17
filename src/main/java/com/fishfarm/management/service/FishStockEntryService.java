package com.fishfarm.management.service;

import com.fishfarm.management.dto.FishStockEntryRequest;
import com.fishfarm.management.dto.FishStockEntryResponse;

import java.util.List;

public interface FishStockEntryService {

    FishStockEntryResponse create(FishStockEntryRequest request);

    List<FishStockEntryResponse> getAll();

    FishStockEntryResponse getById(Long id);

    FishStockEntryResponse update(
            Long id,
            FishStockEntryRequest request
    );

    void delete(Long id);
}