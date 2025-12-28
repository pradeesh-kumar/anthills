package org.anthills.ui;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultAnthillsUI implements AnthillsUI {

  private static final Logger log = LoggerFactory.getLogger(DefaultAnthillsUI.class);

  private final Options options;
  private final RouteHandler routeHandler;

  private HttpServer server;
  private ExecutorService executor;
  private final AtomicBoolean running = new AtomicBoolean(false);

  DefaultAnthillsUI(Options options) {
    this.options = options;
    this.routeHandler = new RouteHandler(options.workStore());
  }

  @Override
  public void start() {
    if (!running.compareAndSet(false, true)) return;
    try {
      server = createServer();
      executor = Executors.newFixedThreadPool(options.threads());
      server.setExecutor(executor);
      registerRoutes();
      server.start();
      String scheme = (options.tls() != null ? "https" : "http");
      log.info("Anthills UI started at {}://{}:{}{}", scheme, options.bindAddress(), options.port(), options.contextPath());
    } catch (Exception e) {
      throw new RuntimeException("Failed to start Anthills UI", e);
    }
  }

  @Override
  public void stop() {
    if (!running.compareAndSet(true, false)) return;
    server.stop(0);
    executor.shutdownNow();
  }

  @Override
  public boolean isRunning() {
    return running.get();
  }

  private void registerRoutes() {
    server.createContext(options.contextPath(), this::handle);
  }

  private void handle(HttpExchange exchange) throws IOException {
    String path = exchange.getRequestURI().getPath();
    log.debug("Received request for path {}", path);
    if (options.auth() != null && !options.auth().authorize(exchange)) {
      return;
    }
    if (path.equals("/") || path.isEmpty()) {
      if (!"GET".equals(exchange.getRequestMethod())) {
        routeHandler.error(exchange, 405, "Method not allowed");
        return;
      }
      routeHandler.handleDashboard(exchange);
      return;
    }
    if (path.equals("/work")) {
      if (!"GET".equals(exchange.getRequestMethod())) {
        routeHandler.error(exchange, 405, "Method not allowed");
        return;
      }
      routeHandler.handleWork(exchange);
      return;
    }
    if (path.startsWith("/work/")) {
      if (!"GET".equals(exchange.getRequestMethod())) {
        routeHandler.error(exchange, 405, "Method not allowed");
        return;
      }
      String id =  path.substring("/work/".length());
      if (id.isEmpty() || id.contains("/")) {
        routeHandler.error(exchange, 404, "Not found");
        return;
      }
      routeHandler.handleWorkDetails(exchange, id);
      return;
    }
    if (path.equals("/scheduler")) {
      if (!"GET".equals(exchange.getRequestMethod())) {
        routeHandler.error(exchange, 405, "Method not allowed");
        return;
      }
      routeHandler.handleScheduler(exchange);
      return;
    }
    routeHandler.error(exchange, 404, "Not Found");
  }

  private HttpServer createServer() throws Exception {
    InetSocketAddress addr = new InetSocketAddress(options.bindAddress(), options.port());
    if (options.tls() == null) {
      return HttpServer.create(addr, 0);
    }
    SSLContext ctx = buildSslContext(options.tls());
    HttpsServer httpsServer = HttpsServer.create(addr, 0);
    httpsServer.setHttpsConfigurator(new HttpsConfigurator(ctx) {
      @Override
      public void configure(HttpsParameters params) {
        SSLParameters sslParams = ctx.getDefaultSSLParameters();
        params.setSSLParameters(sslParams);
      }
    });
    return httpsServer;
  }

  private SSLContext buildSslContext(TlsOptions tls) throws Exception {
    String certPem = Files.readString(Path.of(tls.certificatePemPath()));
    String keyPem = Files.readString(Path.of(tls.privateKeyPemPath()));
    X509Certificate[] chain = parseCertificates(certPem);
    PrivateKey key = parsePrivateKey(keyPem);

    KeyStore ks = KeyStore.getInstance("JKS");
    ks.load(null, null);
    char[] pass = "changeit".toCharArray();
    ks.setKeyEntry("server", key, pass, chain);

    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(ks, pass);

    SSLContext ctx = SSLContext.getInstance("TLS");
    ctx.init(kmf.getKeyManagers(), null, new SecureRandom());
    return ctx;
  }

  private static X509Certificate[] parseCertificates(String pem) throws Exception {
    List<X509Certificate> certs = new ArrayList<>();
    CertificateFactory cf = CertificateFactory.getInstance("X.509");
    String start = "-----BEGIN CERTIFICATE-----";
    String end = "-----END CERTIFICATE-----";
    int idx = 0;
    while (true) {
      int s = pem.indexOf(start, idx);
      if (s == -1) break;
      int e = pem.indexOf(end, s);
      if (e == -1) break;
      String base64 = pem.substring(s + start.length(), e).replaceAll("\\s+", "");
      byte[] der = Base64.getDecoder().decode(base64);
      X509Certificate cert = (X509Certificate) cf.generateCertificate(new java.io.ByteArrayInputStream(der));
      certs.add(cert);
      idx = e + end.length();
    }
    if (certs.isEmpty()) {
      throw new IllegalArgumentException("No X.509 certificates found in certificate PEM");
    }
    return certs.toArray(new X509Certificate[0]);
  }

  private static PrivateKey parsePrivateKey(String pem) throws Exception {
    String base64 = getBase64(pem);
    byte[] der = Base64.getDecoder().decode(base64);
    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);

    Exception last = null;
    for (String alg : new String[] {"RSA", "EC", "DSA"}) {
      try {
        return KeyFactory.getInstance(alg).generatePrivate(spec);
      } catch (Exception ex) {
        last = ex;
      }
    }
    throw new RuntimeException("Unsupported private key algorithm in PKCS#8", last);
  }

  private static String getBase64(String pem) {
    String pkcs8Header = "-----BEGIN PRIVATE KEY-----";
    String pkcs8Footer = "-----END PRIVATE KEY-----";
    String pkcs1Header = "-----BEGIN RSA PRIVATE KEY-----";
    if (pem.contains(pkcs1Header)) {
      throw new IllegalArgumentException("PKCS#1 (BEGIN RSA PRIVATE KEY) format not supported. Provide PKCS#8 (BEGIN PRIVATE KEY).");
    }
    int s = pem.indexOf(pkcs8Header);
    int e = pem.indexOf(pkcs8Footer);
    if (s == -1 || e == -1) {
      throw new IllegalArgumentException("Private key must be in PKCS#8 PEM format (BEGIN PRIVATE KEY)");
    }
    String base64 = pem.substring(s + pkcs8Header.length(), e).replaceAll("\\s+", "");
    return base64;
  }

  public static AnthillsUIBuilder builder() {
    return new DefaultAnthillsUIBuilder();
  }

}
