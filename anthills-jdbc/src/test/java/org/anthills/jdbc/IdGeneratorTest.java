package org.anthills.jdbc;

import org.anthills.jdbc.util.IdGenerator;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

final class IdGeneratorTest {

  private static final Pattern URL_SAFE_12_CHARS = Pattern.compile("^[A-Za-z0-9_-]{12}$");

  @Test
  void generates_url_safe_12_char_without_padding() {
    String id = IdGenerator.generateRandomId();
    assertNotNull(id);
    assertEquals(12, id.length(), "Expected 12 chars for 9 bytes base64url (no padding)");
    assertTrue(URL_SAFE_12_CHARS.matcher(id).matches(), "Should be URL-safe base64 without padding");
  }

  @Test
  void high_uniqueness_over_many_generations() {
    int n = 5000;
    Set<String> seen = new HashSet<>(n * 2);
    for (int i = 0; i < n; i++) {
      String id = IdGenerator.generateRandomId();
      assertTrue(URL_SAFE_12_CHARS.matcher(id).matches(), "Each id should be URL-safe");
      boolean added = seen.add(id);
      if (!added) {
        fail("Duplicate id generated: " + id);
      }
    }
    assertEquals(n, seen.size());
  }
}
