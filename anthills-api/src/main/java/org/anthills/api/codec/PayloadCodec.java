package org.anthills.api.codec;

public interface PayloadCodec {
  String name();
  <T> byte[] encode(T payload, int version);
  <T> T decode(byte[] data, Class<T> type, int version);
}
