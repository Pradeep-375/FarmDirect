package com.farmdirect.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.*;
import lombok.*;
import com.farmdirect.enums.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name="notifications")
@Getter @Setter @NoArgsConstructor
public class Notification extends BaseEntity {
  @ManyToOne(optional=false) private User user;
  private String title;
  @Column(length=1000) private String message;
  private String type;
  @Column(name="is_read") private boolean read;
}
