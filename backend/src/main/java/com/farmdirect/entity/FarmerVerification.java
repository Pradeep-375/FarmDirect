package com.farmdirect.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.*;
import lombok.*;
import com.farmdirect.enums.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name="farmer_verifications")
@Getter @Setter @NoArgsConstructor
public class FarmerVerification extends BaseEntity {
  @JsonIgnore @ManyToOne(optional=false) private FarmerProfile farmer;
  private String documentType;
  private String documentUrl;
  @Enumerated(EnumType.STRING) private VerificationStatus status = VerificationStatus.PENDING;
  private String reviewerNote;
}
