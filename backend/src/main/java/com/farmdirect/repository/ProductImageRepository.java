package com.farmdirect.repository;

import com.farmdirect.entity.*;
import com.farmdirect.enums.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
  List<ProductImage> findByProductIdOrderByPosition(Long id);
  @Modifying void deleteByProductId(Long id);
}
