package com.fishfarm.management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WeightEntryRequest {

    @NotNull
    private Long sessionId;

    @NotNull
    private BigDecimal weightKg;

    private String remarks;
}