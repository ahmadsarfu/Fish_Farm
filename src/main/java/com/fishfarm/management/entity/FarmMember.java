package com.fishfarm.management.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="farm_members",uniqueConstraints=@UniqueConstraint(columnNames={"farm_id","account_id"})) @Getter @Setter
public class FarmMember {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false,fetch=FetchType.EAGER) @JoinColumn(name="farm_id") private Farm farm;
    @ManyToOne(optional=false,fetch=FetchType.EAGER) @JoinColumn(name="account_id") private AppAccount account;
    @Column(nullable=false,length=15) private String role;
    @Column(nullable=false) private boolean active = true;
}
