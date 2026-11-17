package com.fishfarm.management.repository;
import com.fishfarm.management.entity.AppAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface AppAccountRepository extends JpaRepository<AppAccount,Long> {
    Optional<AppAccount> findByUsername(String username);
    boolean existsByUsername(String username);
}
