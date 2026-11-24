package com.farmdirect.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.*;
import lombok.*;
import com.farmdirect.enums.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name="farmer_profiles")
@Getter @Setter @NoArgsConstructor
public class FarmerProfile extends BaseEntity {
  @OneToOne(optional=false) @JoinColumn(unique=true) private User user;
  private String farmName;
  private String farmAddress;
  private String farmLocation;
  private Double latitude;
  private Double longitude;
  private String farmingCategory;
  @Column(length=2000) private String farmDetails;
  private String profileImageUrl;
  @Column(length=2000) private String farmImageUrls;
  @Enumerated(EnumType.STRING) private VerificationStatus verificationStatus = VerificationStatus.PENDING;
  private double ratingAverage;
  private int ratingCount;
}
