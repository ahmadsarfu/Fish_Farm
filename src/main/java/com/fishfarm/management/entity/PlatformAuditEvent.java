package com.fishfarm.management.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
@Entity @Table(name="platform_audit_events") @Getter @Setter
public class PlatformAuditEvent {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long actorId;
    @Column(nullable=false) private Long farmId;
    @Column(nullable=false,length=40) private String action;
    @Column(nullable=false) private Instant occurredAt;
}
