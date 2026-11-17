package com.fishfarm.management.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FishStockEntryRequest {

    @NotNull
    private Long fishTypeId;

    @NotNull
    private Long pondId;

    @NotNull
    private LocalDate stockDate;

    @PositiveOrZero
    private BigDecimal sizeFromGram;

    @PositiveOrZero
    private BigDecimal sizeToGram;

    @PositiveOrZero
    private Integer quantity;

    @NotNull
    @PositiveOrZero
    private BigDecimal purchaseAmount;

    private String supplierName;

    private String remarks;
}