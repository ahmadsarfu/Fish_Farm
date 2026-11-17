package com.fishfarm.management.repository;

import com.fishfarm.management.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByDeletedFalse();

    Optional<Customer> findByIdAndDeletedFalse(Long id);

    Optional<Customer> findByMobileAndDeletedFalse(String mobile);
}