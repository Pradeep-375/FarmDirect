package com.farmdirect.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.*;
import lombok.*;
import com.farmdirect.enums.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name="delivery_locations")
@Getter @Setter @NoArgsConstructor
public class DeliveryLocation extends BaseEntity {
  @ManyToOne(optional=false) private Delivery delivery;
  private Double latitude;
  private Double longitude;
  private Instant recordedAt;
}
