package com.farmdirect.controller;

import com.farmdirect.dto.CatalogDtos.*;
import com.farmdirect.dto.OrderDtos.*;
import com.farmdirect.entity.*;
import com.farmdirect.enums.*;
import com.farmdirect.exception.ApiException;
import com.farmdirect.repository.*;
import com.farmdirect.security.Current;
import com.farmdirect.service.*;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/farmer")
public class FarmerController {
  private final FarmerProfileRepository farmers; private final CatalogService catalog; private final OrderService orders; private final FarmerAnalyticsService analytics;
  private final FarmerVerificationRepository verifications; private final UserRepository users; private final ReviewRepository reviews; private final InventoryRepository inventory;

  public FarmerController(FarmerProfileRepository farmers, CatalogService catalog, OrderService orders, FarmerAnalyticsService analytics, FarmerVerificationRepository verifications, UserRepository users, ReviewRepository reviews, InventoryRepository inventory) {
    this.farmers = farmers; this.catalog = catalog; this.orders = orders; this.analytics = analytics; this.verifications = verifications; this.users = users; this.reviews = reviews; this.inventory = inventory;
  }
  private FarmerProfile me() { return farmers.findByUserId(Current.id()).orElseThrow(() -> ApiException.notFound("Farmer profile not found")); }

  @GetMapping("/profile") @Transactional(readOnly = true) public FarmerDto profile() { return catalog.toDto(me()); }

  @PutMapping("/profile") @Transactional
  public FarmerDto update(@Valid @RequestBody FarmerProfileRequest r) {
    FarmerProfile f = me(); User u = f.getUser();
    f.setFarmName(r.farmName()); f.setFarmAddress(r.farmAddress()); f.setFarmLocation(r.farmLocation()); f.setLatitude(r.latitude()); f.setLongitude(r.longitude());
    f.setFarmingCategory(r.farmingCategory()); f.setFarmDetails(r.farmDetails()); f.setProfileImageUrl(r.profileImageUrl());
    f.setFarmImageUrls(r.farmImages() == null ? null : String.join(",", r.farmImages()));
    if (r.state() != null) u.setState(r.state()); if (r.district() != null) u.setDistrict(r.district()); if (r.city() != null) u.setCity(r.city()); if (r.pincode() != null) u.setPincode(r.pincode());
    return catalog.toDto(f);
  }

  @PostMapping("/verification") @Transactional
  public Map<String, String> submitDoc(@Valid @RequestBody VerificationDocRequest r) {
    FarmerProfile f = me(); FarmerVerification v = new FarmerVerification(); v.setFarmer(f); v.setDocumentType(r.documentType()); v.setDocumentUrl(r.documentUrl()); verifications.save(v);
    if (f.getVerificationStatus() == VerificationStatus.REJECTED) f.setVerificationStatus(VerificationStatus.PENDING);
    return Map.of("message", "Document submitted for review");
  }
  @GetMapping("/verification") public List<FarmerVerification> docs() { return verifications.findByFarmerId(me().getId()); }

  @GetMapping("/products") public List<ProductDto> products() { return catalog.farmerProducts(me()); }
  @PostMapping("/products") public ProductDto create(@Valid @RequestBody ProductRequest r) { return catalog.save(me(), null, r); }
  @PutMapping("/products/{id}") public ProductDto update(@PathVariable Long id, @Valid @RequestBody ProductRequest r) { return catalog.save(me(), id, r); }
  @PatchMapping("/products/{id}/stock") public Map<String, String> stock(@PathVariable Long id, @Valid @RequestBody StockRequest r) { catalog.setStock(me(), id, r.stock()); return Map.of("message", "Stock updated"); }
  @PatchMapping("/products/{id}/toggle") public Map<String, String> toggle(@PathVariable Long id) { catalog.toggle(me(), id); return Map.of("message", "Availability updated"); }
  @DeleteMapping("/products/{id}") public Map<String, String> delete(@PathVariable Long id) { catalog.delete(me(), id); return Map.of("message", "Product deleted"); }

  @GetMapping("/orders") public List<SubOrderDto> orders(@RequestParam(required = false) OrderStatus status) { return orders.farmerOrders(Current.id(), status); }
  @PostMapping("/orders/{id}/accept") public Map<String, String> accept(@PathVariable Long id, @RequestBody(required = false) AcceptRequest r) { orders.farmerAccept(Current.id(), id, r == null ? null : r.fulfilmentMode()); return Map.of("message", "Order accepted"); }
  @PostMapping("/orders/{id}/reject") public Map<String, String> reject(@PathVariable Long id, @Valid @RequestBody RejectRequest r) { orders.farmerReject(Current.id(), id, r.reason()); return Map.of("message", "Order rejected"); }
  @PostMapping("/orders/{id}/status") public Map<String, String> status(@PathVariable Long id, @Valid @RequestBody StatusRequest r) { orders.farmerStatus(Current.id(), id, r.status(), r.note()); return Map.of("message", "Status updated"); }
  @GetMapping("/orders/{id}/tracking") public TrackingDto tracking(@PathVariable Long id) { return orders.tracking(Current.id(), Role.FARMER, id); }

  @GetMapping("/analytics") public Map<String, Object> analytics() { return analytics.analytics(me()); }
  @GetMapping("/reviews") public List<ReviewDto> reviews() {
    return reviews.findByFarmerIdOrderByIdDesc(me().getId()).stream().map(r -> new ReviewDto(r.getId(), r.getCustomer().getFullName(), r.getRating(), r.getComment(), r.getCreatedAt(), r.getProduct() == null ? null : r.getProduct().getId())).toList();
  }
}
