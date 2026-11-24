package com.farmdirect;

import com.farmdirect.enums.Role;
import com.farmdirect.security.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
  @Test void roundTripAndRejectsGarbage() {
    JwtService j = new JwtService("0123456789012345678901234567890123456789", 15);
    AuthUser u = j.parse(j.createAccessToken(42L, Role.FARMER));
    assertEquals(42L, u.id()); assertEquals(Role.FARMER, u.role());
    assertNull(j.parse("not-a-token"));
    assertNull(new JwtService("ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ", 15).parse(j.createAccessToken(1L, Role.ADMIN)));
  }
  @Test void shortSecretIsRejected() { assertThrows(IllegalStateException.class, () -> new JwtService("short", 15)); }
}
