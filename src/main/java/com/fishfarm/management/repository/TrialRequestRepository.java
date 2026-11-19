package com.fishfarm.management.repository;
import com.fishfarm.management.entity.TrialRequest;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface TrialRequestRepository extends JpaRepository<TrialRequest,Long> {
    Page<TrialRequest> findByStatusOrderByIdDesc(String status,Pageable page);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from TrialRequest r where r.id=:id")
    Optional<TrialRequest> lockById(Long id);
    @Modifying @Query(value="INSERT IGNORE INTO trial_requests (owner_name,farm_name,mobile,email,status,created_at) VALUES (:owner,:farm,:mobile,:email,'PENDING',:created)",nativeQuery=true)
    int submit(String owner,String farm,String mobile,String email,java.time.Instant created);
}
