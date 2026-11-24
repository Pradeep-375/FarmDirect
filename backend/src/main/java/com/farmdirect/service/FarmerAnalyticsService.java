package com.farmdirect.service;

import com.farmdirect.entity.*;
import com.farmdirect.enums.OrderStatus;
import com.farmdirect.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/** All numbers are computed from the database on every call. */
@Service
public class FarmerAnalyticsService {
  private final FarmerSubOrderRepository subs; private final ProductRepository products; private final InventoryRepository inventory; private final OrderItemRepository items;
  public FarmerAnalyticsService(FarmerSubOrderRepository subs, ProductRepository products, InventoryRepository inventory, OrderItemRepository items) { this.subs = subs; this.products = products; this.inventory = inventory; this.items = items; }

  @Transactional(readOnly = true)
  public Map<String, Object> analytics(FarmerProfile f) {
    List<FarmerSubOrder> all = subs.forFarmer(f.getId());
    Set<OrderStatus> inProgress = EnumSet.of(OrderStatus.FARMER_ACCEPTED, OrderStatus.PROCESSING, OrderStatus.READY_FOR_PICKUP, OrderStatus.PICKED_UP, OrderStatus.OUT_FOR_DELIVERY);
    List<FarmerSubOrder> delivered = all.stream().filter(s -> s.getStatus() == OrderStatus.DELIVERED).toList();
    Map<String, BigDecimal> monthly = new TreeMap<>();
    for (FarmerSubOrder s : delivered) monthly.merge(YearMonth.from(s.getUpdatedAt().atZone(ZoneOffset.UTC)).toString(), s.getSubtotal(), BigDecimal::add);
    Map<String, BigDecimal> qty = new HashMap<>();
    for (FarmerSubOrder s : delivered) for (OrderItem i : items.findBySubOrderId(s.getId())) qty.merge(i.getProductName(), i.getQuantity(), BigDecimal::add);
    List<Map<String, Object>> best = qty.entrySet().stream().sorted((a, b) -> b.getValue().compareTo(a.getValue())).limit(5).map(e -> Map.<String, Object>of("name", e.getKey(), "quantity", e.getValue())).toList();
    List<Product> mine = products.findByFarmerIdAndDeletedFalse(f.getId());
    List<Map<String, Object>> low = new ArrayList<>();
    for (Product p : mine) inventory.findByProductId(p.getId()).filter(i -> i.getAvailable().compareTo(i.getLowStockThreshold()) <= 0).ifPresent(i -> low.add(Map.of("id", p.getId(), "name", p.getName(), "available", i.getAvailable())));
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("totalProducts", mine.size()); m.put("totalOrders", all.size());
    m.put("pendingOrders", all.stream().filter(s -> s.getStatus() == OrderStatus.ORDER_PLACED).count());
    m.put("acceptedOrders", all.stream().filter(s -> inProgress.contains(s.getStatus())).count());
    m.put("completedOrders", delivered.size());
    m.put("cancelledOrders", all.stream().filter(s -> s.getStatus() == OrderStatus.CANCELLED || s.getStatus() == OrderStatus.FARMER_REJECTED).count());
    m.put("totalSales", delivered.stream().map(FarmerSubOrder::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add));
    m.put("monthlyEarnings", monthly); m.put("bestSelling", best); m.put("lowStock", low);
    m.put("ratingAverage", f.getRatingAverage()); m.put("ratingCount", f.getRatingCount());
    return m;
  }
}
