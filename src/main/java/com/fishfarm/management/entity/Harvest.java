package com.fishfarm.management.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "harvests")
@Data
public class Harvest extends FarmOwnedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String harvestNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "pond_id", nullable = false)
    private Pond pond;

    private LocalDate harvestDate;

    private String remarks;

    private Boolean deleted = false;

    @Column(updatable = false)
    private Long createdBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private Long updatedBy;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "harvest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HarvestItem> items;
}