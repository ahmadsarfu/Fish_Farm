package com.fishfarm.management.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FishTypeResponse {

    private Long id;
    private String name;
    private Boolean active;
}
