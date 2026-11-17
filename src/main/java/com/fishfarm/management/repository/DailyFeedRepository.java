package com.fishfarm.management.repository;

import com.fishfarm.management.entity.DailyFeed;
import com.fishfarm.management.enums.FeedingTime;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface DailyFeedRepository extends JpaRepository<DailyFeed, Long> {
    @Query("select f from DailyFeed f join fetch f.pond join fetch f.feedType where " +
            "f.feedingDate between :from and :to and (:pondId is null or f.pond.id = :pondId) " +
            "order by f.feedingDate desc, f.id desc")
    List<DailyFeed> search(@Param("from") LocalDate from, @Param("to") LocalDate to, @Param("pondId") Long pondId);
    boolean existsByPondIdAndFeedingDateAndFeedingTimeAndFeedTypeIdAndIdNot(
            Long pondId, LocalDate date, FeedingTime time, Long feedTypeId, Long id);
}
