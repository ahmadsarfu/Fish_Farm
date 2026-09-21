package com.fishfarm.management.repository;

import com.fishfarm.management.entity.WeighingSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WeighingSessionRepository extends JpaRepository<WeighingSession, Long> {

    boolean existsByDispatchIdAndFishTypeId(Long dispatchId, Long fishTypeId);

    List<WeighingSession> findByDispatchId(Long dispatchId);
}