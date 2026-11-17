package com.fishfarm.management.service;

import com.fishfarm.management.dto.CustomerRequest;
import com.fishfarm.management.dto.CustomerResponse;

import java.util.List;

public interface CustomerService {

    CustomerResponse create(CustomerRequest request);

    List<CustomerResponse> getAll();

    CustomerResponse getById(Long id);

    CustomerResponse update(Long id, CustomerRequest request);

    void delete(Long id);
}