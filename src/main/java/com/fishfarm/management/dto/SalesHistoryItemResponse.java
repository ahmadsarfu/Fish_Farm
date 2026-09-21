package com.fishfarm.management.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class SalesHistoryItemResponse {

    private Long dispatchId;
    private String dispatchNumber;

    private String vehicleNumber;
    private String driverName;
    private String driverPhone;

    private LocalDate dispatchDate;

    private BigDecimal totalWeightKg;
    private BigDecimal totalAmount;
}