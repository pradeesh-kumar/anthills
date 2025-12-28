package org.anthills.ui;

/**
 * TLS configuration for the embedded UI.
 * <p>
 * Provide paths to:
 * <ul>
 *   <li>X.509 certificate chain in PEM format (leaf first, followed by intermediates).</li>
 *   <li>PKCS#8 private key in PEM format ({@code -----BEGIN PRIVATE KEY-----}).</li>
 * </ul>
 */
public record TlsOptions(String certificatePemPath, String privateKeyPemPath) {}
