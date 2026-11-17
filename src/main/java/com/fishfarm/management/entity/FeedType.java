package com.fishfarm.management.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "feed_types")
@Getter
@Setter
public class FeedType {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private Integer sizeMm;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ratePerKg;
    @Column(nullable = false)
    private Boolean active = true;
}
