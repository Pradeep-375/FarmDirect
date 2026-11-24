package com.farmdirect.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.*;
import lombok.*;
import com.farmdirect.enums.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name="refresh_tokens")
@Getter @Setter @NoArgsConstructor
public class RefreshToken extends BaseEntity {
  @ManyToOne(optional=false) private User user;
  @Column(unique=true,nullable=false) private String tokenHash;
  private String familyId;
  private Instant expiresAt;
  private boolean revoked;
}
