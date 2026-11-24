package com.farmdirect.repository;

import com.farmdirect.entity.*;
import com.farmdirect.enums.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
  List<WishlistItem> findByWishlistId(Long id);
  Optional<WishlistItem> findByWishlistIdAndProductId(Long w, Long p);
}
