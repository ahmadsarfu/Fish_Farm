package com.fishfarm.management.dto;



import com.fishfarm.management.enums.PondStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PondResponse {

    private Long id;

    private String pondNumber;

    private String pondName;

    private BigDecimal areaAcres;

    private String description;

    private PondStatus status;
}