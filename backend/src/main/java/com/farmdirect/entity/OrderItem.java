package com.farmdirect.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.*;
import lombok.*;
import com.farmdirect.enums.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name="order_items")
@Getter @Setter @NoArgsConstructor
public class OrderItem extends BaseEntity {
  @ManyToOne(optional=false) private FarmerSubOrder subOrder;
  @ManyToOne(optional=false) private Product product;
  private String productName;
  private BigDecimal unitPrice;
  private BigDecimal quantity;
  private BigDecimal lineTotal;
}
