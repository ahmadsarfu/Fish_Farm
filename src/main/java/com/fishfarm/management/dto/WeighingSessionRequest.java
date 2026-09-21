package com.fishfarm.management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WeighingSessionRequest {

    @NotNull
    private Long dispatchId;

    @NotNull
    private Long fishTypeId;

    private String sessionLabel;

    @NotNull
    private Long pondId;
}