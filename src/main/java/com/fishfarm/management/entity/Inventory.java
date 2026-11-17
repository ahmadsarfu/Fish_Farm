package com.fishfarm.management.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory")
@Data
public class Inventory extends FarmOwnedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "fish_type_id")
    private FishType fishType;

    @Column(name = "current_stock_kg")
    private BigDecimal currentStockKg;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Version
    private Long version;
}