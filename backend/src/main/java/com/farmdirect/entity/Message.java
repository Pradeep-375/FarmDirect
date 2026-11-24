package com.farmdirect.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.*;
import lombok.*;
import com.farmdirect.enums.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name="messages")
@Getter @Setter @NoArgsConstructor
public class Message extends BaseEntity {
  @ManyToOne(optional=false) private User sender;
  @ManyToOne(optional=false) private User recipient;
  @Column(length=2000) private String body;
}
