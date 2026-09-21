package com.fishfarm.management.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class HarvestResponse {

    private Long id;

    private String harvestNumber;

    private Long pondId;

    private String pondName;

    private LocalDate harvestDate;

    private String remarks;

    private List<HarvestItemResponse> items;
}
