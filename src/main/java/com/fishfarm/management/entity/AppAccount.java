package com.fishfarm.management.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="app_accounts") @Getter @Setter
public class AppAccount {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,unique=true,length=120) private String username;
    @Column(nullable=false,length=120) private String displayName;
    @Column(nullable=false,length=100) private String passwordHash;
    @Column(unique=true,length=254) private String email;
    @Column(unique=true,length=16) private String mobile;
    @Column(nullable=false,columnDefinition="boolean default false") private boolean platformAdmin = false;
    @Column(nullable=false) private int sessionVersion = 0;
    @Column(nullable=false) private boolean active = true;
}
