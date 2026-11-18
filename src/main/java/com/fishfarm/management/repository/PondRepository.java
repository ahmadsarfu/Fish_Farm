package com.fishfarm.management.repository;

import com.fishfarm.management.entity.Pond;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PondRepository extends JpaRepository<Pond, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pond p where p.id = :id")
    Optional<Pond> findForFeedingUpdate(@Param("id") Long id);

    long countByDeletedFalse();

    boolean existsByPondNumber(String pondNumber);

    List<Pond> findByDeletedFalse();
}
