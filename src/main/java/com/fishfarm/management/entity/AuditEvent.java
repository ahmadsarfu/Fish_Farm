package com.fishfarm.management.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
@Entity @Table(name="farm_audit_events") @Getter @Setter
public class AuditEvent extends FarmOwnedEntity {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
@Column(nullable=false,length=40) private String action;
@Column(nullable=false,length=40) private String entityType;
private Long entityId;
@Column(nullable=false,length=1000) private String detail;
}
