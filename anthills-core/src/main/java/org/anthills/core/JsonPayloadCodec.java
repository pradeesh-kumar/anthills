package org.anthills.core;

import com.google.gson.Gson;
import org.anthills.api.codec.PayloadCodec;

import java.nio.charset.StandardCharsets;

public class JsonPayloadCodec implements PayloadCodec {

  private final Gson gson = new Gson();

  public static JsonPayloadCodec defaultInstance() {
    return new JsonPayloadCodec();
  }

  @Override
  public String name() {
    return "json";
  }

  @Override
  public <T> byte[] encode(T payload, int version) {
    return gson.toJson(payload, payload.getClass()).getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public <T> T decode(byte[] data, Class<T> type, int version) {
    return gson.fromJson(new String(data, StandardCharsets.UTF_8), type);
  }
}
