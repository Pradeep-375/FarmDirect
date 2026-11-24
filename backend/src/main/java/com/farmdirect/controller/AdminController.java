package com.farmdirect.controller;

import com.farmdirect.dto.CatalogDtos.*;
import com.farmdirect.dto.OrderDtos.*;
import com.farmdirect.entity.*;
import com.farmdirect.enums.*;
import com.farmdirect.exception.ApiException;
import com.farmdirect.repository.*;
import com.farmdirect.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController @RequestMapping("/api/admin")
public class AdminController {
  private final UserRepository users; private final FarmerProfileRepository farmers; private final ProductRepository products; private final ProductCategoryRepository categories;
  private final OrderRepository orders; private final FarmerSubOrderRepository subs; private final OrderItemRepository items; private final DeliveryRepository deliveries;
  private final ComplaintRepository complaints; private final RefundRepository refunds; private final ReviewRepository reviews; private final CouponRepository coupons;
  private final PlatformSettingRepository settings; private final FarmerVerificationRepository verifications; private final DeliveryProfileRepository deliveryProfiles;
  private final OrderService orderService; private final DeliveryService deliveryService; private final CatalogService catalog; private final NotificationService notifier; private final RazorpayService razorpay;

  public AdminController(UserRepository users, FarmerProfileRepository farmers, ProductRepository products, ProductCategoryRepository categories, OrderRepository orders, FarmerSubOrderRepository subs,
      OrderItemRepository items, DeliveryRepository deliveries, ComplaintRepository complaints, RefundRepository refunds, ReviewRepository reviews, CouponRepository coupons, PlatformSettingRepository settings,
      FarmerVerificationRepository verifications, DeliveryProfileRepository deliveryProfiles, OrderService orderService, DeliveryService deliveryService, CatalogService catalog, NotificationService notifier, RazorpayService razorpay) {
    this.users = users; this.farmers = farmers; this.products = products; this.categories = categories; this.orders = orders; this.subs = subs; this.items = items; this.deliveries = deliveries;
    this.complaints = complaints; this.refunds = refunds; this.reviews = reviews; this.coupons = coupons; this.settings = settings; this.verifications = verifications; this.deliveryProfiles = deliveryProfiles;
    this.orderService = orderService; this.deliveryService = deliveryService; this.catalog = catalog; this.notifier = notifier; this.razorpay = razorpay;
  }

  // ---- analytics
  @GetMapping("/analytics") @Transactional(readOnly = true)
  public Map<String, Object> analytics() {
    List<Order> all = orders.findAll();
    Set<OrderStatus> done = EnumSet.of(OrderStatus.DELIVERED), dead = EnumSet.of(OrderStatus.CANCELLED, OrderStatus.FARMER_REJECTED, OrderStatus.REFUNDED);
    Map<String, Long> registrations = new TreeMap<>();
    users.findAll().forEach(u -> registrations.merge(YearMonth.from(u.getCreatedAt().atZone(ZoneOffset.UTC)).toString(), 1L, Long::sum));
    Map<String, BigDecimal> categoryQty = new HashMap<>();
    items.findAll().forEach(i -> categoryQty.merge(i.getProduct().getCategory().getName(), i.getQuantity(), BigDecimal::add));
    Instant weekAgo = Instant.now().minusSeconds(7 * 86400);
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("totalFarmers", users.countByRole(Role.FARMER)); m.put("totalCustomers", users.countByRole(Role.CUSTOMER)); m.put("totalDeliveryPartners", users.countByRole(Role.DELIVERY_PARTNER));
    m.put("totalProducts", products.findAll().stream().filter(p -> !p.isDeleted()).count()); m.put("totalOrders", all.size());
    m.put("completedOrders", all.stream().filter(o -> done.contains(o.getStatus())).count());
    m.put("cancelledOrders", all.stream().filter(o -> dead.contains(o.getStatus())).count());
    m.put("pendingOrders", all.stream().filter(o -> !done.contains(o.getStatus()) && !dead.contains(o.getStatus())).count());
    m.put("pendingVerifications", farmers.findAll().stream().filter(f -> f.getVerificationStatus() == VerificationStatus.PENDING).count());
    m.put("monthlyRegistrations", registrations); m.put("popularCategories", categoryQty);
    m.put("ordersLast7Days", all.stream().filter(o -> o.getCreatedAt().isAfter(weekAgo)).count());
    m.put("openComplaints", complaints.findAll().stream().filter(c -> "OPEN".equals(c.getStatus())).count());
    return m;
  }

  // ---- users
  public record UserRow(Long id, String fullName, String email, String mobile, Role role, AccountStatus status, String state, String district, java.time.Instant createdAt, String verification) {}
  @GetMapping("/users") @Transactional(readOnly = true)
  public List<UserRow> users(@RequestParam(required = false) Role role, @RequestParam(required = false) String q) {
    List<User> list = role == null ? users.findAll() : users.findByRole(role);
    return list.stream().filter(u -> q == null || q.isBlank() || (u.getFullName() + u.getEmail() + u.getMobile()).toLowerCase().contains(q.toLowerCase()))
      .map(u -> new UserRow(u.getId(), u.getFullName(), u.getEmail(), u.getMobile(), u.getRole(), u.getStatus(), u.getState(), u.getDistrict(), u.getCreatedAt(),
        u.getRole() == Role.FARMER ? farmers.findByUserId(u.getId()).map(f -> f.getVerificationStatus().name()).orElse(null) : null)).toList();
  }
  public record StatusBody(@NotNull AccountStatus status) {}
  @PatchMapping("/users/{id}/status") @Transactional
  public Map<String, String> setStatus(@PathVariable Long id, @Valid @RequestBody StatusBody b) {
    User u = users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
    if (u.getRole() == Role.ADMIN) throw ApiException.forbidden("Administrator accounts cannot be changed here");
    u.setStatus(b.status()); return Map.of("message", "Account updated");
  }

  // ---- farmer verification
  public record VerificationRow(Long farmerId, String farmName, String farmerName, String status, List<FarmerVerification> documents) {}
  @GetMapping("/verifications") @Transactional(readOnly = true)
  public List<VerificationRow> verifications() {
    return farmers.findAll().stream().map(f -> new VerificationRow(f.getId(), f.getFarmName(), f.getUser().getFullName(), f.getVerificationStatus().name(), verifications.findByFarmerId(f.getId())))
      .sorted(Comparator.comparing((VerificationRow r) -> !"PENDING".equals(r.status()))).toList();
  }
  public record DecisionBody(boolean approve, String note) {}
  @PostMapping("/verifications/{farmerId}") @Transactional
  public Map<String, String> decide(@PathVariable Long farmerId, @RequestBody DecisionBody b) {
    FarmerProfile f = farmers.findById(farmerId).orElseThrow(() -> ApiException.notFound("Farmer not found"));
    VerificationStatus st = b.approve() ? VerificationStatus.APPROVED : VerificationStatus.REJECTED;
    f.setVerificationStatus(st);
    verifications.findByFarmerId(farmerId).stream().filter(d -> d.getStatus() == VerificationStatus.PENDING).forEach(d -> { d.setStatus(st); d.setReviewerNote(b.note()); });
    notifier.notify(f.getUser(), b.approve() ? "Verification approved" : "Verification rejected", b.approve() ? "Your farm is now verified." : ("Your verification was rejected. " + (b.note() == null ? "" : b.note())), "ACCOUNT", true);
    return Map.of("message", "Decision saved");
  }

  // ---- categories & products
  @GetMapping("/categories") public List<ProductCategory> categories() { return categories.findAll(); }
  @PostMapping("/categories") public ProductCategory addCategory(@Valid @RequestBody CategoryRequest r) {
    if (categories.findByNameIgnoreCase(r.name().trim()).isPresent()) throw ApiException.conflict("Category already exists");
    ProductCategory c = new ProductCategory(); c.setName(r.name().trim()); c.setDescription(r.description()); return categories.save(c);
  }
  @PutMapping("/categories/{id}") @Transactional public ProductCategory editCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest r) {
    ProductCategory c = categories.findById(id).orElseThrow(() -> ApiException.notFound("Category not found")); c.setName(r.name().trim()); c.setDescription(r.description()); if (r.active() != null) c.setActive(r.active()); return c;
  }
  @GetMapping("/products") @Transactional(readOnly = true) public List<ProductDto> products() { return products.findAll().stream().filter(p -> !p.isDeleted()).map(catalog::toDto).toList(); }
  @PatchMapping("/products/{id}/toggle") @Transactional public Map<String, String> toggleProduct(@PathVariable Long id) {
    Product p = products.findById(id).orElseThrow(() -> ApiException.notFound("Product not found")); p.setEnabled(!p.isEnabled()); return Map.of("message", "Updated");
  }

  // ---- orders & deliveries
  @GetMapping("/orders") @Transactional(readOnly = true) public List<OrderDto> orders() { return orders.findAll().stream().sorted(Comparator.comparing(Order::getId).reversed()).limit(300).map(o -> orderService.toOrderDto(o, true)).toList(); }
  @GetMapping("/suborders/{id}/tracking") public TrackingDto tracking(@PathVariable Long id) { return orderService.tracking(com.farmdirect.security.Current.id(), Role.ADMIN, id); }
  public record PartnerRow(Long id, String name, String mobile, String vehicle, boolean available) {}
  @GetMapping("/delivery-partners") @Transactional(readOnly = true)
  public List<PartnerRow> partners() {
    return users.findByRole(Role.DELIVERY_PARTNER).stream().filter(u -> u.getStatus() == AccountStatus.ACTIVE).map(u -> {
      var p = deliveryProfiles.findByUserId(u.getId()).orElse(null);
      return new PartnerRow(u.getId(), u.getFullName(), u.getMobile(), p == null ? null : p.getVehicleType(), p == null || p.isAvailable());
    }).toList();
  }
  public record AssignBody(@NotNull Long subOrderId, @NotNull Long partnerId) {}
  @PostMapping("/deliveries/assign") public Map<String, String> assign(@Valid @RequestBody AssignBody b) { deliveryService.assign(b.subOrderId(), b.partnerId()); return Map.of("message", "Delivery partner assigned"); }
  @GetMapping("/deliveries") @Transactional(readOnly = true)
  public List<Map<String, Object>> deliveries() {
    return deliveries.findAll().stream().sorted(Comparator.comparing(Delivery::getId).reversed()).map(d -> {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("id", d.getId()); m.put("subOrderId", d.getSubOrder().getId()); m.put("orderNumber", d.getSubOrder().getOrder().getOrderNumber()); m.put("status", d.getSubOrder().getStatus());
      m.put("farmName", d.getSubOrder().getFarmer().getFarmName()); m.put("partner", d.getPartner() == null ? null : d.getPartner().getFullName()); m.put("trackingActive", d.isTrackingActive()); return m;
    }).toList();
  }
  @GetMapping("/awaiting-assignment") @Transactional(readOnly = true)
  public List<SubOrderDto> awaiting() {
    return subs.findAll().stream().filter(s -> s.getOrder().isConfirmed() && s.getFulfilmentMode() == FulfilmentMode.PLATFORM_PARTNER && EnumSet.of(OrderStatus.FARMER_ACCEPTED, OrderStatus.PROCESSING, OrderStatus.READY_FOR_PICKUP).contains(s.getStatus())
      && deliveries.findBySubOrderId(s.getId()).map(d -> d.getPartner() == null).orElse(true)).map(s -> orderService.toSubDto(s, false)).toList();
  }

  // ---- complaints, refunds, reviews
  @GetMapping("/complaints") public List<Complaint> complaints() { return complaints.findAll().stream().sorted(Comparator.comparing(Complaint::getId).reversed()).toList(); }
  public record ResolveBody(@NotBlank String resolution) {}
  @PostMapping("/complaints/{id}/resolve") @Transactional public Map<String, String> resolve(@PathVariable Long id, @Valid @RequestBody ResolveBody b) {
    Complaint c = complaints.findById(id).orElseThrow(() -> ApiException.notFound("Not found")); c.setStatus("RESOLVED"); c.setResolution(b.resolution());
    if (c.getUser() != null) notifier.notify(c.getUser(), "Complaint resolved", b.resolution(), "ACCOUNT", false);
    return Map.of("message", "Resolved");
  }
  @GetMapping("/refunds") @Transactional(readOnly = true) public List<Map<String, Object>> refunds() {
    return refunds.findAll().stream().map(r -> Map.<String, Object>of("id", r.getId(), "orderNumber", r.getPayment().getOrder().getOrderNumber(), "amount", r.getAmount(), "status", r.getStatus(), "reason", String.valueOf(r.getReason()))).toList();
  }
  @PostMapping("/refunds/{id}/retry") @Transactional public Map<String, String> retryRefund(@PathVariable Long id) {
    Refund r = refunds.findById(id).orElseThrow(() -> ApiException.notFound("Not found"));
    if (!r.getStatus().startsWith("FAILED")) throw ApiException.badRequest("Only failed refunds can be retried");
    r.setRazorpayRefundId(razorpay.refund(r.getPayment().getRazorpayPaymentId(), r.getAmount().movePointRight(2).longValueExact())); r.setStatus("PROCESSING"); return Map.of("message", "Refund retried");
  }
  @GetMapping("/reviews") @Transactional(readOnly = true) public List<ReviewDto> reviews() {
    return reviews.findAll().stream().map(r -> new ReviewDto(r.getId(), r.getCustomer().getFullName(), r.getRating(), r.getComment(), r.getCreatedAt(), r.getProduct() == null ? null : r.getProduct().getId())).toList();
  }
  @DeleteMapping("/reviews/{id}") @Transactional public Map<String, String> deleteReview(@PathVariable Long id) {
    Review r = reviews.findById(id).orElseThrow(() -> ApiException.notFound("Not found")); FarmerProfile f = r.getFarmer(); reviews.delete(r);
    var rest = reviews.findByFarmerIdOrderByIdDesc(f.getId()).stream().filter(x -> !x.getId().equals(id)).toList();
    f.setRatingCount(rest.size()); f.setRatingAverage(rest.isEmpty() ? 0 : Math.round(rest.stream().mapToInt(Review::getRating).average().orElse(0) * 10.0) / 10.0);
    return Map.of("message", "Review removed");
  }

  // ---- promotions & settings
  public record CouponBody(@NotBlank String code, @Min(1) @Max(90) int percentOff, BigDecimal minOrder, Instant validUntil) {}
  @GetMapping("/coupons") public List<Coupon> coupons() { return coupons.findAll(); }
  @PostMapping("/coupons") public Coupon addCoupon(@Valid @RequestBody CouponBody b) {
    if (coupons.findByCodeIgnoreCase(b.code()).isPresent()) throw ApiException.conflict("Coupon code exists");
    Coupon c = new Coupon(); c.setCode(b.code().trim().toUpperCase()); c.setPercentOff(b.percentOff()); if (b.minOrder() != null) c.setMinOrder(b.minOrder()); c.setValidUntil(b.validUntil()); return coupons.save(c);
  }
  @PatchMapping("/coupons/{id}/toggle") @Transactional public Map<String, String> toggleCoupon(@PathVariable Long id) { Coupon c = coupons.findById(id).orElseThrow(() -> ApiException.notFound("Not found")); c.setActive(!c.isActive()); return Map.of("message", "Updated"); }

  private static final Set<String> SETTING_KEYS = Set.of("delivery.base", "delivery.freeAbove", "cod.enabled");
  @GetMapping("/settings") public Map<String, String> settings() { return settings.findAll().stream().collect(Collectors.toMap(PlatformSetting::getSettingKey, PlatformSetting::getSettingValue)); }
  public record SettingBody(@NotBlank String key, @NotBlank @Pattern(regexp = "^\\d+(\\.\\d{1,2})?$", message = "Enter a number") String value) {}
  @PutMapping("/settings") @Transactional public Map<String, String> setSetting(@Valid @RequestBody SettingBody b) {
    if (!SETTING_KEYS.contains(b.key())) throw ApiException.badRequest("Unknown setting");
    PlatformSetting s = settings.findBySettingKey(b.key()).orElseGet(() -> { PlatformSetting n = new PlatformSetting(); n.setSettingKey(b.key()); return n; }); s.setSettingValue(b.value()); settings.save(s);
    return Map.of("message", "Setting saved");
  }

  // ---- reports
  @GetMapping(value = "/reports/orders.csv", produces = "text/csv") @Transactional(readOnly = true)
  public ResponseEntity<String> ordersCsv() {
    StringBuilder b = new StringBuilder("order_number,created_at,customer,status,payment_method,payment_status,subtotal,delivery,discount,total\n");
    for (Order o : orders.findAll()) b.append(String.join(",", o.getOrderNumber(), String.valueOf(o.getCreatedAt()), csv(o.getCustomer().getFullName()), o.getStatus().name(), o.getPaymentMethod().name(), o.getPaymentStatus().name(),
      o.getSubtotal().toString(), o.getDeliveryCharge().toString(), o.getDiscount().toString(), o.getTotal().toString())).append('\n');
    return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=orders.csv").body(b.toString());
  }
  private static String csv(String s) { String v = s == null ? "" : s.replace("\"", "\"\""); if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0) v = "'" + v; return "\"" + v + "\""; }
}
