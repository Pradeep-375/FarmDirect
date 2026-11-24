package com.farmdirect.repository;

import com.farmdirect.entity.*;
import com.farmdirect.enums.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface FarmerSubOrderRepository extends JpaRepository<FarmerSubOrder, Long> {
  List<FarmerSubOrder> findByOrderId(Long id);
  @Query("select s from FarmerSubOrder s where s.farmer.id=:f and s.order.confirmed=true order by s.id desc") List<FarmerSubOrder> forFarmer(Long f);
  @Query("select s from FarmerSubOrder s where s.order.customer.id=:c order by s.id desc") List<FarmerSubOrder> forCustomer(Long c);
}
