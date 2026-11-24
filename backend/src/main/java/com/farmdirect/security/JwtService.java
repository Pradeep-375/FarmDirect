package com.farmdirect.security;

import com.farmdirect.enums.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
  private final SecretKey key;
  private final long accessMinutes;

  public JwtService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.access-minutes}") long accessMinutes) {
    if (secret == null || secret.length() < 32) throw new IllegalStateException("JWT_SECRET must be at least 32 characters");
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.accessMinutes = accessMinutes;
  }

  public String createAccessToken(Long userId, Role role) {
    Instant now = Instant.now();
    return Jwts.builder().subject(String.valueOf(userId)).claim("role", role.name())
        .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(accessMinutes * 60))).signWith(key).compact();
  }

  /** Returns null when the token is invalid or expired. */
  public AuthUser parse(String token) {
    try {
      Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
      return new AuthUser(Long.valueOf(c.getSubject()), Role.valueOf(c.get("role", String.class)));
    } catch (JwtException | IllegalArgumentException e) { return null; }
  }
}
