package com.fishfarm.management.entity;


import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "weighing_sessions", uniqueConstraints = {@UniqueConstraint(name = "uq_dispatch_fish", columnNames = {"dispatch_id", "fish_type_id"})})
@Data
public class WeighingSession extends FarmOwnedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatch_id", nullable = false)
    private Dispatch dispatch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fish_type_id", nullable = false)
    private FishType fishType;

    private String sessionLabel;

    private Boolean isClosed = false;

    @Column(updatable = false)
    private Long createdBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private Long updatedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime updatedAt;

    @Column(precision = 10, scale = 2)
    private BigDecimal ratePerKg;

    @Column(precision = 10, scale = 3)
    private BigDecimal netWeightKg;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column
    private BigDecimal deductionPct = new BigDecimal("3.00");

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pond_id")
    private Pond pond;

}
