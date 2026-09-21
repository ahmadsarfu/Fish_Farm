package com.fishfarm.management.dto;

import jakarta.persistence.Column;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class WeighingSessionResponse {

    private Long id;

    private Long dispatchId;

    private String dispatchNumber;

    private Long fishTypeId;

    private String fishTypeName;

    private String sessionLabel;

    private Boolean isClosed;

    private BigDecimal ratePerKg;

    private BigDecimal netWeightKg;

    private BigDecimal totalAmount;

    private Long pondId;

    private String pondNumber;

    private String pondName;
}