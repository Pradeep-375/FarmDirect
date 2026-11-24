package com.farmdirect.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.*;
import lombok.*;
import com.farmdirect.enums.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name="farmer_sub_orders")
@Getter @Setter @NoArgsConstructor
public class FarmerSubOrder extends BaseEntity {
  @ManyToOne(optional=false) private Order order;
  @ManyToOne(optional=false) private FarmerProfile farmer;
  @Enumerated(EnumType.STRING) private OrderStatus status = OrderStatus.ORDER_PLACED;
  private BigDecimal subtotal;
  private BigDecimal deliveryCharge;
  private String rejectionReason;
  @Enumerated(EnumType.STRING) private FulfilmentMode fulfilmentMode = FulfilmentMode.PLATFORM_PARTNER;
}
