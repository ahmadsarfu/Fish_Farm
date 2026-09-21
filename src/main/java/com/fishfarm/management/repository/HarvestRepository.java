package com.fishfarm.management.repository;


import com.fishfarm.management.entity.Harvest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface HarvestRepository extends JpaRepository<Harvest, Long> {

    @Query("""
            select distinct h
            from Harvest h
            left join fetch h.items i
            left join fetch i.fishType
            left join fetch h.pond
            where h.deleted = false
            """)
    List<Harvest> findAllWithItems();

    @Query("""
            select distinct h
            from Harvest h
            left join fetch h.items i
            left join fetch i.fishType
            left join fetch h.pond
            where h.id = :id
            """)
    Optional<Harvest> findByIdWithItems(Long id);
}
