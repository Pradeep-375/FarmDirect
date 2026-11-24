package com.farmdirect.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.*;
import lombok.*;
import com.farmdirect.enums.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name="product_images")
@Getter @Setter @NoArgsConstructor
public class ProductImage extends BaseEntity {
  @ManyToOne(optional=false) private Product product;
  @Column(nullable=false) private String url;
  private int position;
}
