package com.farmdirect.service;

import com.farmdirect.dto.OrderDtos.*;
import com.farmdirect.entity.*;
import com.farmdirect.enums.*;
import com.farmdirect.exception.ApiException;
import com.farmdirect.repository.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service
public class DeliveryService {
  public record Assignment(Long deliveryId, Long subOrderId, String orderNumber, OrderStatus status, String farmName, String pickupAddress, Double pickupLat, Double pickupLng,
    String customerName, String customerMobile, String dropAddress, Double dropLat, Double dropLng, PaymentMethod paymentMethod, java.math.BigDecimal codAmount, boolean trackingActive) {}

  private final DeliveryRepository deliveries; private final FarmerSubOrderRepository subs; private final DeliveryLocationRepository locations; private final UserRepository users;
  private final OrderStatusService status; private final OrderService orders; private final NotificationService notifier; private final SimpMessagingTemplate ws;

  public DeliveryService(DeliveryRepository deliveries, FarmerSubOrderRepository subs, DeliveryLocationRepository locations, UserRepository users, OrderStatusService status, OrderService orders, NotificationService notifier, SimpMessagingTemplate ws) {
    this.deliveries = deliveries; this.subs = subs; this.locations = locations; this.users = users; this.status = status; this.orders = orders; this.notifier = notifier; this.ws = ws;
  }

  @Transactional
  public void assign(Long subOrderId, Long partnerUserId) {
    FarmerSubOrder s = subs.findById(subOrderId).orElseThrow(() -> ApiException.notFound("Order not found"));
    User partner = users.findById(partnerUserId).filter(u -> u.getRole() == Role.DELIVERY_PARTNER && u.getStatus() == AccountStatus.ACTIVE).orElseThrow(() -> ApiException.badRequest("Invalid delivery partner"));
    if (s.getFulfilmentMode() != FulfilmentMode.PLATFORM_PARTNER) throw ApiException.badRequest("This order is not handled by delivery partners");
    if (!EnumSet.of(OrderStatus.FARMER_ACCEPTED, OrderStatus.PROCESSING, OrderStatus.READY_FOR_PICKUP).contains(s.getStatus())) throw ApiException.badRequest("Order is not ready for assignment");
    Delivery d = deliveries.findBySubOrderId(subOrderId).orElseGet(() -> { Delivery n = new Delivery(); n.setSubOrder(s); return n; });
    d.setPartner(partner); deliveries.save(d);
    notifier.notify(partner, "New delivery assigned", "Order " + s.getOrder().getOrderNumber() + " from " + s.getFarmer().getFarmName() + " is assigned to you.", "DELIVERY", true);
    notifier.notify(s.getFarmer().getUser(), "Delivery partner assigned", partner.getFullName() + " will pick up order " + s.getOrder().getOrderNumber() + ".", "DELIVERY", false);
    notifier.notify(s.getOrder().getCustomer(), "Delivery partner assigned", partner.getFullName() + " will deliver your order " + s.getOrder().getOrderNumber() + ".", "DELIVERY", false);
  }

  @Transactional(readOnly = true)
  public List<Assignment> assignments(Long partnerId) { return deliveries.findByPartnerIdOrderByIdDesc(partnerId).stream().map(this::toAssignment).toList(); }

  private Assignment toAssignment(Delivery d) {
    FarmerSubOrder s = d.getSubOrder(); Order o = s.getOrder(); Address a = o.getDeliveryAddress(); FarmerProfile f = s.getFarmer();
    java.math.BigDecimal cod = o.getPaymentMethod() == PaymentMethod.COD && o.getPaymentStatus() != PaymentStatus.COD_COLLECTED ? s.getSubtotal().add(s.getDeliveryCharge()) : null;
    return new Assignment(d.getId(), s.getId(), o.getOrderNumber(), s.getStatus(), f.getFarmName(), f.getFarmAddress(), f.getLatitude(), f.getLongitude(), o.getCustomer().getFullName(), o.getCustomer().getMobile(),
      a.getLine1() + ", " + a.getCity() + ", " + a.getDistrict() + ", " + a.getState() + " " + a.getPincode(), a.getLatitude(), a.getLongitude(), o.getPaymentMethod(), cod, d.isTrackingActive());
  }

  private Delivery mine(Long partnerId, Long deliveryId) {
    return deliveries.findById(deliveryId).filter(d -> d.getPartner() != null && d.getPartner().getId().equals(partnerId)).orElseThrow(() -> ApiException.notFound("Delivery not found"));
  }

  @Transactional
  public void step(Long partnerId, Long deliveryId, OrderStatus to) {
    Delivery d = mine(partnerId, deliveryId);
    if (to != OrderStatus.PICKED_UP && to != OrderStatus.OUT_FOR_DELIVERY && to != OrderStatus.DELIVERED) throw ApiException.forbidden("Not allowed");
    FarmerSubOrder s = d.getSubOrder();
    status.transition(s, to, d.getPartner().getFullName(), OrderService.defaultNote(to));
    if (to == OrderStatus.DELIVERED) { d.setCashCollected(s.getOrder().getPaymentMethod() == PaymentMethod.COD); orders.afterDelivered(s); }
  }

  /** Location is accepted only for an active delivery (picked up / out for delivery) from the assigned partner. */
  @Transactional
  public void location(Long partnerId, Long deliveryId, LocationRequest r) {
    Delivery d = mine(partnerId, deliveryId);
    if (!d.isTrackingActive()) throw ApiException.badRequest("Location sharing is only available during an active delivery");
    DeliveryLocation l = new DeliveryLocation(); l.setDelivery(d); l.setLatitude(r.latitude()); l.setLongitude(r.longitude()); l.setRecordedAt(Instant.now()); locations.save(l);
    d.setLastLatitude(r.latitude()); d.setLastLongitude(r.longitude()); d.setLastLocationAt(l.getRecordedAt());
    ws.convertAndSend("/topic/tracking/" + d.getSubOrder().getId(), Map.of("type", "LOCATION", "latitude", r.latitude(), "longitude", r.longitude(), "at", l.getRecordedAt().toString()));
  }
}
