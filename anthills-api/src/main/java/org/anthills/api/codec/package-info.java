/**
 * Codec SPI for encoding and decoding work payloads.
 *
 * <p>This package defines the {@link org.anthills.api.codec.PayloadCodec} contract used by
 * producers and processors to serialize/deserialize typed payloads for transport and persistence.
 * Implementations should be deterministic, reversible, and support schema evolution via the
 * supplied version argument.</p>
 *
 * <h2>Key concepts</h2>
 * <ul>
 *   <li><b>Versioned payloads</b> — callers pass a semantic schema version when encoding/decoding.</li>
 *   <li><b>Human-readable names</b> — codecs expose a stable {@code name()} for discovery and logging.</li>
 *   <li><b>Failure signaling</b> — serious mapping or data issues should throw {@link org.anthills.api.codec.CodecException}.</li>
 * </ul>
 */
package org.anthills.api.codec;
