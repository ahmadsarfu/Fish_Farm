package com.fishfarm.management.repository;
import com.fishfarm.management.entity.FishBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface FishBatchRepository extends JpaRepository<FishBatch,Long> {
boolean existsByCode(String code);
boolean existsByStockEntryId(Long stockEntryId);
List<FishBatch> findAllByOrderByStartedOnDescIdDesc();
}
