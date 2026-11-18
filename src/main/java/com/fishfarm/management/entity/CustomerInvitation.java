package com.fishfarm.management.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
@Entity @Table(name="customer_invitations") @Getter @Setter
public class CustomerInvitation {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false) @JoinColumn(name="farm_id",unique=true) private Farm farm;
    @ManyToOne(optional=false) @JoinColumn(name="account_id",unique=true) private AppAccount account;
    @Column(nullable=false,unique=true,length=64) private String tokenHash;
    @Column(nullable=false) private Instant expiresAt;
    private Instant acceptedAt;
    private Instant tokenConsumedAt;
    @Column(nullable=false,length=20,columnDefinition="varchar(20) default 'ACTIVATION'") private String purpose = "ACTIVATION";
    @Column(nullable=false) private int trialDays;
}
