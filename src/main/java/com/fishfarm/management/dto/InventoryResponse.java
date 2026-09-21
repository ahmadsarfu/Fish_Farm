package com.fishfarm.management.dto;


import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class InventoryResponse {

    private Long fishTypeId;

    private String fishTypeName;

    private BigDecimal currentStockKg;
}