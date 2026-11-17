package com.fishfarm.management.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="farms") @Getter @Setter
public class Farm {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=120) private String name;
    @Column(nullable=false) private boolean legacyClaimable = false;
    @Column(nullable=false) private boolean active = true;
}
