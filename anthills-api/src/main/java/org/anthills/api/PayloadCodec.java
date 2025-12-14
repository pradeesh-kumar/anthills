package org.anthills.api;

public interface PayloadCodec {
    <T> byte[] encode(T payload);
    <T> T decode(byte[] data, Class<T> type);
}
