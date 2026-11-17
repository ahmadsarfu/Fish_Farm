
package com.fishfarm.management.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "pond_feed_configs")
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

    @Column(name = "morning_qty_kg")
    private BigDecimal morningQtyKg;

    @Column(name = "evening_qty_kg")
    private BigDecimal eveningQtyKg;
}
