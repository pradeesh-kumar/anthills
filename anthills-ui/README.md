# Anthills UI

Embedded HTTP/HTTPS UI server for viewing Anthills work items and scheduler state.

This module provides a small server you can embed in your application process to inspect and manage work. It supports:
- Basic auth (optional)
- Custom bind address and port
- Adjustable worker threads
- Optional HTTPS using PEM certificate and private key via a fluent `.tls(...)` builder method

## Usage

### Minimal HTTP example
```java
WorkStore store = ...;

AnthillsUI ui = AnthillsUI.builder()
  .workStore(store)
  .port(8080)
  .bindAddress("0.0.0.0")
  .contextPath("/")
  .threads(4)
  // Optional auth
  .basicAuth("admin", "admin")
  .build();

ui.start();
// ...
// ui.stop();
```

The server logs the URL on startup, for example:
```
INFO Anthills UI started at http://0.0.0.0:8080/
```

### Enable HTTPS
Provide a certificate (X.509 in PEM) and a private key in PKCS#8 PEM format.

```java
WorkStore store = ...;

AnthillsUI ui = AnthillsUI.builder()
  .workStore(store)
  .port(8443)
  .bindAddress("0.0.0.0")
  .threads(4)
  .basicAuth("admin", "admin") // optional
  .tls("/path/to/cert.pem", "/path/to/server-key-pkcs8.pem")
  .build();

ui.start();
```

On startup you’ll see:
```
INFO Anthills UI started at https://0.0.0.0:8443/
```

Notes:
- Certificate (cert.pem): must be X.509 PEM. You can include the full chain in the same file. The end-entity (leaf) certificate should appear first, followed by any intermediates.
- Private key (server-key-pkcs8.pem): must be PKCS#8 PEM, i.e. it should contain:
  ```
  -----BEGIN PRIVATE KEY-----
  ...
  -----END PRIVATE KEY-----
  ```
  Keys in PKCS#1 format (BEGIN RSA PRIVATE KEY) are not supported – convert them to PKCS#8 (see below).

### Converting keys and generating dev certificates

Generate a self-signed certificate for localhost (for development only):
```bash
# Generate key and certificate
openssl req -x509 -newkey rsa:2048 -keyout key.pem -out cert.pem -days 365 -nodes -subj "/CN=localhost"

# Convert key to PKCS#8 (required)
openssl pkcs8 -topk8 -inform PEM -outform PEM -in key.pem -out key-pkcs8.pem -nocrypt
```

Use `cert.pem` and `key-pkcs8.pem` with `.tls("cert.pem", "key-pkcs8.pem")`.

## API

```java
public interface AnthillsUIBuilder {
  AnthillsUIBuilder port(int port);
  AnthillsUIBuilder bindAddress(String address); // default: "localhost"
  AnthillsUIBuilder workStore(WorkStore store);
  AnthillsUIBuilder contextPath(String path); // default: "/"
  AnthillsUIBuilder enableWriteActions(boolean enabled); // default: false
  AnthillsUIBuilder basicAuth(String username, String password);
  AnthillsUIBuilder threads(int threads);
  AnthillsUIBuilder tls(String certificatePemPath, String privateKeyPemPath); // Enable HTTPS
  AnthillsUI build();
}
```

## Implementation details

- If `.tls(...)` is provided, an `HttpsServer` is created and configured using an in-memory KeyStore populated from:
  - The provided PEM certificate(s) as the certificate chain
  - The provided PKCS#8 private key
- If `.tls(...)` is not provided, a plain `HttpServer` is created.
- Log output indicates `http` or `https` accordingly.
- Stopping the UI shuts down the server and its thread pool.

## Security considerations

- For production, ensure you supply a valid certificate chain and a properly secured private key file.
- Consider running the UI behind a reverse proxy/ingress that handles TLS termination and additional security policies if that better fits your operational requirements.
- Bind the server to the appropriate interface for your environment (e.g., `127.0.0.1` for local-only access).
