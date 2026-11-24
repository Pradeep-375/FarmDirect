package com.farmdirect.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.*;
import lombok.*;
import com.farmdirect.enums.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name="inventory")
@Getter @Setter @NoArgsConstructor
public class Inventory extends BaseEntity {
  @OneToOne(optional=false) @JoinColumn(unique=true) private Product product;
  @Column(nullable=false) private BigDecimal available = BigDecimal.ZERO;
  @Column(nullable=false) private BigDecimal reserved = BigDecimal.ZERO;
  private BigDecimal lowStockThreshold = BigDecimal.TEN;
  @Version private Long version;
}
