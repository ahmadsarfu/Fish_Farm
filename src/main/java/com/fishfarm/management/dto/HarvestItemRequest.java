package com.fishfarm.management.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class HarvestItemRequest {

    @NotNull(message = "Fish type id is required")
    private Long fishTypeId;

    @NotNull(message = "Weight is required")
    @DecimalMin(value = "0.001", message = "Weight must be greater than zero")
    private BigDecimal weightKg;
}