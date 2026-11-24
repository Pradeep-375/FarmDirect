package com.farmdirect.service;

import com.farmdirect.dto.CatalogDtos.ProductDto;
import com.farmdirect.dto.OrderDtos.*;
import com.farmdirect.entity.*;
import com.farmdirect.exception.ApiException;
import com.farmdirect.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

/** Cart, wishlist and address book. Everything lives in MySQL so it follows the user across devices. */
@Service
public class CartService {
  private final CartRepository carts; private final CartItemRepository cartItems; private final WishlistRepository wishlists; private final WishlistItemRepository wishItems;
  private final AddressRepository addresses; private final UserRepository users; private final ProductRepository products; private final CatalogService catalog;

  public CartService(CartRepository carts, CartItemRepository cartItems, WishlistRepository wishlists, WishlistItemRepository wishItems, AddressRepository addresses,
                     UserRepository users, ProductRepository products, CatalogService catalog) {
    this.carts = carts; this.cartItems = cartItems; this.wishlists = wishlists; this.wishItems = wishItems; this.addresses = addresses; this.users = users; this.products = products; this.catalog = catalog;
  }

  private Cart cart(Long userId) { return carts.findByUserId(userId).orElseGet(() -> { Cart c = new Cart(); c.setUser(users.getReferenceById(userId)); return carts.save(c); }); }

  @Transactional
  public CartDto view(Long userId) {
    Cart c = cart(userId); List<CartLine> lines = new ArrayList<>(); BigDecimal sub = BigDecimal.ZERO;
    for (CartItem i : cartItems.findByCartId(c.getId())) {
      ProductDto p = catalog.toDto(i.getProduct());
      BigDecimal lt = p.price().multiply(i.getQuantity());
      sub = sub.add(lt);
      lines.add(new CartLine(i.getId(), p.id(), p.name(), p.imageUrls().isEmpty() ? null : p.imageUrls().get(0), p.price(), p.unit(), i.getQuantity(), p.minOrderQuantity(), p.available(), p.farmerName(), p.farmerId(), lt));
    }
    return new CartDto(lines, sub);
  }

  @Transactional
  public CartDto set(Long userId, CartItemRequest r, boolean add) {
    Product p = products.findById(r.productId()).filter(x -> !x.isDeleted() && x.isEnabled()).orElseThrow(() -> ApiException.notFound("Product not available"));
    Cart c = cart(userId);
    CartItem item = cartItems.findByCartIdAndProductId(c.getId(), p.getId()).orElseGet(() -> { CartItem i = new CartItem(); i.setCart(c); i.setProduct(p); i.setQuantity(BigDecimal.ZERO); return i; });
    BigDecimal q = add ? item.getQuantity().add(r.quantity()) : r.quantity();
    if (q.compareTo(p.getMinOrderQuantity()) < 0) throw ApiException.badRequest("Minimum order for " + p.getName() + " is " + p.getMinOrderQuantity() + " " + p.getUnit().name().toLowerCase());
    item.setQuantity(q); cartItems.save(item);
    return view(userId);
  }

  @Transactional
  public CartDto remove(Long userId, Long itemId) {
    CartItem i = cartItems.findById(itemId).orElseThrow(() -> ApiException.notFound("Item not found"));
    if (!i.getCart().getUser().getId().equals(userId)) throw ApiException.forbidden("Not your cart");
    cartItems.delete(i); return view(userId);
  }

  // wishlist
  private Wishlist wish(Long userId) { return wishlists.findByUserId(userId).orElseGet(() -> { Wishlist w = new Wishlist(); w.setUser(users.getReferenceById(userId)); return wishlists.save(w); }); }
  @Transactional(readOnly = false)
  public List<ProductDto> wishlist(Long userId) { return wishItems.findByWishlistId(wish(userId).getId()).stream().map(i -> catalog.toDto(i.getProduct())).toList(); }
  @Transactional public List<ProductDto> addWish(Long userId, Long productId) {
    Wishlist w = wish(userId);
    if (wishItems.findByWishlistIdAndProductId(w.getId(), productId).isEmpty()) { WishlistItem i = new WishlistItem(); i.setWishlist(w); i.setProduct(products.findById(productId).orElseThrow(() -> ApiException.notFound("Product not found"))); wishItems.save(i); }
    return wishlist(userId);
  }
  @Transactional public List<ProductDto> removeWish(Long userId, Long productId) {
    wishItems.findByWishlistIdAndProductId(wish(userId).getId(), productId).ifPresent(wishItems::delete); return wishlist(userId);
  }

  // addresses
  public List<Address> addresses(Long userId) { return addresses.findByUserId(userId); }
  @Transactional public Address saveAddress(Long userId, Long id, AddressRequest r) {
    Address a = id == null ? new Address() : addresses.findById(id).filter(x -> x.getUser().getId().equals(userId)).orElseThrow(() -> ApiException.notFound("Address not found"));
    a.setUser(users.getReferenceById(userId)); a.setLabel(r.label()); a.setLine1(r.line1()); a.setCity(r.city()); a.setDistrict(r.district()); a.setState(r.state()); a.setPincode(r.pincode());
    a.setLatitude(r.latitude()); a.setLongitude(r.longitude());
    if (r.defaultAddress()) addresses.findByUserId(userId).forEach(x -> x.setDefaultAddress(false));
    a.setDefaultAddress(r.defaultAddress());
    return addresses.save(a);
  }
  @Transactional public void deleteAddress(Long userId, Long id) {
    Address a = addresses.findById(id).filter(x -> x.getUser().getId().equals(userId)).orElseThrow(() -> ApiException.notFound("Address not found")); addresses.delete(a);
  }
}
