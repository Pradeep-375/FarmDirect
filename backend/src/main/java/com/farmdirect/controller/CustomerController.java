package com.farmdirect.controller;

import com.farmdirect.dto.CatalogDtos.ProductDto;
import com.farmdirect.dto.OrderDtos.*;
import com.farmdirect.entity.*;
import com.farmdirect.enums.Role;
import com.farmdirect.repository.*;
import com.farmdirect.security.Current;
import com.farmdirect.service.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/customer")
public class CustomerController {
  private final CartService cart; private final OrderService orders; private final ComplaintRepository complaints; private final UserRepository users;
  public CustomerController(CartService cart, OrderService orders, ComplaintRepository complaints, UserRepository users) { this.cart = cart; this.orders = orders; this.complaints = complaints; this.users = users; }

  @GetMapping("/cart") public CartDto cart() { return cart.view(Current.id()); }
  @PostMapping("/cart/items") public CartDto addToCart(@Valid @RequestBody CartItemRequest r) { return cart.set(Current.id(), r, true); }
  @PutMapping("/cart/items") public CartDto setQuantity(@Valid @RequestBody CartItemRequest r) { return cart.set(Current.id(), r, false); }
  @DeleteMapping("/cart/items/{itemId}") public CartDto removeFromCart(@PathVariable Long itemId) { return cart.remove(Current.id(), itemId); }

  @GetMapping("/wishlist") public List<ProductDto> wishlist() { return cart.wishlist(Current.id()); }
  @PostMapping("/wishlist/{productId}") public List<ProductDto> addWish(@PathVariable Long productId) { return cart.addWish(Current.id(), productId); }
  @DeleteMapping("/wishlist/{productId}") public List<ProductDto> removeWish(@PathVariable Long productId) { return cart.removeWish(Current.id(), productId); }

  public record AddressDto(Long id, String label, String line1, String city, String district, String state, String pincode, Double latitude, Double longitude, boolean defaultAddress) {}
  private AddressDto dto(Address a) { return new AddressDto(a.getId(), a.getLabel(), a.getLine1(), a.getCity(), a.getDistrict(), a.getState(), a.getPincode(), a.getLatitude(), a.getLongitude(), a.isDefaultAddress()); }
  @GetMapping("/addresses") public List<AddressDto> addresses() { return cart.addresses(Current.id()).stream().map(this::dto).toList(); }
  @PostMapping("/addresses") public AddressDto addAddress(@Valid @RequestBody AddressRequest r) { return dto(cart.saveAddress(Current.id(), null, r)); }
  @PutMapping("/addresses/{id}") public AddressDto updateAddress(@PathVariable Long id, @Valid @RequestBody AddressRequest r) { return dto(cart.saveAddress(Current.id(), id, r)); }
  @DeleteMapping("/addresses/{id}") public void deleteAddress(@PathVariable Long id) { cart.deleteAddress(Current.id(), id); }

  @PostMapping("/orders/checkout") public CheckoutResponse checkout(@Valid @RequestBody CheckoutRequest r) { return orders.checkout(Current.id(), r); }
  @GetMapping("/orders") public List<OrderDto> myOrders() { return orders.customerOrders(Current.id()); }
  @GetMapping("/orders/{id}") public OrderDto order(@PathVariable Long id) { return orders.customerOrder(Current.id(), id); }
  @GetMapping("/orders/{id}/invoice") public ResponseEntity<String> invoice(@PathVariable Long id) {
    return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).header("Content-Disposition", "inline; filename=invoice-" + id + ".html").body(orders.invoiceHtml(Current.id(), id));
  }
  @GetMapping("/suborders/{id}/tracking") public TrackingDto tracking(@PathVariable Long id) { return orders.tracking(Current.id(), Role.CUSTOMER, id); }
  @PostMapping("/suborders/{id}/cancel") public Map<String, String> cancel(@PathVariable Long id) { orders.customerCancel(Current.id(), id); return Map.of("message", "Order cancelled"); }
  @PostMapping("/suborders/{id}/return") public Map<String, String> ret(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
    orders.requestReturn(Current.id(), id, body == null ? null : body.get("reason")); return Map.of("message", "Return requested");
  }
  @PostMapping("/reviews") public Map<String, String> review(@Valid @RequestBody ReviewRequest r) { orders.review(Current.id(), r); return Map.of("message", "Thanks for your review"); }
  @PostMapping("/complaints") public Map<String, String> complaint(@Valid @RequestBody ComplaintRequest r) {
    Complaint c = new Complaint(); c.setUser(users.getReferenceById(Current.id())); c.setSubject(r.subject()); c.setDescription(r.description()); c.setOrderId(r.orderId()); complaints.save(c);
    return Map.of("message", "Complaint submitted");
  }
}
