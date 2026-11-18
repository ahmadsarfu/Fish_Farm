package com.fishfarm.management.repository;
import com.fishfarm.management.entity.PlatformAuditEvent;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PlatformAuditRepository extends JpaRepository<PlatformAuditEvent,Long> {
    Page<PlatformAuditEvent> findAllByOrderByIdDesc(Pageable pageable);
}
