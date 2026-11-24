package com.farmdirect.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.*;
import lombok.*;
import com.farmdirect.enums.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name="payments")
@Getter @Setter @NoArgsConstructor
public class Payment extends BaseEntity {
  @ManyToOne(optional=false) private Order order;
  @Enumerated(EnumType.STRING) private PaymentMethod method;
  @Enumerated(EnumType.STRING) private PaymentStatus status = PaymentStatus.PENDING;
  @Column(unique=true) private String razorpayOrderId;
  @Column(unique=true) private String razorpayPaymentId;
  private BigDecimal amount;
}
