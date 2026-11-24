package com.farmdirect.controller;

import com.farmdirect.dto.CatalogDtos.*;
import com.farmdirect.entity.*;
import com.farmdirect.exception.ApiException;
import com.farmdirect.repository.*;
import com.farmdirect.service.CatalogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;

@RestController @RequestMapping("/api/public")
public class PublicController {
  private final CatalogService catalog; private final ProductCategoryRepository categories; private final FarmerProfileRepository farmers;
  private final ReviewRepository reviews; private final ComplaintRepository complaints;
  public PublicController(CatalogService catalog, ProductCategoryRepository categories, FarmerProfileRepository farmers, ReviewRepository reviews, ComplaintRepository complaints) {
    this.catalog = catalog; this.categories = categories; this.farmers = farmers; this.reviews = reviews; this.complaints = complaints;
  }

  @GetMapping("/categories") public List<ProductCategory> categories() { return categories.findAll().stream().filter(ProductCategory::isActive).toList(); }

  @GetMapping("/products")
  public Page<ProductDto> products(@RequestParam(required = false) String q, @RequestParam(required = false) Long categoryId, @RequestParam(required = false) BigDecimal minPrice,
      @RequestParam(required = false) BigDecimal maxPrice, @RequestParam(required = false) Long farmerId, @RequestParam(required = false) String state,
      @RequestParam(required = false) String district, @RequestParam(required = false) String pincode, @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "12") int size, @RequestParam(defaultValue = "newest") String sort) {
    Sort s = switch (sort) { case "priceAsc" -> Sort.by("price").ascending(); case "priceDesc" -> Sort.by("price").descending(); default -> Sort.by("id").descending(); };
    return catalog.search(q, categoryId, minPrice, maxPrice, farmerId, state, district, pincode, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 48), s));
  }
  @GetMapping("/products/{id}") public ProductDto product(@PathVariable Long id) { return catalog.get(id); }
  @GetMapping("/products/{id}/reviews") public List<ReviewDto> productReviews(@PathVariable Long id) { return reviews.findByProductIdOrderByIdDesc(id).stream().map(this::review).toList(); }

  @GetMapping("/farmers") public List<FarmerDto> farmers(@RequestParam(required = false) String state, @RequestParam(required = false) String district, @RequestParam(required = false) String pincode) {
    return catalog.farmers(state, district, pincode);
  }
  @GetMapping("/farmers/{id}") public FarmerDto farmer(@PathVariable Long id) { return catalog.toDto(farmers.findById(id).orElseThrow(() -> ApiException.notFound("Farmer not found"))); }
  @GetMapping("/farmers/{id}/reviews") public List<ReviewDto> farmerReviews(@PathVariable Long id) { return reviews.findByFarmerIdOrderByIdDesc(id).stream().map(this::review).toList(); }

  public record ContactRequest(@NotBlank String name, @NotBlank @Email String email, @NotBlank String subject, @NotBlank @Size(max = 2000) String message) {}
  @PostMapping("/contact") public Map<String, String> contact(@Valid @RequestBody ContactRequest r) {
    Complaint c = new Complaint(); c.setContactEmail(r.email()); c.setSubject(r.subject()); c.setDescription(r.name() + ": " + r.message()); complaints.save(c);
    return Map.of("message", "Thanks, we will get back to you.");
  }
  private ReviewDto review(Review r) { return new ReviewDto(r.getId(), r.getCustomer().getFullName(), r.getRating(), r.getComment(), r.getCreatedAt(), r.getProduct() == null ? null : r.getProduct().getId()); }
}
