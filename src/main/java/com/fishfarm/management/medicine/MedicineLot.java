package com.fishfarm.management.medicine;
import com.fishfarm.management.entity.FarmOwnedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
@Entity @Table(name="medicine_lots") @Getter @Setter
public class MedicineLot extends FarmOwnedEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false,updatable=false) private MedicineProduct product;
 @Column(nullable=false,length=50,updatable=false) private String batchNumber;
 @Column(nullable=false,updatable=false) private LocalDate expiryDate;
 @Column(nullable=false,updatable=false) private LocalDate receivedOn;
 @Column(nullable=false,precision=12,scale=2,updatable=false) private BigDecimal unitCost;
}
