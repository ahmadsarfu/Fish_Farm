package com.fishfarm.management.repository;

import com.fishfarm.management.entity.FishType;
import com.fishfarm.management.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByFishType(FishType fishType);
}
