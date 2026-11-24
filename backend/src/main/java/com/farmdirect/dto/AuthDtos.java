package com.farmdirect.dto;

import com.farmdirect.enums.Role;
import jakarta.validation.constraints.*;

public final class AuthDtos {
  private AuthDtos() {}
  public static final String PASSWORD_RULE = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{12,}$";
  public static final String PASSWORD_MSG = "Password must be at least 12 characters with uppercase, lowercase, a number and a special character";

  public record RegisterRequest(
    @NotBlank @Size(max = 120) String fullName,
    @NotBlank @Email String email,
    @NotBlank @Pattern(regexp = "^(\\+91|91|0)?[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number") String mobile,
    @NotBlank @Pattern(regexp = PASSWORD_RULE, message = PASSWORD_MSG) String password,
    @NotBlank String confirmPassword,
    @NotNull Role role,
    @NotBlank String state, @NotBlank String district, @NotBlank String city, @NotBlank String address,
    @NotBlank @Pattern(regexp = "^[1-9]\\d{5}$", message = "Enter a valid 6-digit PIN code") String pincode,
    String farmName, String farmAddress, String farmLocation, String farmingCategory, String farmDetails,
    String vehicleType, String vehicleNumber) {}

  /** identifier = registered email address or 10-digit mobile number. */
  public record LoginRequest(@NotBlank String identifier, @NotBlank String password) {}
  public record RefreshRequest(@NotBlank String refreshToken) {}
  public record TokenResponse(String accessToken, String refreshToken, Role role, Long userId, String fullName) {}
}
