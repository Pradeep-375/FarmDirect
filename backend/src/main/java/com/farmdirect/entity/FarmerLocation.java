package com.farmdirect.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.*;
import lombok.*;
import com.farmdirect.enums.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name="farmer_locations")
@Getter @Setter @NoArgsConstructor
public class FarmerLocation extends BaseEntity {
  @ManyToOne(optional=false) private FarmerProfile farmer;
  private String state;
  private String district;
  private String city;
  private String pincode;
  private Double latitude;
  private Double longitude;
}
