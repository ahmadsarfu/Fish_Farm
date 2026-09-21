package com.fishfarm.management.dto;


import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class HarvestItemResponse {

    private Long fishTypeId;

    private String fishTypeName;

    private BigDecimal weightKg;
}
