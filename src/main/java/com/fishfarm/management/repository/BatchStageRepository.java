package com.fishfarm.management.repository;
import com.fishfarm.management.entity.BatchStage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
public interface BatchStageRepository extends JpaRepository<BatchStage,Long> {
List<BatchStage> findByBatchIdOrderByIdAsc(Long batchId);
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<BatchStage> findByPondIdAndCurrentCountGreaterThan(Long pondId,Integer count);
}
