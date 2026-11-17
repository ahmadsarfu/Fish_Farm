package com.fishfarm.management.entity;
import com.fishfarm.management.enums.DispatchStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "dispatches")
@Data
public class Dispatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String dispatchNumber;

    private String vehicleNumber;

    private String driverName;

    private LocalDate dispatchDate;

    @Enumerated(EnumType.STRING)
    private DispatchStatus status = DispatchStatus.CREATED;

    private Boolean deleted = false;

    private Long createdBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private Long updatedBy;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private String driverPhone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;
}
