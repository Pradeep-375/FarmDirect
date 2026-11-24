package com.farmdirect.controller;

import com.farmdirect.dto.OrderDtos.LocationRequest;
import com.farmdirect.enums.OrderStatus;
import com.farmdirect.security.Current;
import com.farmdirect.service.DeliveryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/delivery")
public class DeliveryController {
  private final DeliveryService svc;
  public DeliveryController(DeliveryService svc) { this.svc = svc; }
  @GetMapping("/assignments") public List<DeliveryService.Assignment> assignments() { return svc.assignments(Current.id()); }
  @PostMapping("/{id}/pickup") public Map<String, String> pickup(@PathVariable Long id) { svc.step(Current.id(), id, OrderStatus.PICKED_UP); return Map.of("message", "Pickup confirmed"); }
  @PostMapping("/{id}/out-for-delivery") public Map<String, String> out(@PathVariable Long id) { svc.step(Current.id(), id, OrderStatus.OUT_FOR_DELIVERY); return Map.of("message", "Marked out for delivery"); }
  @PostMapping("/{id}/delivered") public Map<String, String> delivered(@PathVariable Long id) { svc.step(Current.id(), id, OrderStatus.DELIVERED); return Map.of("message", "Delivery confirmed"); }
  @PostMapping("/{id}/location") public Map<String, String> location(@PathVariable Long id, @Valid @RequestBody LocationRequest r) { svc.location(Current.id(), id, r); return Map.of("message", "ok"); }
}
