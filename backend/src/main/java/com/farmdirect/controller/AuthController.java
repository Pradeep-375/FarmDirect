package com.farmdirect.controller;

import com.farmdirect.dto.AuthDtos.*;
import com.farmdirect.security.Current;
import com.farmdirect.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api/auth")
public class AuthController {
  private final AuthService auth;
  public AuthController(AuthService auth) { this.auth = auth; }

  @PostMapping("/register") public TokenResponse register(@Valid @RequestBody RegisterRequest r) { return auth.register(r); }
  @PostMapping("/login") public TokenResponse login(@Valid @RequestBody LoginRequest r) { return auth.login(r); }
  @PostMapping("/refresh") public TokenResponse refresh(@Valid @RequestBody RefreshRequest r) { return auth.refresh(r.refreshToken()); }
  @PostMapping("/logout") public Map<String, String> logout(@Valid @RequestBody RefreshRequest r) { auth.logout(r.refreshToken()); return Map.of("message", "Logged out"); }
  @PostMapping("/logout-all") public Map<String, String> logoutAll() { auth.logoutAll(Current.id()); return Map.of("message", "Logged out everywhere"); }
}
