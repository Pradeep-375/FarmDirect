package com.farmdirect.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.*;
import lombok.*;
import com.farmdirect.enums.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name="wishlist_items")
@Getter @Setter @NoArgsConstructor
public class WishlistItem extends BaseEntity {
  @ManyToOne(optional=false) private Wishlist wishlist;
  @ManyToOne(optional=false) private Product product;
}
