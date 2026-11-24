package com.farmdirect.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.*;
import lombok.*;
import com.farmdirect.enums.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name="orders")
@Getter @Setter @NoArgsConstructor
public class Order extends BaseEntity {
  @ManyToOne(optional=false) private User customer;
  @Column(unique=true,nullable=false) private String orderNumber;
  @ManyToOne(optional=false) private Address deliveryAddress;
  private BigDecimal subtotal;
  private BigDecimal deliveryCharge;
  private BigDecimal discount = BigDecimal.ZERO;
  private BigDecimal total;
  @Enumerated(EnumType.STRING) private PaymentMethod paymentMethod;
  @Enumerated(EnumType.STRING) private PaymentStatus paymentStatus = PaymentStatus.PENDING;
  @Enumerated(EnumType.STRING) private OrderStatus status = OrderStatus.ORDER_PLACED;
  private String couponCode;
  private boolean confirmed;
}
