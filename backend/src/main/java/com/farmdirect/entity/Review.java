package com.farmdirect.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.*;
import lombok.*;
import com.farmdirect.enums.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name="reviews")
@Getter @Setter @NoArgsConstructor
public class Review extends BaseEntity {
  @ManyToOne(optional=false) private User customer;
  @ManyToOne(optional=false) private FarmerProfile farmer;
  @ManyToOne private Product product;
  private int rating;
  @Column(length=1000) private String comment;
  private Long subOrderId;
}
