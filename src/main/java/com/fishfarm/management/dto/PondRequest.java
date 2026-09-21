package com.fishfarm.management.dto;

import com.fishfarm.management.enums.PondStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PondRequest {

    @NotBlank
    private String pondNumber;

    @NotBlank
    private String pondName;

    private BigDecimal areaAcres;

    private String description;

    private PondStatus status;
}