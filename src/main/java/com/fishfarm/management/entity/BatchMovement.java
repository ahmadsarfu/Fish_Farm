package com.fishfarm.management.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
@Entity @Table(name="batch_movements", uniqueConstraints=@UniqueConstraint(name="uq_movement_request",columnNames={"farm_id","request_id"})) @Getter @Setter
public class BatchMovement extends FarmOwnedEntity {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
@ManyToOne(optional=false) @JoinColumn(name="stage_id") private BatchStage stage;
@ManyToOne @JoinColumn(name="target_stage_id") private BatchStage target;
@Column(nullable=false,length=36) private String requestId;
@Column(nullable=false,length=64) private String fingerprint;
@Column(nullable=false,length=20) private String kind;
@Column(nullable=false) private LocalDate eventDate;
@Column(nullable=false) private Integer fishCount;
@Column(nullable=false,precision=18,scale=2) private BigDecimal allocatedCost=BigDecimal.ZERO;
@Column(nullable=false,precision=18,scale=2) private BigDecimal revenue=BigDecimal.ZERO;
@Column(precision=12,scale=3) private BigDecimal weightKg;
private Long harvestId;
@Column(length=500) private String notes;
}
