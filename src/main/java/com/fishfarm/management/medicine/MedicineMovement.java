package com.fishfarm.management.medicine;
import com.fishfarm.management.entity.FarmOwnedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
@Entity @Table(name="medicine_movements",uniqueConstraints=@UniqueConstraint(columnNames={"farm_id","request_key"})) @Getter @Setter
public class MedicineMovement extends FarmOwnedEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false,updatable=false) private MedicineLot lot;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(updatable=false) private MedicineMovement reference;
 @Column(nullable=false,length=20,updatable=false) private String kind;
 @Column(nullable=false,updatable=false) private LocalDate eventDate;
 @Column(nullable=false,updatable=false) private int delta;
 @Column(nullable=false,precision=12,scale=2,updatable=false) private BigDecimal unitPrice;
 @Column(nullable=false,length=120,updatable=false) private String party;
 @Column(nullable=false,length=300,updatable=false) private String notes;
 @Column(nullable=false,length=36,updatable=false) private String requestKey;
 @Column(nullable=false,length=32,updatable=false) private byte[] payloadHash;
}
