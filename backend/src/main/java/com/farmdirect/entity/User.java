package com.farmdirect.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.*;
import lombok.*;
import com.farmdirect.enums.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name="users")
@Getter @Setter @NoArgsConstructor
public class User extends BaseEntity {
  @Column(nullable=false) private String fullName;
  @Column(unique=true,nullable=false) private String email;
  @Column(unique=true,nullable=false) private String mobile;
  @JsonIgnore @Column(nullable=false) private String passwordHash;
  @Enumerated(EnumType.STRING) @Column(nullable=false) private Role role;
  @Enumerated(EnumType.STRING) @Column(nullable=false) private AccountStatus status = AccountStatus.ACTIVE;
  private int failedLogins;
  private Instant lockedUntil;
  private String state;
  private String district;
  private String city;
  private String address;
  private String pincode;
  private String preferredLanguage = "en";
}
