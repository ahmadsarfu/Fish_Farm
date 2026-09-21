package com.fishfarm.management.repository;

import com.fishfarm.management.entity.HarvestItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HarvestItemRepository extends JpaRepository<HarvestItem, Long> {
}