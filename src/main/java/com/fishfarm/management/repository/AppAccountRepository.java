package com.fishfarm.management.repository;
import com.fishfarm.management.entity.AppAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface AppAccountRepository extends JpaRepository<AppAccount,Long> {
    Optional<AppAccount> findByUsername(String username);
    Optional<AppAccount> findByEmail(String email);
    Optional<AppAccount> findByMobile(String mobile);
    boolean existsByEmail(String email);
    boolean existsByMobile(String mobile);
    boolean existsByPlatformAdminTrue();
    boolean existsByUsername(String username);
}
