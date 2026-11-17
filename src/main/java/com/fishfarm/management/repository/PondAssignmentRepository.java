package com.fishfarm.management.repository;
import com.fishfarm.management.entity.PondAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface PondAssignmentRepository extends JpaRepository<PondAssignment,Long> {
    List<PondAssignment> findByAccountId(Long accountId);
    boolean existsByAccountIdAndPondId(Long accountId, Long pondId);
    void deleteByAccountId(Long accountId);
}
