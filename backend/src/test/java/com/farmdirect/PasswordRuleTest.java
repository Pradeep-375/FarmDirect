package com.farmdirect;

import com.farmdirect.dto.AuthDtos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordRuleTest {
  private boolean ok(String p) { return p.matches(AuthDtos.PASSWORD_RULE); }
  @Test void acceptsStrongPassword() { assertTrue(ok("Farm#Direct2026!")); }
  @Test void rejectsShort() { assertFalse(ok("Ab1!short")); }
  @Test void rejectsMissingClasses() {
    assertFalse(ok("alllowercase1234!")); assertFalse(ok("ALLUPPERCASE1234!")); assertFalse(ok("NoNumbersHere!!!!")); assertFalse(ok("NoSpecials12345Ab"));
  }
}
