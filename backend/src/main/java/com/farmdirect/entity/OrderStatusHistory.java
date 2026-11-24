package com.farmdirect.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.*;
import lombok.*;
import com.farmdirect.enums.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name="order_status_history")
@Getter @Setter @NoArgsConstructor
public class OrderStatusHistory extends BaseEntity {
  @ManyToOne(optional=false) private FarmerSubOrder subOrder;
  @Enumerated(EnumType.STRING) private OrderStatus status;
  private String updatedBy;
  @Column(length=500) private String description;
}
