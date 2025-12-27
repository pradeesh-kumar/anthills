package org.anthills.api.codec;

/**
 * Contract for encoding and decoding work payloads for persistence.
 * Implementations must be deterministic across versions and reversible with decode.
 */
public interface PayloadCodec {

  /**
   * Human-readable identifier for this codec (e.g., "json", "smile").
   * Used for discovery and logging and should be stable across versions.
   *
   * @return codec name
   */
  String name();

  /**
   * Encodes the given payload into a byte array using the requested schema/version.
   *
   * @param payload domain object to encode; may be null if the codec supports it
   * @param version semantic schema version to encode with; used for forward/backward compatibility
   * @param <T> type of the payload being encoded
   * @return serialized bytes
   * @throws CodecException if the payload cannot be encoded
   */
  <T> byte[] encode(T payload, int version);

  /**
   * Decodes the given bytes into the target type using the requested schema/version.
   *
   * @param data serialized bytes
   * @param type target class to materialize
   * @param version semantic schema version to decode with
   * @param <T> type of the decoded object
   * @return decoded object
   * @throws CodecException if the data cannot be decoded or mapped to the target type
   */
  <T> T decode(byte[] data, Class<T> type, int version);
}
