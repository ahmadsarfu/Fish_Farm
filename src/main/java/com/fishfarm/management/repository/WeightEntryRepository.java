package com.fishfarm.management.repository;


import com.fishfarm.management.entity.WeightEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WeightEntryRepository
        extends JpaRepository<WeightEntry, Long> {

    List<WeightEntry> findBySessionId(Long sessionId);

    Integer countBySessionId(Long sessionId);
}