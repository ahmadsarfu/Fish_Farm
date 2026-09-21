package com.fishfarm.management.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FishTypeRequest {

    @NotBlank(message = "Fish name is required")
    private String name;

    private Boolean active;

}