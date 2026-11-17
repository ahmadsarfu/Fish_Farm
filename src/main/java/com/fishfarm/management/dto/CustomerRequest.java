package com.fishfarm.management.dto;

import com.fishfarm.management.enums.CustomerType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerRequest {

    @NotBlank
    private String name;

    private String mobile;
    private String address;
    private String gstNumber;

    private CustomerType customerType = CustomerType.RETAIL;
}