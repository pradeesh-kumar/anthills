package org.anthills.jdbc.util;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

public final class IdGenerator {

  private static final SecureRandom RANDOM = new SecureRandom();

  // ~72 bits entropy → collision-safe for billions of IDs
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
