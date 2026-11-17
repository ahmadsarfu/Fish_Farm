package com.fishfarm.management.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class FishStockEntryResponse {

    private Long id;

    private Long fishTypeId;
    private String fishTypeName;

    private Long pondId;
    private String pondNumber;
    private String pondName;

    private LocalDate stockDate;

    private BigDecimal sizeFromGram;
    private BigDecimal sizeToGram;

    private Integer quantity;

    private BigDecimal purchaseAmount;

    private String supplierName;
    private String remarks;

    private LocalDateTime createdAt;
}