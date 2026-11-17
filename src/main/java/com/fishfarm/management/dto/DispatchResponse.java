package com.fishfarm.management.dto;

import com.fishfarm.management.enums.DispatchStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class DispatchResponse {

    private Long id;

    private String dispatchNumber;

    private String vehicleNumber;

    private String driverName;

    private LocalDate dispatchDate;

    private DispatchStatus status;

    private String driverPhone;

    private Long customerId;
    private String customerName;
    private String customerMobile;
}