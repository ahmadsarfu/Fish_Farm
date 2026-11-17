package com.fishfarm.management.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
@Entity @Table(name="fish_batches", uniqueConstraints=@UniqueConstraint(name="uq_batch_code",columnNames={"farm_id","code"})) @Getter @Setter
public class FishBatch extends FarmOwnedEntity {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
@Column(nullable=false,length=40) private String code;
@Column(nullable=false,length=120) private String name;
@ManyToOne(optional=false) @JoinColumn(name="fish_type_id") private FishType fishType;
@OneToOne @JoinColumn(name="stock_entry_id",unique=true) private FishStockEntry stockEntry;
@Column(nullable=false) private LocalDate startedOn;
@Column(nullable=false) private Integer initialCount;
@Column(nullable=false,precision=18,scale=2) private BigDecimal purchaseCost;
}
