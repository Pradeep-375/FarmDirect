package com.farmdirect.service;

import com.farmdirect.entity.*;
import com.farmdirect.enums.*;
import com.farmdirect.exception.ApiException;
import com.farmdirect.repository.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Single entry point for status changes: validates the transition, records permanent history, pushes live updates and notifies. */
@Service
public class OrderStatusService {
  public static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = new EnumMap<>(OrderStatus.class);
  static {
    ALLOWED.put(OrderStatus.ORDER_PLACED, EnumSet.of(OrderStatus.FARMER_ACCEPTED, OrderStatus.FARMER_REJECTED, OrderStatus.CANCELLED));
    ALLOWED.put(OrderStatus.FARMER_ACCEPTED, EnumSet.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED));
    ALLOWED.put(OrderStatus.PROCESSING, EnumSet.of(OrderStatus.READY_FOR_PICKUP));
    ALLOWED.put(OrderStatus.READY_FOR_PICKUP, EnumSet.of(OrderStatus.PICKED_UP, OrderStatus.DELIVERED));   // DELIVERED only for customer pickup
    ALLOWED.put(OrderStatus.PICKED_UP, EnumSet.of(OrderStatus.OUT_FOR_DELIVERY));
    ALLOWED.put(OrderStatus.OUT_FOR_DELIVERY, EnumSet.of(OrderStatus.DELIVERED));
    ALLOWED.put(OrderStatus.DELIVERED, EnumSet.of(OrderStatus.RETURN_REQUESTED));
    ALLOWED.put(OrderStatus.RETURN_REQUESTED, EnumSet.of(OrderStatus.REFUNDED));
    ALLOWED.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
    ALLOWED.put(OrderStatus.FARMER_REJECTED, EnumSet.noneOf(OrderStatus.class));
    ALLOWED.put(OrderStatus.REFUNDED, EnumSet.noneOf(OrderStatus.class));
  }
  public static boolean canMove(OrderStatus from, OrderStatus to) { return ALLOWED.getOrDefault(from, Set.of()).contains(to); }

  private final FarmerSubOrderRepository subs; private final OrderStatusHistoryRepository history; private final OrderRepository orders;
  private final DeliveryRepository deliveries; private final NotificationService notifier; private final SimpMessagingTemplate ws;
  private final OrderItemRepository items; private final InventoryRepository inventory;

  public OrderStatusService(FarmerSubOrderRepository subs, OrderStatusHistoryRepository history, OrderRepository orders, DeliveryRepository deliveries,
                            NotificationService notifier, SimpMessagingTemplate ws, OrderItemRepository items, InventoryRepository inventory) {
    this.items = items; this.inventory = inventory;
    this.subs = subs; this.history = history; this.orders = orders; this.deliveries = deliveries; this.notifier = notifier; this.ws = ws;
  }

  @Transactional
  public void transition(FarmerSubOrder s, OrderStatus to, String updatedBy, String description) {
    if (!canMove(s.getStatus(), to)) throw ApiException.badRequest("Cannot change order from " + s.getStatus() + " to " + to);
    record(s, to, updatedBy, description);
  }

  /** Records the initial ORDER_PLACED entry (no transition check). */
  @Transactional
  public void record(FarmerSubOrder s, OrderStatus to, String updatedBy, String description) {
    s.setStatus(to); subs.save(s);
    OrderStatusHistory h = new OrderStatusHistory(); h.setSubOrder(s); h.setStatus(to); h.setUpdatedBy(updatedBy); h.setDescription(description); history.save(h);
    Delivery d = deliveries.findBySubOrderId(s.getId()).orElse(null);
    if (d != null) d.setTrackingActive(to == OrderStatus.PICKED_UP || to == OrderStatus.OUT_FOR_DELIVERY);
    if (to == OrderStatus.DELIVERED) adjustStock(s, true);
    if (to == OrderStatus.CANCELLED || to == OrderStatus.FARMER_REJECTED) adjustStock(s, false);
    rollUp(s.getOrder());
    ws.convertAndSend("/topic/tracking/" + s.getId(), Map.of("type", "STATUS", "status", to.name(), "description", String.valueOf(description), "at", java.time.Instant.now().toString()));
    if (to != OrderStatus.ORDER_PLACED) {
      boolean sms = to == OrderStatus.OUT_FOR_DELIVERY || to == OrderStatus.DELIVERED || to == OrderStatus.FARMER_REJECTED || to == OrderStatus.CANCELLED;
      notifier.notify(s.getOrder().getCustomer(), "Order " + s.getOrder().getOrderNumber() + ": " + pretty(to), description, "ORDER", sms);
    }
  }

  /** commit=true: sold units leave 'reserved'. commit=false: reserved units return to 'available'. Locks in product-id order to avoid deadlocks. */
  private void adjustStock(FarmerSubOrder s, boolean commit) {
    items.findBySubOrderId(s.getId()).stream().sorted(Comparator.comparing(i -> i.getProduct().getId())).forEach(i -> {
      Inventory inv = inventory.lockByProductId(i.getProduct().getId()).orElseThrow();
      inv.setReserved(inv.getReserved().subtract(i.getQuantity()).max(java.math.BigDecimal.ZERO));
      if (!commit) inv.setAvailable(inv.getAvailable().add(i.getQuantity()));
    });
  }

  /** Parent order status mirrors its fulfilment groups: all terminal -> summary, otherwise the least-advanced active group. */
  private void rollUp(Order o) {
    List<FarmerSubOrder> all = subs.findByOrderId(o.getId());
    List<FarmerSubOrder> active = all.stream().filter(x -> x.getStatus() != OrderStatus.CANCELLED && x.getStatus() != OrderStatus.FARMER_REJECTED).toList();
    if (active.isEmpty()) { o.setStatus(all.stream().anyMatch(x -> x.getStatus() == OrderStatus.CANCELLED) ? OrderStatus.CANCELLED : OrderStatus.FARMER_REJECTED); return; }
    o.setStatus(active.stream().map(FarmerSubOrder::getStatus).min(Comparator.comparingInt(Enum::ordinal)).get());
    orders.save(o);
  }
  static String pretty(OrderStatus s) { return s.name().replace('_', ' ').toLowerCase(); }
}
