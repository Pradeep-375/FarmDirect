package com.farmdirect.dto;

import com.farmdirect.enums.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {
  private OrderDtos() {}
  public record CartItemRequest(@NotNull Long productId, @NotNull @DecimalMin("0.001") BigDecimal quantity) {}
  public record CartLine(Long itemId, Long productId, String name, String imageUrl, BigDecimal price, Unit unit, BigDecimal quantity, BigDecimal minOrderQuantity,
    BigDecimal available, String farmerName, Long farmerId, BigDecimal lineTotal) {}
  public record CartDto(List<CartLine> items, BigDecimal subtotal) {}
  public record AddressRequest(String label, @NotBlank String line1, @NotBlank String city, @NotBlank String district, @NotBlank String state,
    @NotBlank @Pattern(regexp = "^[1-9]\\d{5}$") String pincode, Double latitude, Double longitude, boolean defaultAddress) {}
  public record CheckoutRequest(@NotNull Long addressId, @NotNull PaymentMethod paymentMethod, String couponCode, FulfilmentMode fulfilmentMode) {}
  public record CheckoutResponse(Long orderId, String orderNumber, BigDecimal total, PaymentMethod paymentMethod, String razorpayKeyId, String razorpayOrderId, long amountPaise) {}
  public record PaymentVerifyRequest(@NotBlank String razorpayOrderId, @NotBlank String razorpayPaymentId, @NotBlank String razorpaySignature) {}
  public record RejectRequest(@NotBlank String reason) {}
  public record StatusRequest(@NotNull OrderStatus status, String note) {}
  public record AcceptRequest(FulfilmentMode fulfilmentMode) {}
  public record LocationRequest(@NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude, @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude) {}
  public record ReviewRequest(@NotNull Long subOrderId, Long productId, @Min(1) @Max(5) int rating, @Size(max = 1000) String comment) {}
  public record ComplaintRequest(@NotBlank String subject, @NotBlank @Size(max = 2000) String description, Long orderId) {}

  public record ItemDto(Long productId, String name, BigDecimal unitPrice, BigDecimal quantity, BigDecimal lineTotal) {}
  public record HistoryDto(OrderStatus status, Instant at, String updatedBy, String description) {}
  public record PartnerDto(String name, String mobile, String vehicle) {}
  public record SubOrderDto(Long id, Long orderId, String orderNumber, OrderStatus status, Long farmerId, String farmName, String farmerName, BigDecimal subtotal, BigDecimal deliveryCharge,
    FulfilmentMode fulfilmentMode, String rejectionReason, List<ItemDto> items, Instant createdAt, String customerName, String customerMobile, String deliveryAddress,
    PaymentStatus paymentStatus, PaymentMethod paymentMethod) {}
  public record TrackingDto(SubOrderDto order, List<HistoryDto> history, PartnerDto partner, boolean trackingActive, Double partnerLat, Double partnerLng, Instant partnerLocationAt,
    Double pickupLat, Double pickupLng, Double dropLat, Double dropLng) {}
  public record OrderDto(Long id, String orderNumber, BigDecimal subtotal, BigDecimal deliveryCharge, BigDecimal discount, BigDecimal total, PaymentMethod paymentMethod,
    PaymentStatus paymentStatus, OrderStatus status, Instant createdAt, List<SubOrderDto> subOrders) {}
}
