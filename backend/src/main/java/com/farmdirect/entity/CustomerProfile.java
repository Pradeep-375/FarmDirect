package com.farmdirect.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.*;
import lombok.*;
import com.farmdirect.enums.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name="customer_profiles")
@Getter @Setter @NoArgsConstructor
public class CustomerProfile extends BaseEntity {
  @OneToOne(optional=false) @JoinColumn(unique=true) private User user;
  private String profileImageUrl;
}
