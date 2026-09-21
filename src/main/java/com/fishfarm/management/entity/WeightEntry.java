package com.fishfarm.management.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "weight_entries")
@Data
public class WeightEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private WeighingSession session;

    private Integer basketNumber;

    private BigDecimal weightKg;

    private String remarks;

    private Long createdBy;

    private LocalDateTime createdAt;
}