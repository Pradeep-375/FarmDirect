package com.farmdirect.service;

import com.farmdirect.dto.CatalogDtos.*;
import com.farmdirect.entity.*;
import com.farmdirect.enums.*;
import com.farmdirect.exception.ApiException;
import com.farmdirect.repository.*;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service
public class CatalogService {
  private final ProductRepository products; private final ProductImageRepository images; private final InventoryRepository inventory;
  private final ProductCategoryRepository categories; private final FarmerProfileRepository farmers; private final ReviewRepository reviews;

  public CatalogService(ProductRepository products, ProductImageRepository images, InventoryRepository inventory, ProductCategoryRepository categories,
                        FarmerProfileRepository farmers, ReviewRepository reviews) {
    this.products = products; this.images = images; this.inventory = inventory; this.categories = categories; this.farmers = farmers; this.reviews = reviews;
  }

  public ProductDto toDto(Product p) {
    FarmerProfile f = p.getFarmer(); User u = f.getUser();
    List<String> urls = images.findByProductIdOrderByPosition(p.getId()).stream().map(ProductImage::getUrl).toList();
    BigDecimal avail = inventory.findByProductId(p.getId()).map(Inventory::getAvailable).orElse(BigDecimal.ZERO);
    return new ProductDto(p.getId(), p.getName(), p.getDescription(), p.getPrice(), p.getUnit(), p.getCategory().getId(), p.getCategory().getName(), avail,
      p.getMinOrderQuantity(), p.getHarvestDate(), p.isFarmerProduced(), p.getSourceNote(), p.isEnabled(), urls, f.getId(), u.getFullName(), f.getFarmName(),
      f.getFarmLocation(), f.getVerificationStatus() == VerificationStatus.APPROVED, u.getState(), u.getDistrict(), f.getRatingAverage());
  }

  public FarmerDto toDto(FarmerProfile f) {
    User u = f.getUser();
    List<String> imgs = f.getFarmImageUrls() == null || f.getFarmImageUrls().isBlank() ? List.of() : Arrays.asList(f.getFarmImageUrls().split(","));
    return new FarmerDto(f.getId(), f.getFarmName(), u.getFullName(), f.getFarmLocation(), f.getFarmAddress(), f.getFarmDetails(), f.getFarmingCategory(),
      f.getProfileImageUrl(), imgs, f.getVerificationStatus().name(), f.getRatingAverage(), f.getRatingCount(), u.getState(), u.getDistrict(), u.getCity());
  }

  @Transactional(readOnly = true)
  public Page<ProductDto> search(String q, Long categoryId, BigDecimal min, BigDecimal max, Long farmerId, String state, String district, String pincode, Pageable pageable) {
    Specification<Product> spec = (root, query, cb) -> {
      List<Predicate> ps = new ArrayList<>();
      Join<Product, FarmerProfile> f = root.join("farmer"); Join<FarmerProfile, User> u = f.join("user");
      ps.add(cb.isFalse(root.get("deleted"))); ps.add(cb.isTrue(root.get("enabled"))); ps.add(cb.equal(u.get("status"), AccountStatus.ACTIVE));
      if (q != null && !q.isBlank()) { String like = "%" + q.trim().toLowerCase() + "%"; ps.add(cb.or(cb.like(cb.lower(root.get("name")), like), cb.like(cb.lower(f.get("farmName")), like))); }
      if (categoryId != null) ps.add(cb.equal(root.get("category").get("id"), categoryId));
      if (min != null) ps.add(cb.greaterThanOrEqualTo(root.get("price"), min));
      if (max != null) ps.add(cb.lessThanOrEqualTo(root.get("price"), max));
      if (farmerId != null) ps.add(cb.equal(f.get("id"), farmerId));
      if (state != null && !state.isBlank()) ps.add(cb.equal(cb.lower(u.get("state")), state.toLowerCase()));
      if (district != null && !district.isBlank()) ps.add(cb.equal(cb.lower(u.get("district")), district.toLowerCase()));
      if (pincode != null && !pincode.isBlank()) ps.add(cb.like(u.get("pincode"), pincode.substring(0, Math.min(3, pincode.length())) + "%"));
      return cb.and(ps.toArray(new Predicate[0]));
    };
    return products.findAll(spec, pageable).map(this::toDto);
  }

  @Transactional(readOnly = true)
  public ProductDto get(Long id) {
    Product p = products.findById(id).filter(x -> !x.isDeleted()).orElseThrow(() -> ApiException.notFound("Product not found"));
    return toDto(p);
  }

  @Transactional(readOnly = true)
  public List<FarmerDto> farmers(String state, String district, String pincode) {
    return farmers.findAll().stream().filter(f -> f.getUser().getStatus() == AccountStatus.ACTIVE)
      .filter(f -> state == null || state.isBlank() || state.equalsIgnoreCase(f.getUser().getState()))
      .filter(f -> district == null || district.isBlank() || district.equalsIgnoreCase(f.getUser().getDistrict()))
      .filter(f -> pincode == null || pincode.length() < 3 || (f.getUser().getPincode() != null && f.getUser().getPincode().startsWith(pincode.substring(0, 3))))
      .map(this::toDto).toList();
  }

  // ---- farmer-side product management ----
  @Transactional
  public ProductDto save(FarmerProfile farmer, Long productId, ProductRequest r) {
    Product p = productId == null ? new Product() : ownedProduct(farmer, productId);
    ProductCategory c = categories.findById(r.categoryId()).filter(ProductCategory::isActive).orElseThrow(() -> ApiException.badRequest("Invalid category"));
    boolean produced = r.farmerProduced() == null || r.farmerProduced();
    if (!produced && (r.sourceNote() == null || r.sourceNote().isBlank())) throw ApiException.badRequest("Describe the source of products not produced on your farm");
    p.setFarmer(farmer); p.setCategory(c); p.setName(r.name().trim()); p.setDescription(r.description()); p.setPrice(r.price()); p.setUnit(r.unit());
    p.setMinOrderQuantity(r.minOrderQuantity() == null ? BigDecimal.ONE : r.minOrderQuantity()); p.setHarvestDate(r.harvestDate());
    p.setFarmerProduced(produced); p.setSourceNote(r.sourceNote());
    p = products.save(p);
    final Product savedProduct = p;
    Inventory inv = inventory.lockByProductId(savedProduct.getId()).orElseGet(() -> { Inventory i = new Inventory(); i.setProduct(products.getReferenceById(savedProduct.getId())); return i; });
    inv.setAvailable(r.stock()); inventory.save(inv);
    if (r.imageUrls() != null) {
      images.deleteByProductId(p.getId());
      int pos = 0;
      for (String url : r.imageUrls()) { ProductImage im = new ProductImage(); im.setProduct(p); im.setUrl(url); im.setPosition(pos++); images.save(im); }
    }
    return toDto(p);
  }

  @Transactional
  public void setStock(FarmerProfile farmer, Long productId, BigDecimal stock) {
    ownedProduct(farmer, productId);
    Inventory inv = inventory.lockByProductId(productId).orElseThrow();
    inv.setAvailable(stock);
  }

  @Transactional
  public void toggle(FarmerProfile farmer, Long id) { Product p = ownedProduct(farmer, id); p.setEnabled(!p.isEnabled()); }

  @Transactional
  public void delete(FarmerProfile farmer, Long id) { Product p = ownedProduct(farmer, id); p.setDeleted(true); p.setEnabled(false); }   // soft delete keeps order history intact

  public Product ownedProduct(FarmerProfile farmer, Long id) {
    Product p = products.findById(id).filter(x -> !x.isDeleted()).orElseThrow(() -> ApiException.notFound("Product not found"));
    if (!p.getFarmer().getId().equals(farmer.getId())) throw ApiException.forbidden("This product belongs to another farmer");
    return p;
  }

  @Transactional(readOnly = true)
  public List<ProductDto> farmerProducts(FarmerProfile f) { return products.findByFarmerIdAndDeletedFalse(f.getId()).stream().map(this::toDto).toList(); }
}
