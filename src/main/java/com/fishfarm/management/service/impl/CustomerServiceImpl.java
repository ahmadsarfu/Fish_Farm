package com.fishfarm.management.service.impl;

import com.fishfarm.management.dto.CustomerRequest;
import com.fishfarm.management.dto.CustomerResponse;
import com.fishfarm.management.entity.Customer;
import com.fishfarm.management.repository.CustomerRepository;
import com.fishfarm.management.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository repository;

    @Override
    public CustomerResponse create(CustomerRequest request) {

        if (request.getMobile() != null &&
                !request.getMobile().isBlank()) {

            repository
                    .findByMobileAndDeletedFalse(request.getMobile())
                    .ifPresent(c -> {
                        throw new RuntimeException(
                                "Customer with this mobile already exists"
                        );
                    });
        }

        Customer customer = new Customer();

        customer.setName(request.getName().trim());
        customer.setMobile(request.getMobile());
        customer.setAddress(request.getAddress());
        customer.setGstNumber(request.getGstNumber());
        customer.setCustomerType(request.getCustomerType());

        customer.setCreatedBy(com.fishfarm.management.security.FarmContext.userId());

        return map(repository.save(customer));
    }

    @Override
    public List<CustomerResponse> getAll() {
        return repository.findByDeletedFalse()
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    public CustomerResponse getById(Long id) {

        Customer customer =
                repository.findByIdAndDeletedFalse(id)
                        .orElseThrow(() ->
                                new RuntimeException("Customer not found")
                        );

        return map(customer);
    }

    @Override
    public CustomerResponse update(Long id, CustomerRequest request) {

        Customer customer = repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() ->
                        new RuntimeException("Customer not found")
                );

        // If mobile changed, prevent duplicate mobile
        if (request.getMobile() != null &&
                !request.getMobile().isBlank() &&
                !request.getMobile().equals(customer.getMobile())) {

            repository.findByMobileAndDeletedFalse(request.getMobile())
                    .ifPresent(existing -> {
                        throw new RuntimeException(
                                "Customer with this mobile already exists"
                        );
                    });
        }

        customer.setName(request.getName().trim());
        customer.setMobile(request.getMobile());
        customer.setAddress(request.getAddress());
        customer.setGstNumber(request.getGstNumber());
        customer.setCustomerType(request.getCustomerType());
        customer.setUpdatedBy(com.fishfarm.management.security.FarmContext.userId());

        return map(repository.save(customer));
    }

    @Override
    public void delete(Long id) {

        Customer customer = repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() ->
                        new RuntimeException("Customer not found")
                );

        customer.setDeleted(true);
        customer.setUpdatedBy(com.fishfarm.management.security.FarmContext.userId());

        repository.save(customer);
    }

    private CustomerResponse map(Customer customer) {

        return CustomerResponse.builder()
                .id(customer.getId())
                .name(customer.getName())
                .mobile(customer.getMobile())
                .address(customer.getAddress())
                .gstNumber(customer.getGstNumber())
                .customerType(customer.getCustomerType())
                .build();
    }
}