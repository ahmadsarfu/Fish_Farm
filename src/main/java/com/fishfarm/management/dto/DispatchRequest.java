package com.fishfarm.management.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DispatchRequest {

    @NotBlank
    private String vehicleNumber;

    @NotBlank
    private String driverName;

    @NotNull
    private LocalDate dispatchDate;

    private String driverPhone;
}
