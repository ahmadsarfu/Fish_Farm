package com.fishfarm.management.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
@Entity @Table(name="stage_expenses") @Getter @Setter
public class StageExpense extends FarmOwnedEntity {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
@ManyToOne(optional=false) @JoinColumn(name="stage_id") private BatchStage stage;
@Column(nullable=false) private LocalDate expenseDate;
@Column(nullable=false,length=20) private String category;
@Column(nullable=false,precision=18,scale=2) private BigDecimal amount;
@Column(unique=true) private Long feedEntryId;
@Column(length=500) private String notes;
}
