package org.anthills.jdbc;

import org.anthills.api.PayloadCodec;

public class JsonPayloadCodec implements PayloadCodec {

  public static JsonPayloadCodec defaultInstance() {
    return new JsonPayloadCodec();
  }

  @Override
  public String name() {
    return "json";
  }

  @Override
  public <T> byte[] encode(T payload, int version) {
    return new byte[0];
  }

  @Override
  public <T> T decode(byte[] data, Class<T> type, int version) {
    switch (version) {
      case 1 -> ...
      default -> throw new UnsupportedVersionException(version);
    }
    return null;
  }
}
