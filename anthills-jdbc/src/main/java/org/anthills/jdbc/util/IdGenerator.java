package org.anthills.jdbc.util;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * Utility for generating compact, URL-safe identifiers with high entropy.
 *
 * The generated IDs are Base64 URL-safe (no padding) strings derived from
 * 72 bits of entropy (~4.7e21 possibilities), which is collision-safe for
 * very large keyspaces and practical workloads.
 */
public final class IdGenerator {

  private static final SecureRandom RANDOM = new SecureRandom();

  /**
   * Generates a random URL-safe identifier with approximately 72 bits of entropy.
   *
   * Composition:
   * - 64 bits from a UUID's most significant bits
   * - 8 random bits from {@link SecureRandom}
   *
   * The result is encoded using Base64 URL-safe encoding without padding.
   *
   * @return URL-safe random identifier (e.g., "VhK3k1yqVQeJr6QO")
   */
  public static String generateRandomId() {
    UUID uuid = UUID.randomUUID();
    byte[] bytes = new byte[9]; // 72 bits
    long msb = uuid.getMostSignificantBits();
    for (int i = 0; i < 8; i++) {
      bytes[i] = (byte) (msb >>> (8 * (7 - i)));
    }
    bytes[8] = (byte) RANDOM.nextInt();
    return Base64.getUrlEncoder()
      .withoutPadding()
      .encodeToString(bytes);
  }
}
