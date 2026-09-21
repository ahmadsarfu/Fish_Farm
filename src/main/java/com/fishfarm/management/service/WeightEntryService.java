package com.fishfarm.management.service;

import com.fishfarm.management.dto.WeighingSessionSummaryResponse;
import com.fishfarm.management.dto.WeightEntryRequest;
import com.fishfarm.management.dto.WeightEntryResponse;

import java.util.List;

public interface WeightEntryService {

    WeightEntryResponse create(WeightEntryRequest request);

    WeightEntryResponse getById(Long id);

    List<WeightEntryResponse> getBySession(Long sessionId);

    void delete(Long id);

    WeighingSessionSummaryResponse getSummary(Long sessionId);
}