package com.farmdirect.repository;

import com.farmdirect.entity.*;
import com.farmdirect.enums.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select i from Inventory i where i.product.id=:id") Optional<Inventory> lockByProductId(Long id);
  Optional<Inventory> findByProductId(Long id);
}
