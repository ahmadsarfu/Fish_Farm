package com.fishfarm.management.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;
@Entity @Table(name="feed_revisions",uniqueConstraints=@UniqueConstraint(name="uq_feed_revision",columnNames={"feed_id","revision"})) @Getter @Setter
public class FeedRevision extends FarmOwnedEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="feed_id",nullable=false,updatable=false) private DailyFeed feed;
    @Column(nullable=false,updatable=false) private long revision;
    @Column(nullable=false,length=16,updatable=false) private String action;
    @Column(nullable=false,precision=10,scale=3,updatable=false) private BigDecimal beforeQuantityKg;
    @Column(nullable=false,precision=10,scale=3,updatable=false) private BigDecimal afterQuantityKg;
    @Column(nullable=false,precision=18,scale=2,updatable=false) private BigDecimal beforeExpense;
    @Column(nullable=false,precision=18,scale=2,updatable=false) private BigDecimal afterExpense;
    @Column(nullable=false,precision=18,scale=2,updatable=false) private BigDecimal adjustment;
    @Column(nullable=false,length=500,updatable=false) private String reason;
    @Column(nullable=false,updatable=false) private Long actorId;
    @Column(nullable=false,length=120,updatable=false) private String actorName;
    @Column(nullable=false,updatable=false) private Instant reviewedAt;
}
