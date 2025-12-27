package org.anthills.core;

import com.google.gson.Gson;
import org.anthills.api.codec.PayloadCodec;

import java.nio.charset.StandardCharsets;

/**
 * {@link PayloadCodec} implementation backed by Gson for JSON serialization.
 *
 * Notes:
 * - The provided {@code version} parameter is currently not used by this codec.
 *   Schema evolution should be handled by payload types in a backward-compatible manner,
 *   or by introducing a new codec/name if necessary.
 * - This codec uses UTF-8 for byte representation.
 */
public class JsonPayloadCodec implements PayloadCodec {

  private final Gson gson = new Gson();

  /**
   * Creates a new default, stateless {@code JsonPayloadCodec} instance.
   *
   * @return a new codec instance
   */
  public static JsonPayloadCodec defaultInstance() {
    return new JsonPayloadCodec();
  }

  @Override
  /**
   * Returns the human-readable codec name.
   *
   * @return {@code "json"}
   */
  public String name() {
    return "json";
  }

  @Override
  /**
   * Encodes the given payload to JSON using Gson and returns UTF-8 bytes.
   *
   * @param payload object to serialize (must not be null)
   * @param version payload schema version (ignored by this codec)
   * @param <T> payload type
   * @return UTF-8 JSON bytes
   * @throws IllegalArgumentException if serialization fails
   */
  public <T> byte[] encode(T payload, int version) {
    try {
      return gson.toJson(payload, payload.getClass()).getBytes(StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to encode payload of type " + (payload != null ? payload.getClass().getName() : "null"), e);
    }
  }

  @Override
  /**
   * Decodes the given UTF-8 JSON bytes into the requested type.
   *
   * @param data UTF-8 JSON bytes
   * @param type target class
   * @param version payload schema version (ignored by this codec)
   * @param <T> target type
   * @return deserialized instance of {@code type}
   * @throws IllegalArgumentException if deserialization fails
   */
  public <T> T decode(byte[] data, Class<T> type, int version) {
    try {
      return gson.fromJson(new String(data, StandardCharsets.UTF_8), type);
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to decode payload to type " + (type != null ? type.getName() : "null"), e);
    }
  }
}
