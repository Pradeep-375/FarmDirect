package com.farmdirect.repository;

import com.farmdirect.entity.*;
import com.farmdirect.enums.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {
  Optional<CustomerProfile> findByUserId(Long userId);
}
