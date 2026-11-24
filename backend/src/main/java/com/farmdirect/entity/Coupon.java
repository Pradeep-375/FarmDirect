package com.farmdirect.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.*;
import lombok.*;
import com.farmdirect.enums.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name="coupons")
@Getter @Setter @NoArgsConstructor
public class Coupon extends BaseEntity {
  @Column(unique=true,nullable=false) private String code;
  private int percentOff;
  private BigDecimal minOrder = BigDecimal.ZERO;
  private boolean active = true;
  private Instant validUntil;
}
