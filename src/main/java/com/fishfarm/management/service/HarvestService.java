package com.fishfarm.management.service;

import com.fishfarm.management.dto.HarvestRequest;
import com.fishfarm.management.dto.HarvestResponse;

import java.util.List;

public interface HarvestService {

    HarvestResponse createForBatch(HarvestRequest request);

    HarvestResponse create(HarvestRequest request);

    List<HarvestResponse> getAll();

    HarvestResponse getById(Long id);

    void delete(Long id);
}