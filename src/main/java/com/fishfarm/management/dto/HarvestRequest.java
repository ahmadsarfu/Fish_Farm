package com.fishfarm.management.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;


@Data
public class HarvestRequest {

    @NotNull(message = "Pond id is required")
    private Long pondId;

    @NotNull(message = "Harvest date is required")
    private LocalDate harvestDate;

    private String remarks;

    @Valid
    @NotNull(message = "Harvest items are required")
    private List<HarvestItemRequest> items;
}
