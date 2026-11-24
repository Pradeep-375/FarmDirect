package com.farmdirect.repository;

import com.farmdirect.entity.*;
import com.farmdirect.enums.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface FarmerVerificationRepository extends JpaRepository<FarmerVerification, Long> {
  List<FarmerVerification> findByFarmerId(Long id);
  List<FarmerVerification> findByStatus(VerificationStatus s);
}
