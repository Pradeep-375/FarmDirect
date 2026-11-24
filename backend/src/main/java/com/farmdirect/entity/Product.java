package com.farmdirect.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.*;
import lombok.*;
import com.farmdirect.enums.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name="products")
@Getter @Setter @NoArgsConstructor
public class Product extends BaseEntity {
  @ManyToOne(optional=false) private FarmerProfile farmer;
  @ManyToOne(optional=false) private ProductCategory category;
  @Column(nullable=false) private String name;
  @Column(length=2000) private String description;
  @Column(nullable=false) private BigDecimal price;
  @Enumerated(EnumType.STRING) private Unit unit;
  private BigDecimal minOrderQuantity = BigDecimal.ONE;
  private LocalDate harvestDate;
  private boolean farmerProduced = true;
  private String sourceNote;
  private boolean enabled = true;
  private boolean deleted;
}
