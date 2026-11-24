package com.farmdirect.repository;

import com.farmdirect.entity.*;
import com.farmdirect.enums.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface ReviewRepository extends JpaRepository<Review, Long> {
  boolean existsByCustomerIdAndSubOrderIdAndProductId(Long c, Long s, Long p);
  List<Review> findByFarmerIdOrderByIdDesc(Long id);
  List<Review> findByProductIdOrderByIdDesc(Long id);
}
