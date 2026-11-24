package com.farmdirect.controller;

import com.fasterxml.jackson.databind.*;
import com.farmdirect.dto.OrderDtos.PaymentVerifyRequest;
import com.farmdirect.entity.*;
import com.farmdirect.enums.*;
import com.farmdirect.exception.ApiException;
import com.farmdirect.repository.*;
import com.farmdirect.security.Current;
import com.farmdirect.service.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class PaymentController {
  private final RazorpayService razorpay; private final OrderService orders; private final PaymentRepository payments; private final RefundRepository refunds; private final ObjectMapper mapper;
  public PaymentController(RazorpayService razorpay, OrderService orders, PaymentRepository payments, RefundRepository refunds, ObjectMapper mapper) {
    this.razorpay = razorpay; this.orders = orders; this.payments = payments; this.refunds = refunds; this.mapper = mapper;
  }

  /** Called by the browser after Razorpay Checkout. Success is only reported when the HMAC signature verifies on the server. */
  @PostMapping("/api/customer/payments/verify") @Transactional
  public Map<String, String> verify(@Valid @RequestBody PaymentVerifyRequest r) {
    Payment p = payments.findByRazorpayOrderId(r.razorpayOrderId()).filter(x -> x.getOrder().getCustomer().getId().equals(Current.id())).orElseThrow(() -> ApiException.notFound("Payment not found"));
    if (!razorpay.verifyCheckoutSignature(r.razorpayOrderId(), r.razorpayPaymentId(), r.razorpaySignature())) { orders.paymentFailed(p); throw ApiException.badRequest("Payment verification failed"); }
    orders.confirmPayment(p, r.razorpayPaymentId());
    return Map.of("status", "PAID", "orderNumber", p.getOrder().getOrderNumber());
  }

  /** Razorpay webhook: payment.captured / payment.failed / refund.processed. Verified against the raw body. */
  @PostMapping("/api/payments/webhook") @Transactional
  public ResponseEntity<Void> webhook(@RequestBody String raw, @RequestHeader(value = "X-Razorpay-Signature", required = false) String sig) throws Exception {
    if (!razorpay.verifyWebhookSignature(raw, sig)) return ResponseEntity.status(400).build();
    JsonNode root = mapper.readTree(raw); String event = root.path("event").asText();
    switch (event) {
      case "payment.captured", "order.paid" -> {
        JsonNode e = root.path("payload").path("payment").path("entity");
        payments.findByRazorpayOrderId(e.path("order_id").asText()).ifPresent(p -> orders.confirmPayment(p, e.path("id").asText()));
      }
      case "payment.failed" -> payments.findByRazorpayOrderId(root.path("payload").path("payment").path("entity").path("order_id").asText()).ifPresent(orders::paymentFailed);
      case "refund.processed" -> {
        JsonNode e = root.path("payload").path("refund").path("entity");
        refunds.findAll().stream().filter(x -> e.path("id").asText().equals(x.getRazorpayRefundId())).forEach(x -> { x.setStatus("REFUNDED"); x.getPayment().setStatus(PaymentStatus.REFUNDED); });
      }
      default -> {}
    }
    return ResponseEntity.ok().build();
  }
}
