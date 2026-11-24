package com.farmdirect.repository;

import com.farmdirect.entity.*;
import com.farmdirect.enums.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
  Optional<Delivery> findBySubOrderId(Long id);
  List<Delivery> findByPartnerIdOrderByIdDesc(Long id);
}
