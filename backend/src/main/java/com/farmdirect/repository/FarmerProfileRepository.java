package com.farmdirect.repository;

import com.farmdirect.entity.*;
import com.farmdirect.enums.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface FarmerProfileRepository extends JpaRepository<FarmerProfile, Long> {
  Optional<FarmerProfile> findByUserId(Long userId);
}
