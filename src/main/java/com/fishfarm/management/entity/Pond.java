package com.fishfarm.management.entity;

import com.fishfarm.management.enums.PondStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "ponds", uniqueConstraints = @UniqueConstraint(name = "uq_ponds_farm_key", columnNames = {"farm_id", "pond_number"}))
@Data
public class Pond extends FarmOwnedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pond_number", nullable = false, unique = false)
    private String pondNumber;

    @Column(name = "pond_name", nullable = false)
    private String pondName;

    @Column(name = "area_acres")
    private BigDecimal areaAcres;

    private String description;

    @Enumerated(EnumType.STRING)
    private PondStatus status = PondStatus.ACTIVE;

    private Boolean deleted = false;
}