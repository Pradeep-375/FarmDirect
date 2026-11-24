package com.farmdirect.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.*;
import lombok.*;
import com.farmdirect.enums.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name="cart_items")
@Getter @Setter @NoArgsConstructor
public class CartItem extends BaseEntity {
  @ManyToOne(optional=false) private Cart cart;
  @ManyToOne(optional=false) private Product product;
  @Column(nullable=false) private BigDecimal quantity;
}
