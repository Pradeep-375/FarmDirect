package com.farmdirect.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.*;
import lombok.*;
import com.farmdirect.enums.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name="addresses")
@Getter @Setter @NoArgsConstructor
public class Address extends BaseEntity {
  @ManyToOne(optional=false) private User user;
  private String label;
  @Column(nullable=false) private String line1;
  private String city;
  private String district;
  private String state;
  private String pincode;
  private Double latitude;
  private Double longitude;
  private boolean defaultAddress;
}
