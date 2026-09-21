package com.fishfarm.management.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class WeightEntryResponse {

    private Long id;

    private Long sessionId;

    private Integer basketNumber;

    private BigDecimal weightKg;

    private String remarks;
}