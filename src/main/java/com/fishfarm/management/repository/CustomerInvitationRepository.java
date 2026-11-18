package com.fishfarm.management.repository;
import com.fishfarm.management.entity.CustomerInvitation;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface CustomerInvitationRepository extends JpaRepository<CustomerInvitation,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select i from CustomerInvitation i where i.tokenHash=:hash")
    Optional<CustomerInvitation> lockByTokenHash(String hash);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select i from CustomerInvitation i where i.farm.id=:farmId")
    Optional<CustomerInvitation> lockByFarmId(Long farmId);
    @Query("select i.farm.id from CustomerInvitation i where i.tokenHash=:hash")
    Optional<Long> findFarmIdByTokenHash(String hash);
    Optional<CustomerInvitation> findByTokenHash(String tokenHash);
    @EntityGraph(attributePaths={"account","farm"})
    java.util.List<CustomerInvitation> findByFarmIdIn(java.util.Collection<Long> farmIds);
    Optional<CustomerInvitation> findByFarmId(Long farmId);
}
