package com.fishfarm.management.repository;
import com.fishfarm.management.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface AuditEventRepository extends JpaRepository<AuditEvent,Long> {
List<AuditEvent> findTop100ByOrderByIdDesc();
}
