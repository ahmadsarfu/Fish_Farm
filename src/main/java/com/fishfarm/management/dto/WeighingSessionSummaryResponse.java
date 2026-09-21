package com.fishfarm.management.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class WeighingSessionSummaryResponse {

    private Long sessionId;

    private Long fishTypeId;

    private String fishTypeName;

    private Integer totalBaskets;

    private BigDecimal totalWeightKg;
}