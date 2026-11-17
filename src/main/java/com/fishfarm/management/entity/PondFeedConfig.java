
package com.fishfarm.management.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "pond_feed_configs", uniqueConstraints = @UniqueConstraint(name = "uq_pond_feed_config", columnNames = "pond_id"))
@Data
public class PondFeedConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pond_id", nullable = false)
    private Pond pond;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_type_id", nullable = false)
    private FeedType feedType;

    @Column(name = "morning_qty_kg", nullable = false, precision = 10, scale = 3)
    private BigDecimal morningQtyKg;

    @Column(name = "evening_qty_kg", nullable = false, precision = 10, scale = 3)
    private BigDecimal eveningQtyKg;
}
