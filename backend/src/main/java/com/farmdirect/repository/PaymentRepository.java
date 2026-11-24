package com.farmdirect.repository;

import com.farmdirect.entity.*;
import com.farmdirect.enums.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
  Optional<Payment> findByRazorpayOrderId(String id);
  Optional<Payment> findByRazorpayPaymentId(String id);
  Optional<Payment> findByOrderId(Long id);
}
