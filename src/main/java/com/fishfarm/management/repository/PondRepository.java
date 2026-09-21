package com.fishfarm.management.repository;

import com.fishfarm.management.entity.Pond;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PondRepository extends JpaRepository<Pond, Long> {

    boolean existsByPondNumber(String pondNumber);

    List<Pond> findByDeletedFalse();
}
