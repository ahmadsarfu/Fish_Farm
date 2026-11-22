package com.fishfarm.management.entity;

import com.fishfarm.management.enums.FeedingTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "daily_feeds", uniqueConstraints = @UniqueConstraint(name = "uq_daily_feed", columnNames = {"pond_id", "feeding_date", "feeding_time", "feed_type_id"}))
@Getter
@Setter
public class DailyFeed extends FarmOwnedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pond_id", nullable = false)
    private Pond pond;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feed_type_id", nullable = false)
    private FeedType feedType;
    @Column(nullable = false)
    private LocalDate feedingDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10)
    private FeedingTime feedingTime;
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal quantityKg;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ratePerKg;
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal expense;
    @Column(length = 500)
    private String notes;
    @Column(nullable=false, updatable=false, precision=10, scale=3) private BigDecimal originalQuantityKg;
    @Column(nullable=false, updatable=false, precision=10, scale=2) private BigDecimal originalRatePerKg;
    @Column(nullable=false, updatable=false, precision=18, scale=2) private BigDecimal originalExpense;
    @Column(nullable=false,length=16) private String status = "RECORDED";
    @Column(nullable=false,length=16,updatable=false) private String source = "LEGACY";
    @Version @Column(nullable=false) private long version;
    @PrePersist void preserveOriginal() {
        originalQuantityKg=quantityKg; originalRatePerKg=ratePerKg; originalExpense=expense;
    }
    public boolean cancelled() { return "CANCELLED".equals(status); }
    public BigDecimal netQuantity() { return cancelled() ? BigDecimal.ZERO : quantityKg; }
    public BigDecimal netExpense() { return cancelled() ? BigDecimal.ZERO : expense; }
}
