package com.farmdirect.dto;

import com.farmdirect.enums.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class CatalogDtos {
  private CatalogDtos() {}
  public record ProductRequest(@NotBlank @Size(max = 150) String name, @Size(max = 2000) String description,
    @NotNull @DecimalMin("0.01") BigDecimal price, @NotNull Unit unit, @NotNull Long categoryId,
    @NotNull @DecimalMin("0") BigDecimal stock, @DecimalMin("0.001") BigDecimal minOrderQuantity,
    LocalDate harvestDate, Boolean farmerProduced, String sourceNote, List<String> imageUrls) {}
  public record ProductDto(Long id, String name, String description, BigDecimal price, Unit unit, Long categoryId, String categoryName,
    BigDecimal available, BigDecimal minOrderQuantity, LocalDate harvestDate, boolean farmerProduced, String sourceNote, boolean enabled,
    List<String> imageUrls, Long farmerId, String farmerName, String farmName, String farmLocation, boolean farmerVerified, String state, String district,
    double farmerRating) {}
  public record FarmerDto(Long id, String farmName, String farmerName, String farmLocation, String farmAddress, String farmDetails, String farmingCategory,
    String profileImageUrl, List<String> farmImages, String verificationStatus, double ratingAverage, int ratingCount, String state, String district, String city) {}
  public record FarmerProfileRequest(@NotBlank String farmName, @NotBlank String farmAddress, String farmLocation, Double latitude, Double longitude,
    @NotBlank String farmingCategory, String farmDetails, String profileImageUrl, List<String> farmImages,
    String state, String district, String city, String pincode) {}
  public record StockRequest(@NotNull @DecimalMin("0") BigDecimal stock) {}
  public record VerificationDocRequest(@NotBlank String documentType, @NotBlank String documentUrl) {}
  public record CategoryRequest(@NotBlank String name, String description, Boolean active) {}
  public record ReviewDto(Long id, String customerName, int rating, String comment, java.time.Instant createdAt, Long productId) {}
}
