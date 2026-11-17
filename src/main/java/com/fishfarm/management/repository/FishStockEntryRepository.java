package com.fishfarm.management.repository;

import com.fishfarm.management.entity.FishStockEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FishStockEntryRepository
        extends JpaRepository<FishStockEntry, Long> {

    List<FishStockEntry> findByDeletedFalseOrderByStockDateDescIdDesc();

    Optional<FishStockEntry> findByIdAndDeletedFalse(Long id);
}