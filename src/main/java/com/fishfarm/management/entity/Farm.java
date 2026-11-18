package com.fishfarm.management.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="farms") @Getter @Setter
public class Farm {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=120) private String name;
    @Column(nullable=false,length=80,columnDefinition="varchar(80) default 'Legacy'") private String planName = "Legacy";
    @Column(nullable=false,length=12,columnDefinition="varchar(12) default 'ACTIVE'") private String subscriptionStatus = "ACTIVE";
    private java.time.LocalDate subscriptionEndsOn;
    @Column(nullable=false,columnDefinition="boolean default false") private boolean managedSubscription = false;
    private Integer pondLimit;
    private Integer userLimit;
    @Column(nullable=false) private boolean legacyClaimable = false;
    @Column(nullable=false) private boolean active = true;
}
