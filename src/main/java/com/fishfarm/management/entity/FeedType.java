package com.fishfarm.management.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "feed_types", uniqueConstraints = @UniqueConstraint(name = "uq_feed_types_farm_key", columnNames = {"farm_id", "size_mm"}))
@Getter
@Setter
public class FeedType extends FarmOwnedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = false)
    private Integer sizeMm;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ratePerKg;
    @Column(nullable = false)
    private Boolean active = true;
}
