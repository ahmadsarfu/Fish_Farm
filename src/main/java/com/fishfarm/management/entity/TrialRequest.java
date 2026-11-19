package com.fishfarm.management.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
@Entity @Table(name="trial_requests") @Getter @Setter
public class TrialRequest {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=120) private String ownerName;
    @Column(nullable=false,length=120) private String farmName;
    @Column(nullable=false,unique=true,length=16) private String mobile;
    @Column(length=254) private String email;
    @Column(nullable=false,length=20) private String status="PENDING";
    @Column(nullable=false) private Instant createdAt;
    private Instant reviewedAt;
    private Long reviewedBy;
    private Long farmId;
}
