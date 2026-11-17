package com.fishfarm.management.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "feeding_records", uniqueConstraints = @UniqueConstraint(name = "uq_pond_date_slot", columnNames = {"pond_id", "feeding_date", "feeding_slot"}))
@Data
public class FeedingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pond_id", nullable = false)
    private Pond pond;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_type_id", nullable = false)
    private FeedType feedType;

    @Column(name = "feeding_date", nullable = false)
    private LocalDate feedingDate;

    @Column(name = "feeding_slot", nullable = false)
    private String feedingSlot;

    @Column(name = "quantity_kg", nullable = false)
    private BigDecimal quantityKg;

    @Column(name = "rate_per_kg", nullable = false)
    private BigDecimal ratePerKg;

    @Column(name = "total_cost", nullable = false)
    private BigDecimal totalCost;

    @Column(name = "expense_id")
    private Long expenseId;
}
