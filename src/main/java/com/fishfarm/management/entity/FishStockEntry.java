package com.fishfarm.management.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fish_stock_entries")
@Data
public class FishStockEntry extends FarmOwnedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fish_type_id", nullable = false)
    private FishType fishType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pond_id", nullable = false)
    private Pond pond;

    @Column(name = "stock_date", nullable = false)
    private LocalDate stockDate;

    @Column(name = "size_from_gram", precision = 10, scale = 2)
    private BigDecimal sizeFromGram;

    @Column(name = "size_to_gram", precision = 10, scale = 2)
    private BigDecimal sizeToGram;

    private Integer quantity;

    @Column(name = "purchase_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal purchaseAmount;

    @Column(name = "supplier_name", length = 150)
    private String supplierName;

    @Column(length = 500)
    private String remarks;

    private Boolean deleted = false;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}