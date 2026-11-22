package com.fishfarm.management.medicine;
import com.fishfarm.management.entity.FarmOwnedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
@Entity @Table(name="medicine_products",uniqueConstraints=@UniqueConstraint(columnNames={"farm_id","name","brand","pack_label"})) @Getter @Setter
public class MedicineProduct extends FarmOwnedEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=100) private String name;
 @Column(nullable=false,length=60) private String brand;
 @Column(nullable=false,length=40) private String packLabel;
 @Column(nullable=false,length=8) private String unit;
 @Column(nullable=false) private int minimumStock;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal sellingPrice;
 @Column(nullable=false) private boolean active=true;
 @Version private long version;
}
