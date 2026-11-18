package com.fishfarm.management.repository;
import com.fishfarm.management.entity.FarmMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface FarmMemberRepository extends JpaRepository<FarmMember,Long> {
    List<FarmMember> findByAccountIdAndActiveTrueOrderByIdAsc(Long accountId);
    Optional<FarmMember> findByAccountIdAndFarmIdAndActiveTrue(Long accountId, Long farmId);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths={"account","farm"})
    List<FarmMember> findByFarmIdInOrderByIdAsc(Collection<Long> farmIds);
    List<FarmMember> findByFarmIdOrderByIdAsc(Long farmId);
    long countByFarmIdAndActiveTrue(Long farmId);
    boolean existsByFarmId(Long farmId);
}
