package com.farmdirect.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.*;
import lombok.*;
import com.farmdirect.enums.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name="deliveries")
@Getter @Setter @NoArgsConstructor
public class Delivery extends BaseEntity {
  @OneToOne(optional=false) @JoinColumn(unique=true) private FarmerSubOrder subOrder;
  @ManyToOne private User partner;
  private boolean trackingActive;
  private Double lastLatitude;
  private Double lastLongitude;
  private Instant lastLocationAt;
  private boolean cashCollected;
}
