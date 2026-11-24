package com.farmdirect.service;

import com.farmdirect.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Thin REST client for Razorpay (test or live keys come from env). Card data never touches this server. */
@Service
public class RazorpayService {
  private final RestClient http = RestClient.create("https://api.razorpay.com/v1");
  @Value("${app.razorpay.key-id}") String keyId;
  @Value("${app.razorpay.key-secret}") String keySecret;
  @Value("${app.razorpay.webhook-secret}") String webhookSecret;

  public String keyId() { return keyId; }
  private void ensure() { if (keyId.isBlank() || keySecret.isBlank()) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Online payments are not configured"); }

  @SuppressWarnings("unchecked")
  public String createOrder(long amountPaise, String receipt) {
    ensure();
    try {
      Map<String, Object> res = http.post().uri("/orders").headers(h -> h.setBasicAuth(keyId, keySecret)).contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("amount", amountPaise, "currency", "INR", "receipt", receipt)).retrieve().body(Map.class);
      return String.valueOf(res.get("id"));
    } catch (Exception e) { throw new ApiException(HttpStatus.BAD_GATEWAY, "Could not start payment"); }
  }

  @SuppressWarnings("unchecked")
  public String refund(String paymentId, long amountPaise) {
    ensure();
    try {
      Map<String, Object> res = http.post().uri("/payments/" + paymentId + "/refund").headers(h -> h.setBasicAuth(keyId, keySecret)).contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("amount", amountPaise)).retrieve().body(Map.class);
      return String.valueOf(res.get("id"));
    } catch (Exception e) { throw new ApiException(HttpStatus.BAD_GATEWAY, "Refund request failed"); }
  }

  public boolean verifyCheckoutSignature(String orderId, String paymentId, String signature) { return hmacMatches(keySecret, orderId + "|" + paymentId, signature); }
  public boolean verifyWebhookSignature(String rawBody, String signature) { return !webhookSecret.isBlank() && hmacMatches(webhookSecret, rawBody, signature); }

  public static boolean hmacMatches(String secret, String data, String signature) {
    if (secret == null || secret.isBlank() || signature == null) return false;
    try {
      Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      String expected = HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
      return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) { return false; }
  }
}
