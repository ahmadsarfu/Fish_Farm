package com.fishfarm.management.service;

import com.fishfarm.management.dto.WeighingSessionRequest;
import com.fishfarm.management.dto.WeighingSessionResponse;
import com.fishfarm.management.dto.WeighingSessionSummaryResponse;

import java.math.BigDecimal;
import java.util.List;

public interface WeighingSessionService {

    WeighingSessionResponse create(WeighingSessionRequest request);

    List<WeighingSessionResponse> getAll();

    WeighingSessionResponse getById(Long id);

    WeighingSessionResponse closeSession(Long id, BigDecimal ratePerKg,BigDecimal deductionPct);

    void delete(Long id);

}