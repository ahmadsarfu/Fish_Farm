package com.fishfarm.management.repository;

import com.fishfarm.management.entity.PondFeedConfig;
import org.springframework.data.jpa.repository.*;
import java.util.List;
import java.util.Optional;

public interface PondFeedConfigRepository extends JpaRepository<PondFeedConfig, Long> {
    @Query("select c from PondFeedConfig c join fetch c.pond join fetch c.feedType")
    List<PondFeedConfig> findAllWithDetails();
    Optional<PondFeedConfig> findByPondId(Long pondId);
}
