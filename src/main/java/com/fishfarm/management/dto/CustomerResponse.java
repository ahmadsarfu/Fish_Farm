package com.fishfarm.management.dto;

import com.fishfarm.management.enums.CustomerType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerResponse {

    private Long id;
    private String name;
    private String mobile;
    private String address;
    private String gstNumber;
    private CustomerType customerType;
}