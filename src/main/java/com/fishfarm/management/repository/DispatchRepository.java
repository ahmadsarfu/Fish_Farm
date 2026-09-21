package com.fishfarm.management.repository;


import com.fishfarm.management.entity.Dispatch;
import com.fishfarm.management.enums.DispatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DispatchRepository extends JpaRepository<Dispatch, Long> {

    List<Dispatch> findByDeletedFalse();

    Optional<Dispatch> findByIdAndDeletedFalse(Long id);

    List<Dispatch> findByStatusAndDeletedFalse(DispatchStatus status);
}
