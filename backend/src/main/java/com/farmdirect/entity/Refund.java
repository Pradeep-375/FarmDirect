package com.farmdirect.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.*;
import lombok.*;
import com.farmdirect.enums.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name="refunds")
@Getter @Setter @NoArgsConstructor
public class Refund extends BaseEntity {
  @ManyToOne(optional=false) private Payment payment;
  private BigDecimal amount;
  private String status = "REQUESTED";
  private String razorpayRefundId;
  private String reason;
}
