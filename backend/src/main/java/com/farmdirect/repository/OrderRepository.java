package com.farmdirect.repository;

import com.farmdirect.entity.*;
import com.farmdirect.enums.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface OrderRepository extends JpaRepository<Order, Long> {
  List<Order> findByCustomerIdOrderByIdDesc(Long id);
  @Query("select o from Order o where o.confirmed=false and o.paymentMethod<>com.farmdirect.enums.PaymentMethod.COD and o.status<>com.farmdirect.enums.OrderStatus.CANCELLED and o.createdAt<:t") List<Order> staleUnpaid(java.time.Instant t);
}
