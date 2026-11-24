package com.farmdirect.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.*;
import lombok.*;
import com.farmdirect.enums.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name="delivery_profiles")
@Getter @Setter @NoArgsConstructor
public class DeliveryProfile extends BaseEntity {
  @OneToOne(optional=false) @JoinColumn(unique=true) private User user;
  private String vehicleType;
  private String vehicleNumber;
  private boolean available = true;
}
