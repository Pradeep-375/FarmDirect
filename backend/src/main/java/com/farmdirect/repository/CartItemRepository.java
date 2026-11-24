package com.farmdirect.repository;

import com.farmdirect.entity.*;
import com.farmdirect.enums.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
  List<CartItem> findByCartId(Long id);
  Optional<CartItem> findByCartIdAndProductId(Long c, Long p);
  @Modifying void deleteByCartId(Long id);
}
