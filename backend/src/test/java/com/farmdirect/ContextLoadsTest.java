package com.farmdirect;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Boots the full context against in-memory H2 (schema generation, repository query validation, security wiring). */
@SpringBootTest @ActiveProfiles("test")
class ContextLoadsTest {
  @Test void contextLoads() {}
}
