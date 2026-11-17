package com.fishfarm.management.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="pond_assignments",uniqueConstraints=@UniqueConstraint(columnNames={"farm_id","account_id","pond_id"})) @Getter @Setter
public class PondAssignment extends FarmOwnedEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long accountId;
    @ManyToOne(optional=false) @JoinColumn(name="pond_id") private Pond pond;
}
