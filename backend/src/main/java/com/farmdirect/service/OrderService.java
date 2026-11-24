package com.farmdirect.service;

import com.farmdirect.dto.OrderDtos.*;
import com.farmdirect.entity.*;
import com.farmdirect.enums.*;
import com.farmdirect.exception.ApiException;
import com.farmdirect.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderService {
  private final CartRepository carts; private final CartItemRepository cartItems; private final InventoryRepository inventory; private final OrderRepository orders;
  private final FarmerSubOrderRepository subs; private final OrderItemRepository items; private final OrderStatusHistoryRepository history; private final PaymentRepository payments;
  private final AddressRepository addresses; private final CouponRepository coupons; private final DeliveryRepository deliveries; private final RefundRepository refunds;
  private final PlatformSettingRepository settings; private final UserRepository users; private final OrderStatusService status; private final NotificationService notifier;
  private final RazorpayService razorpay; private final ReviewRepository reviews; private final FarmerProfileRepository farmers; private final DeliveryProfileRepository deliveryProfiles;
  @Value("${app.delivery.base-charge}") BigDecimal baseCharge;
  @Value("${app.delivery.free-above}") BigDecimal freeAbove;
  private final SecureRandom random = new SecureRandom();

  public OrderService(CartRepository carts, CartItemRepository cartItems, InventoryRepository inventory, OrderRepository orders, FarmerSubOrderRepository subs, OrderItemRepository items,
      OrderStatusHistoryRepository history, PaymentRepository payments, AddressRepository addresses, CouponRepository coupons, DeliveryRepository deliveries, RefundRepository refunds,
      PlatformSettingRepository settings, UserRepository users, OrderStatusService status, NotificationService notifier, RazorpayService razorpay, ReviewRepository reviews,
      FarmerProfileRepository farmers, DeliveryProfileRepository deliveryProfiles) {
    this.carts = carts; this.cartItems = cartItems; this.inventory = inventory; this.orders = orders; this.subs = subs; this.items = items; this.history = history; this.payments = payments;
    this.addresses = addresses; this.coupons = coupons; this.deliveries = deliveries; this.refunds = refunds; this.settings = settings; this.users = users; this.status = status;
    this.notifier = notifier; this.razorpay = razorpay; this.reviews = reviews; this.farmers = farmers; this.deliveryProfiles = deliveryProfiles;
  }

  private BigDecimal setting(String key, BigDecimal def) { return settings.findBySettingKey(key).map(s -> new BigDecimal(s.getSettingValue())).orElse(def); }

  // ------------------------------------------------------------------ checkout
  @Transactional
  public CheckoutResponse checkout(Long userId, CheckoutRequest r) {
    User customer = users.findById(userId).orElseThrow();
    Address address = addresses.findById(r.addressId()).filter(a -> a.getUser().getId().equals(userId)).orElseThrow(() -> ApiException.badRequest("Select a valid delivery address"));
    Cart cart = carts.findByUserId(userId).orElseThrow(() -> ApiException.badRequest("Your cart is empty"));
    List<CartItem> lines = cartItems.findByCartId(cart.getId()).stream().sorted(Comparator.comparing(i -> i.getProduct().getId())).toList();
    if (lines.isEmpty()) throw ApiException.badRequest("Your cart is empty");
    if (r.paymentMethod() == PaymentMethod.COD && setting("cod.enabled", BigDecimal.ONE).signum() == 0) throw ApiException.badRequest("Cash on delivery is not available right now");
    FulfilmentMode mode = r.fulfilmentMode() == null ? FulfilmentMode.PLATFORM_PARTNER : r.fulfilmentMode();

    // 1) validate + reserve stock under pessimistic locks, ordered by product id (no deadlocks, no overselling)
    Map<Long, List<CartItem>> byFarmer = new LinkedHashMap<>();
    for (CartItem ci : lines) {
      Product p = ci.getProduct();
      if (p.isDeleted() || !p.isEnabled() || p.getFarmer().getUser().getStatus() != AccountStatus.ACTIVE) throw ApiException.conflict(p.getName() + " is no longer available");
      if (ci.getQuantity().compareTo(p.getMinOrderQuantity()) < 0) throw ApiException.badRequest("Minimum order for " + p.getName() + " is " + p.getMinOrderQuantity());
      Inventory inv = inventory.lockByProductId(p.getId()).orElseThrow();
      if (inv.getAvailable().compareTo(ci.getQuantity()) < 0) throw ApiException.conflict("Only " + inv.getAvailable().stripTrailingZeros().toPlainString() + " of " + p.getName() + " left");
      inv.setAvailable(inv.getAvailable().subtract(ci.getQuantity())); inv.setReserved(inv.getReserved().add(ci.getQuantity()));
      byFarmer.computeIfAbsent(p.getFarmer().getId(), k -> new ArrayList<>()).add(ci);
    }

    // 2) totals
    Order order = new Order();
    order.setCustomer(customer); order.setDeliveryAddress(address); order.setPaymentMethod(r.paymentMethod()); order.setOrderNumber(orderNumber());
    BigDecimal subtotal = BigDecimal.ZERO, delivery = BigDecimal.ZERO;
    BigDecimal base = setting("delivery.base", baseCharge), free = setting("delivery.freeAbove", freeAbove);
    Map<Long, BigDecimal[]> groupTotals = new LinkedHashMap<>();
    for (var e : byFarmer.entrySet()) {
      BigDecimal gs = e.getValue().stream().map(ci -> ci.getProduct().getPrice().multiply(ci.getQuantity())).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
      BigDecimal gd = (mode == FulfilmentMode.CUSTOMER_PICKUP || gs.compareTo(free) >= 0) ? BigDecimal.ZERO : base;
      groupTotals.put(e.getKey(), new BigDecimal[]{gs, gd}); subtotal = subtotal.add(gs); delivery = delivery.add(gd);
    }
    BigDecimal discount = BigDecimal.ZERO;
    if (r.couponCode() != null && !r.couponCode().isBlank()) {
      Coupon c = coupons.findByCodeIgnoreCase(r.couponCode().trim()).filter(x -> x.isActive() && (x.getValidUntil() == null || x.getValidUntil().isAfter(Instant.now())))
        .orElseThrow(() -> ApiException.badRequest("Invalid or expired coupon"));
      if (subtotal.compareTo(c.getMinOrder()) < 0) throw ApiException.badRequest("Coupon needs a minimum order of " + c.getMinOrder());
      discount = subtotal.multiply(BigDecimal.valueOf(c.getPercentOff())).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP); order.setCouponCode(c.getCode());
    }
    order.setSubtotal(subtotal); order.setDeliveryCharge(delivery); order.setDiscount(discount); order.setTotal(subtotal.add(delivery).subtract(discount).max(BigDecimal.ONE));
    boolean cod = r.paymentMethod() == PaymentMethod.COD;
    order.setPaymentStatus(cod ? PaymentStatus.COD_PENDING : PaymentStatus.PENDING); order.setConfirmed(cod);
    order = orders.save(order);

    // 3) one fulfilment group (sub-order) per farmer
    for (var e : byFarmer.entrySet()) {
      FarmerSubOrder s = new FarmerSubOrder();
      s.setOrder(order); s.setFarmer(e.getValue().get(0).getProduct().getFarmer()); s.setSubtotal(groupTotals.get(e.getKey())[0]); s.setDeliveryCharge(groupTotals.get(e.getKey())[1]); s.setFulfilmentMode(mode);
      s = subs.save(s);
      for (CartItem ci : e.getValue()) {
        OrderItem oi = new OrderItem(); Product p = ci.getProduct();
        oi.setSubOrder(s); oi.setProduct(p); oi.setProductName(p.getName()); oi.setUnitPrice(p.getPrice()); oi.setQuantity(ci.getQuantity()); oi.setLineTotal(p.getPrice().multiply(ci.getQuantity()).setScale(2, RoundingMode.HALF_UP));
        items.save(oi);
      }
      status.record(s, OrderStatus.ORDER_PLACED, customer.getFullName(), cod ? "Order placed. Payment on delivery." : "Order created. Waiting for payment confirmation.");
    }
    cartItems.deleteByCartId(cart.getId());

    // 4) online payment: create the gateway order on the backend
    long paise = order.getTotal().movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    if (cod) { notifyFarmersOfNewOrder(order); return new CheckoutResponse(order.getId(), order.getOrderNumber(), order.getTotal(), r.paymentMethod(), null, null, paise); }
    String rzpOrder = razorpay.createOrder(paise, order.getOrderNumber());   // throws -> whole transaction (and stock reservation) rolls back
    Payment pay = new Payment(); pay.setOrder(order); pay.setMethod(r.paymentMethod()); pay.setAmount(order.getTotal()); pay.setRazorpayOrderId(rzpOrder); payments.save(pay);
    return new CheckoutResponse(order.getId(), order.getOrderNumber(), order.getTotal(), r.paymentMethod(), razorpay.keyId(), rzpOrder, paise);
  }

  private String orderNumber() {
    String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; StringBuilder b = new StringBuilder("FD" + LocalDate.now().toString().replace("-", "") + "-");
    for (int i = 0; i < 6; i++) b.append(alphabet.charAt(random.nextInt(alphabet.length())));
    return b.toString();
  }

  private void notifyFarmersOfNewOrder(Order o) {
    for (FarmerSubOrder s : subs.findByOrderId(o.getId()))
      notifier.notify(s.getFarmer().getUser(), "New order " + o.getOrderNumber(), "You have a new order worth INR " + s.getSubtotal() + ". Please accept or reject it.", "ORDER", true);
    notifier.notify(o.getCustomer(), "Order placed", "Order " + o.getOrderNumber() + " has been placed.", "ORDER", false);
  }

  // ------------------------------------------------------------------ payments
  /** Idempotent: safe to call from both the checkout callback and the webhook. */
  @Transactional
  public void confirmPayment(Payment p, String razorpayPaymentId) {
    if (p.getStatus() == PaymentStatus.PAID) return;
    payments.findByRazorpayPaymentId(razorpayPaymentId).filter(x -> !x.getId().equals(p.getId())).ifPresent(x -> { throw ApiException.conflict("Duplicate payment"); });
    p.setStatus(PaymentStatus.PAID); p.setRazorpayPaymentId(razorpayPaymentId);
    Order o = p.getOrder();
    o.setPaymentStatus(PaymentStatus.PAID);
    if (!o.isConfirmed() && o.getStatus() != OrderStatus.CANCELLED) { o.setConfirmed(true); notifyFarmersOfNewOrder(o); }
    notifier.notify(o.getCustomer(), "Payment received", "Payment for order " + o.getOrderNumber() + " was confirmed.", "PAYMENT", false);
  }

  @Transactional
  public void paymentFailed(Payment p) {
    if (p.getStatus() == PaymentStatus.PENDING) { p.setStatus(PaymentStatus.FAILED); p.getOrder().setPaymentStatus(PaymentStatus.FAILED); }
  }

  /** Unpaid online orders older than 30 minutes are cancelled and their stock is released. */
  @Scheduled(fixedDelay = 300_000)
  @Transactional
  public void expireUnpaid() {
    for (Order o : orders.staleUnpaid(Instant.now().minusSeconds(1800)))
      for (FarmerSubOrder s : subs.findByOrderId(o.getId())) if (s.getStatus() == OrderStatus.ORDER_PLACED) status.transition(s, OrderStatus.CANCELLED, "system", "Payment not completed in time.");
  }

  // ------------------------------------------------------------------ customer actions
  @Transactional
  public void customerCancel(Long userId, Long subId) {
    FarmerSubOrder s = subs.findById(subId).filter(x -> x.getOrder().getCustomer().getId().equals(userId)).orElseThrow(() -> ApiException.notFound("Order not found"));
    if (s.getStatus() != OrderStatus.ORDER_PLACED && s.getStatus() != OrderStatus.FARMER_ACCEPTED) throw ApiException.badRequest("This order can no longer be cancelled");
    status.transition(s, OrderStatus.CANCELLED, s.getOrder().getCustomer().getFullName(), "Cancelled by customer.");
    refundIfPaid(s, "Customer cancelled");
    notifier.notify(s.getFarmer().getUser(), "Order cancelled", "Order " + s.getOrder().getOrderNumber() + " was cancelled by the customer.", "ORDER", false);
  }

  private void refundIfPaid(FarmerSubOrder s, String reason) {
    Payment p = payments.findByOrderId(s.getOrder().getId()).orElse(null);
    if (p == null || p.getStatus() != PaymentStatus.PAID || p.getRazorpayPaymentId() == null) return;
    BigDecimal amount = s.getSubtotal().add(s.getDeliveryCharge());
    Refund rf = new Refund(); rf.setPayment(p); rf.setAmount(amount); rf.setReason(reason);
    try { rf.setRazorpayRefundId(razorpay.refund(p.getRazorpayPaymentId(), amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact())); rf.setStatus("PROCESSING"); }
    catch (ApiException e) { rf.setStatus("FAILED_RETRY_BY_ADMIN"); }   // admin can retry from the Refunds screen
    refunds.save(rf);
    p.setStatus(PaymentStatus.REFUND_PENDING);
    notifier.notify(s.getOrder().getCustomer(), "Refund initiated", "A refund of INR " + amount + " for order " + s.getOrder().getOrderNumber() + " has been initiated.", "PAYMENT", true);
  }

  @Transactional
  public void requestReturn(Long userId, Long subId, String reason) {
    FarmerSubOrder s = subs.findById(subId).filter(x -> x.getOrder().getCustomer().getId().equals(userId)).orElseThrow(() -> ApiException.notFound("Order not found"));
    status.transition(s, OrderStatus.RETURN_REQUESTED, s.getOrder().getCustomer().getFullName(), reason == null ? "Return requested." : reason);
  }

  @Transactional
  public void review(Long userId, ReviewRequest r) {
    FarmerSubOrder s = subs.findById(r.subOrderId()).filter(x -> x.getOrder().getCustomer().getId().equals(userId)).orElseThrow(() -> ApiException.notFound("Order not found"));
    if (s.getStatus() != OrderStatus.DELIVERED) throw ApiException.badRequest("You can review an order after it is delivered");
    if (r.productId() != null && items.findBySubOrderId(s.getId()).stream().noneMatch(i -> i.getProduct().getId().equals(r.productId()))) throw ApiException.badRequest("Product is not part of this order");
    if (reviews.existsByCustomerIdAndSubOrderIdAndProductId(userId, s.getId(), r.productId())) throw ApiException.conflict("You already reviewed this");
    Review v = new Review(); v.setCustomer(s.getOrder().getCustomer()); v.setFarmer(s.getFarmer()); v.setSubOrderId(s.getId()); v.setRating(r.rating()); v.setComment(r.comment());
    if (r.productId() != null) { Product pr = items.findBySubOrderId(s.getId()).stream().filter(i -> i.getProduct().getId().equals(r.productId())).findFirst().get().getProduct(); v.setProduct(pr); }
    reviews.save(v);
    FarmerProfile f = s.getFarmer(); double total = f.getRatingAverage() * f.getRatingCount() + r.rating();
    f.setRatingCount(f.getRatingCount() + 1); f.setRatingAverage(Math.round(total / f.getRatingCount() * 10.0) / 10.0);
  }

  // ------------------------------------------------------------------ farmer actions
  private FarmerSubOrder farmerSub(Long farmerUserId, Long subId) {
    FarmerSubOrder s = subs.findById(subId).orElseThrow(() -> ApiException.notFound("Order not found"));
    if (!s.getFarmer().getUser().getId().equals(farmerUserId) || !s.getOrder().isConfirmed()) throw ApiException.notFound("Order not found");   // other farmers' orders look non-existent
    return s;
  }

  @Transactional
  public void farmerAccept(Long farmerUserId, Long subId, FulfilmentMode mode) {
    FarmerSubOrder s = farmerSub(farmerUserId, subId);
    if (mode != null && s.getFulfilmentMode() != FulfilmentMode.CUSTOMER_PICKUP && mode != FulfilmentMode.CUSTOMER_PICKUP) s.setFulfilmentMode(mode);
    status.transition(s, OrderStatus.FARMER_ACCEPTED, s.getFarmer().getUser().getFullName(), "Farmer accepted your order.");
    if (s.getFulfilmentMode() != FulfilmentMode.CUSTOMER_PICKUP && deliveries.findBySubOrderId(s.getId()).isEmpty()) { Delivery d = new Delivery(); d.setSubOrder(s); deliveries.save(d); }
  }

  @Transactional
  public void farmerReject(Long farmerUserId, Long subId, String reason) {
    FarmerSubOrder s = farmerSub(farmerUserId, subId);
    s.setRejectionReason(reason);
    status.transition(s, OrderStatus.FARMER_REJECTED, s.getFarmer().getUser().getFullName(), "Farmer could not fulfil this order: " + reason);
    refundIfPaid(s, "Rejected by farmer");
  }

  @Transactional
  public void farmerStatus(Long farmerUserId, Long subId, OrderStatus to, String note) {
    FarmerSubOrder s = farmerSub(farmerUserId, subId); FulfilmentMode m = s.getFulfilmentMode();
    boolean farmerOwnsDelivery = m == FulfilmentMode.FARMER_DELIVERY;
    boolean ok = switch (to) {
      case PROCESSING, READY_FOR_PICKUP -> true;
      case PICKED_UP, OUT_FOR_DELIVERY -> farmerOwnsDelivery;
      case DELIVERED -> farmerOwnsDelivery || m == FulfilmentMode.CUSTOMER_PICKUP;
      default -> false;
    };
    if (!ok) throw ApiException.forbidden("You cannot set this status for this order");
    status.transition(s, to, s.getFarmer().getUser().getFullName(), note != null && !note.isBlank() ? note : defaultNote(to));
    if (to == OrderStatus.DELIVERED) afterDelivered(s);
  }

  /** Shared by farmer-managed delivery, pickup and delivery partners. */
  @Transactional
  public void afterDelivered(FarmerSubOrder s) {
    Order o = s.getOrder();
    if (o.getPaymentMethod() == PaymentMethod.COD && subs.findByOrderId(o.getId()).stream().filter(x -> x.getStatus() != OrderStatus.CANCELLED && x.getStatus() != OrderStatus.FARMER_REJECTED).allMatch(x -> x.getStatus() == OrderStatus.DELIVERED))
      o.setPaymentStatus(PaymentStatus.COD_COLLECTED);
  }

  static String defaultNote(OrderStatus s) {
    return switch (s) {
      case PROCESSING -> "Your farmer is preparing your products.";
      case READY_FOR_PICKUP -> "Your order is packed and ready for pickup.";
      case PICKED_UP -> "Order picked up from the farm.";
      case OUT_FOR_DELIVERY -> "Your order is out for delivery.";
      case DELIVERED -> "Order delivered.";
      default -> s.name();
    };
  }

  // ------------------------------------------------------------------ queries / mapping
  @Transactional(readOnly = true)
  public List<OrderDto> customerOrders(Long userId) {
    return orders.findByCustomerIdOrderByIdDesc(userId).stream().map(o -> toOrderDto(o, false)).toList();
  }
  @Transactional(readOnly = true)
  public OrderDto customerOrder(Long userId, Long orderId) {
    Order o = orders.findById(orderId).filter(x -> x.getCustomer().getId().equals(userId)).orElseThrow(() -> ApiException.notFound("Order not found"));
    return toOrderDto(o, false);
  }
  @Transactional(readOnly = true)
  public List<SubOrderDto> farmerOrders(Long farmerUserId, OrderStatus filter) {
    FarmerProfile f = farmers.findByUserId(farmerUserId).orElseThrow();
    return subs.forFarmer(f.getId()).stream().filter(s -> filter == null || s.getStatus() == filter).map(s -> toSubDto(s, true)).toList();
  }

  public OrderDto toOrderDto(Order o, boolean forStaff) {
    return new OrderDto(o.getId(), o.getOrderNumber(), o.getSubtotal(), o.getDeliveryCharge(), o.getDiscount(), o.getTotal(), o.getPaymentMethod(), o.getPaymentStatus(), o.getStatus(), o.getCreatedAt(),
      subs.findByOrderId(o.getId()).stream().map(s -> toSubDto(s, forStaff)).toList());
  }

  public SubOrderDto toSubDto(FarmerSubOrder s, boolean showCustomer) {
    Order o = s.getOrder(); User c = o.getCustomer(); Address a = o.getDeliveryAddress();
    boolean contact = showCustomer && s.getStatus() != OrderStatus.ORDER_PLACED && s.getStatus() != OrderStatus.FARMER_REJECTED && s.getStatus() != OrderStatus.CANCELLED;
    String addr = showCustomer ? (a.getLine1() + ", " + a.getCity() + ", " + a.getDistrict() + ", " + a.getState() + " " + a.getPincode()) : null;
    return new SubOrderDto(s.getId(), o.getId(), o.getOrderNumber(), s.getStatus(), s.getFarmer().getId(), s.getFarmer().getFarmName(), s.getFarmer().getUser().getFullName(), s.getSubtotal(), s.getDeliveryCharge(),
      s.getFulfilmentMode(), s.getRejectionReason(), items.findBySubOrderId(s.getId()).stream().map(i -> new ItemDto(i.getProduct().getId(), i.getProductName(), i.getUnitPrice(), i.getQuantity(), i.getLineTotal())).toList(),
      s.getCreatedAt(), showCustomer ? c.getFullName() : null, contact ? c.getMobile() : null, contact ? addr : null, o.getPaymentStatus(), o.getPaymentMethod());
  }

  @Transactional(readOnly = true)
  public TrackingDto tracking(Long userId, Role role, Long subId) {
    FarmerSubOrder s = subs.findById(subId).orElseThrow(() -> ApiException.notFound("Order not found"));
    boolean customer = s.getOrder().getCustomer().getId().equals(userId), farmer = s.getFarmer().getUser().getId().equals(userId);
    if (!(role == Role.ADMIN || (role == Role.CUSTOMER && customer) || (role == Role.FARMER && farmer))) throw ApiException.notFound("Order not found");
    Delivery d = deliveries.findBySubOrderId(subId).orElse(null);
    PartnerDto partner = null;
    if (d != null && d.getPartner() != null) {
      String vehicle = deliveryProfiles.findByUserId(d.getPartner().getId()).map(p -> p.getVehicleType() + (p.getVehicleNumber() == null ? "" : " " + p.getVehicleNumber())).orElse(null);
      partner = new PartnerDto(d.getPartner().getFullName(), d.getPartner().getMobile(), vehicle);
    }
    boolean live = d != null && d.isTrackingActive() && d.getLastLatitude() != null;
    Address a = s.getOrder().getDeliveryAddress();
    return new TrackingDto(toSubDto(s, farmer || role == Role.ADMIN),
      history.findBySubOrderIdOrderByIdAsc(subId).stream().map(h -> new HistoryDto(h.getStatus(), h.getCreatedAt(), h.getUpdatedBy(), h.getDescription())).toList(),
      partner, d != null && d.isTrackingActive(), live ? d.getLastLatitude() : null, live ? d.getLastLongitude() : null, live ? d.getLastLocationAt() : null,
      s.getFarmer().getLatitude(), s.getFarmer().getLongitude(), a.getLatitude(), a.getLongitude());
  }

  @Transactional(readOnly = true)
  public String invoiceHtml(Long userId, Long orderId) {
    Order o = orders.findById(orderId).filter(x -> x.getCustomer().getId().equals(userId)).orElseThrow(() -> ApiException.notFound("Order not found"));
    StringBuilder b = new StringBuilder("<!doctype html><html><head><meta charset=utf-8><title>Invoice ").append(o.getOrderNumber())
      .append("</title><style>body{font-family:Arial,sans-serif;max-width:720px;margin:24px auto;color:#222}table{width:100%;border-collapse:collapse}td,th{border-bottom:1px solid #ddd;padding:6px;text-align:left}.r{text-align:right}</style></head><body>");
    b.append("<h2>FarmDirect Invoice</h2><p>Order <b>").append(esc(o.getOrderNumber())).append("</b><br>Date: ").append(o.getCreatedAt()).append("<br>Customer: ").append(esc(o.getCustomer().getFullName())).append("</p>");
    for (FarmerSubOrder s : subs.findByOrderId(o.getId())) {
      b.append("<h4>Sold by ").append(esc(s.getFarmer().getFarmName())).append(" (").append(esc(s.getFarmer().getUser().getFullName())).append(") - ").append(s.getStatus()).append("</h4><table><tr><th>Item</th><th class=r>Qty</th><th class=r>Price</th><th class=r>Total</th></tr>");
      for (OrderItem i : items.findBySubOrderId(s.getId())) b.append("<tr><td>").append(esc(i.getProductName())).append("</td><td class=r>").append(i.getQuantity().stripTrailingZeros().toPlainString()).append("</td><td class=r>").append(i.getUnitPrice()).append("</td><td class=r>").append(i.getLineTotal()).append("</td></tr>");
      b.append("<tr><td colspan=3 class=r>Delivery</td><td class=r>").append(s.getDeliveryCharge()).append("</td></tr></table>");
    }
    b.append("<p class=r>Subtotal: INR ").append(o.getSubtotal()).append("<br>Delivery: INR ").append(o.getDeliveryCharge()).append("<br>Discount: INR ").append(o.getDiscount()).append("<br><b>Total: INR ").append(o.getTotal()).append("</b><br>Payment: ").append(o.getPaymentMethod()).append(" (").append(o.getPaymentStatus()).append(")</p><script>window.print()</script></body></html>");
    return b.toString();
  }
  private static String esc(String s) { return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
}
