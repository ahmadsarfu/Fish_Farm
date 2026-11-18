package com.fishfarm.management.repository;
import com.fishfarm.management.entity.Farm;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface FarmRepository extends JpaRepository<Farm,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select f from Farm f where f.id = :id")
    Optional<Farm> lockById(Long id);
    org.springframework.data.domain.Page<Farm> findByNameContainingIgnoreCaseOrderByIdDesc(String name,org.springframework.data.domain.Pageable pageable);
    boolean existsByLegacyClaimableTrue();
}
