package com.fishfarm.management.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
@Entity @Table(name="batch_stages", uniqueConstraints=@UniqueConstraint(name="uq_active_pond_stage",columnNames={"farm_id","active_pond_id"})) @Getter @Setter
public class BatchStage extends FarmOwnedEntity {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
@ManyToOne(optional=false) @JoinColumn(name="batch_id") private FishBatch batch;
@ManyToOne(optional=false) @JoinColumn(name="pond_id") private Pond pond;
@ManyToOne @JoinColumn(name="parent_stage_id") private BatchStage parent;
@Column(nullable=false) private Integer stageNumber;
@Column(nullable=false) private LocalDate startedOn;
private LocalDate endedOn;
@Column(nullable=false) private LocalDate lastActivityOn;
@Column(nullable=false) private Integer openingCount;
@Column(nullable=false) private Integer currentCount;
@Column(nullable=false) private Integer deadCount=0;
@Column(nullable=false) private Integer transferredCount=0;
@Column(nullable=false) private Integer harvestedCount=0;
@Column(nullable=false,precision=18,scale=2) private BigDecimal openingCost;
@Column(nullable=false,precision=18,scale=2) private BigDecimal transferredCost=BigDecimal.ZERO;
@Column(nullable=false,precision=18,scale=2) private BigDecimal harvestedCost=BigDecimal.ZERO;
@Column(name="active_pond_id",insertable=false,updatable=false,columnDefinition="BIGINT GENERATED ALWAYS AS (CASE WHEN current_count > 0 THEN pond_id ELSE NULL END) STORED") private Long activePondId;
}
