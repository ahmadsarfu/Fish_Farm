package com.fishfarm.management.service;

import com.fishfarm.management.dto.FishTypeRequest;
import com.fishfarm.management.dto.FishTypeResponse;

import java.util.List;

public interface FishTypeService {

    FishTypeResponse create(FishTypeRequest request);

    List<FishTypeResponse> getAll();

    FishTypeResponse getById(Long id);

    FishTypeResponse update(Long id, FishTypeRequest request);

    void delete(Long id);
}
