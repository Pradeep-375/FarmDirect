package com.farmdirect.service;

import com.farmdirect.dto.AuthDtos.*;
import com.farmdirect.entity.*;
import com.farmdirect.enums.*;
import com.farmdirect.exception.ApiException;
import com.farmdirect.repository.*;
import com.farmdirect.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;

@Service
public class AuthService {
  private static final int MAX_FAILED = 5;
  private static final long LOCK_SECONDS = 15 * 60;

  private final UserRepository users; private final FarmerProfileRepository farmers; private final CustomerProfileRepository customers;
  private final DeliveryProfileRepository deliveries; private final RefreshTokenRepository refreshTokens;
  private final PasswordEncoder encoder; private final JwtService jwt;
  private final SecureRandom random = new SecureRandom();
  private final String dummyHash;   // compared against when the account does not exist, so response time does not reveal it
  @Value("${app.jwt.refresh-days}") long refreshDays;

  public AuthService(UserRepository users, FarmerProfileRepository farmers, CustomerProfileRepository customers, DeliveryProfileRepository deliveries,
                     RefreshTokenRepository refreshTokens, PasswordEncoder encoder, JwtService jwt) {
    this.users = users; this.farmers = farmers; this.customers = customers; this.deliveries = deliveries;
    this.refreshTokens = refreshTokens; this.encoder = encoder; this.jwt = jwt;
    this.dummyHash = encoder.encode("dummy-password-for-timing");
  }

  public static String normalizeMobile(String m) {
    String d = m.replaceAll("[^0-9]", "");
    if (d.length() == 12 && d.startsWith("91")) d = d.substring(2);
    if (d.length() == 11 && d.startsWith("0")) d = d.substring(1);
    return d;
  }

  @Transactional
  public TokenResponse register(RegisterRequest r) {
    if (r.role() == Role.ADMIN) throw ApiException.forbidden("Administrator accounts cannot be self-registered");
    if (!r.password().equals(r.confirmPassword())) throw ApiException.badRequest("Passwords do not match");
    if (r.role() == Role.FARMER && (blank(r.farmName()) || blank(r.farmAddress()) || blank(r.farmingCategory())))
      throw ApiException.badRequest("Farm name, farm address and farming category are required for farmers");
    String mobile = normalizeMobile(r.mobile()), email = r.email().trim().toLowerCase();
    if (users.existsByEmailIgnoreCase(email)) throw ApiException.conflict("An account with this email already exists");
    if (users.existsByMobile(mobile)) throw ApiException.conflict("An account with this mobile number already exists");
    User u = new User();
    u.setFullName(r.fullName().trim()); u.setEmail(email); u.setMobile(mobile); u.setPasswordHash(encoder.encode(r.password())); u.setRole(r.role());
    u.setState(r.state()); u.setDistrict(r.district()); u.setCity(r.city()); u.setAddress(r.address()); u.setPincode(r.pincode());
    users.save(u);
    switch (r.role()) {
      case FARMER -> { FarmerProfile f = new FarmerProfile(); f.setUser(u); f.setFarmName(r.farmName()); f.setFarmAddress(r.farmAddress());
        f.setFarmLocation(r.farmLocation()); f.setFarmingCategory(r.farmingCategory()); f.setFarmDetails(r.farmDetails()); farmers.save(f); }
      case CUSTOMER -> { CustomerProfile c = new CustomerProfile(); c.setUser(u); customers.save(c); }
      case DELIVERY_PARTNER -> { DeliveryProfile d = new DeliveryProfile(); d.setUser(u); d.setVehicleType(r.vehicleType()); d.setVehicleNumber(r.vehicleNumber()); deliveries.save(d); }
      default -> {}
    }
    return issueTokens(u, UUID.randomUUID().toString());
  }

  /** Password login with a generic error and per-account lockout (5 failures -> 15 minutes). Failed counts persist despite the exception. */
  @Transactional(noRollbackFor = ApiException.class)
  public TokenResponse login(LoginRequest r) {
    ApiException bad = new ApiException(HttpStatus.UNAUTHORIZED, "Incorrect email/mobile number or password");
    String id = r.identifier().trim();
    Optional<User> ou = id.contains("@") ? users.findByEmailIgnoreCase(id) : users.findByMobile(normalizeMobile(id));
    if (ou.isEmpty()) { encoder.matches(r.password(), dummyHash); throw bad; }
    User u = ou.get();
    if (u.getLockedUntil() != null && u.getLockedUntil().isAfter(Instant.now()))
      throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many failed attempts. Try again in a few minutes.");
    if (!encoder.matches(r.password(), u.getPasswordHash())) {
      u.setFailedLogins(u.getFailedLogins() + 1);
      if (u.getFailedLogins() >= MAX_FAILED) { u.setLockedUntil(Instant.now().plusSeconds(LOCK_SECONDS)); u.setFailedLogins(0); }
      throw bad;
    }
    if (u.getStatus() != AccountStatus.ACTIVE) throw new ApiException(HttpStatus.FORBIDDEN, "This account is suspended. Contact support.");
    u.setFailedLogins(0); u.setLockedUntil(null);
    return issueTokens(u, UUID.randomUUID().toString());
  }

  /** Rotating refresh tokens with reuse detection: replaying a used token revokes the whole token family. */
  @Transactional(noRollbackFor = ApiException.class)
  public TokenResponse refresh(String token) {
    ApiException unauthorized = new ApiException(HttpStatus.UNAUTHORIZED, "Session expired. Please log in again.");
    RefreshToken t = refreshTokens.findByTokenHash(sha256(token)).orElseThrow(() -> unauthorized);
    if (t.isRevoked()) { refreshTokens.revokeFamily(t.getFamilyId()); throw unauthorized; }
    if (t.getExpiresAt().isBefore(Instant.now()) || t.getUser().getStatus() != AccountStatus.ACTIVE) { t.setRevoked(true); throw unauthorized; }
    t.setRevoked(true);
    return issueTokens(t.getUser(), t.getFamilyId());
  }

  @Transactional public void logout(String token) { refreshTokens.findByTokenHash(sha256(token)).ifPresent(t -> refreshTokens.revokeFamily(t.getFamilyId())); }
  @Transactional public void logoutAll(Long userId) { refreshTokens.revokeAllForUser(userId); }

  private TokenResponse issueTokens(User u, String family) {
    byte[] raw = new byte[32]; random.nextBytes(raw);
    String refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    RefreshToken t = new RefreshToken();
    t.setUser(u); t.setTokenHash(sha256(refresh)); t.setFamilyId(family); t.setExpiresAt(Instant.now().plusSeconds(refreshDays * 86400));
    refreshTokens.save(t);
    return new TokenResponse(jwt.createAccessToken(u.getId(), u.getRole()), refresh, u.getRole(), u.getId(), u.getFullName());
  }
  private static boolean blank(String s) { return s == null || s.isBlank(); }
  static String sha256(String s) {
    try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8))); }
    catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
  }
}
