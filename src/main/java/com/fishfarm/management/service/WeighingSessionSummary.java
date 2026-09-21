package com.fishfarm.management.service;

import com.fishfarm.management.dto.WeighingSessionSummaryResponse;

public interface WeighingSessionSummary {

    WeighingSessionSummaryResponse getSummary(Long sessionId);
}
