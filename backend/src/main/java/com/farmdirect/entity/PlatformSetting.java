package com.farmdirect.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.*;
import lombok.*;
import com.farmdirect.enums.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name="platform_settings")
@Getter @Setter @NoArgsConstructor
public class PlatformSetting extends BaseEntity {
  @Column(name="setting_key",unique=true,nullable=false) private String settingKey;
  @Column(name="setting_value") private String settingValue;
}
