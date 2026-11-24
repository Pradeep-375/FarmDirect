package com.farmdirect.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.*;
import lombok.*;
import com.farmdirect.enums.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name="complaints")
@Getter @Setter @NoArgsConstructor
public class Complaint extends BaseEntity {
  @JsonIgnore @ManyToOne private User user;
  private String contactEmail;
  private Long orderId;
  private String subject;
  @Column(length=2000) private String description;
  private String status = "OPEN";
  @Column(length=1000) private String resolution;
  @JsonProperty("userName") public String userName() { return user == null ? contactEmail : user.getFullName(); }
}
