package com.farmdirect.repository;

import com.farmdirect.entity.*;
import com.farmdirect.enums.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {
  Optional<ProductCategory> findByNameIgnoreCase(String n);
}
