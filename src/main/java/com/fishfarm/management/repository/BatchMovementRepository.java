package com.fishfarm.management.repository;
import com.fishfarm.management.entity.BatchMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
public interface BatchMovementRepository extends JpaRepository<BatchMovement,Long> {
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<BatchMovement> findByRequestId(String requestId);
boolean existsByHarvestId(Long harvestId);
List<BatchMovement> findByStageBatchIdOrderByEventDateAscIdAsc(Long batchId);
@Lock(LockModeType.PESSIMISTIC_WRITE)
List<BatchMovement> findByStageIdAndKindInAndEventDateGreaterThanEqual(Long stageId,List<String> kinds,java.time.LocalDate date);
}
