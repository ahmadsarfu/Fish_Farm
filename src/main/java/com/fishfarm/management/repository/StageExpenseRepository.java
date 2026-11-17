package com.fishfarm.management.repository;
import com.fishfarm.management.entity.StageExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
public interface StageExpenseRepository extends JpaRepository<StageExpense,Long> {
List<StageExpense> findByStageBatchIdOrderByExpenseDateAscIdAsc(Long batchId);
@Lock(LockModeType.PESSIMISTIC_WRITE)
List<StageExpense> findByStageId(Long stageId);
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<StageExpense> findByFeedEntryId(Long feedEntryId);
}
