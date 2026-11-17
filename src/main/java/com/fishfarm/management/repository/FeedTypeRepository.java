package com.fishfarm.management.repository;

import com.fishfarm.management.entity.FeedType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FeedTypeRepository extends JpaRepository<FeedType, Long> {
    List<FeedType> findAllByOrderBySizeMmAsc();
    boolean existsBySizeMmAndIdNot(Integer sizeMm, Long id);
}
