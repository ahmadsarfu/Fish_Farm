package com.fishfarm.management.repository;
import com.fishfarm.management.entity.FeedRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface FeedRevisionRepository extends JpaRepository<FeedRevision,Long> {
    List<FeedRevision> findByFeedIdOrderByRevisionAsc(Long feedId);
}
