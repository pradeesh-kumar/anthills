package org.anthills.ui;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

class BasicAuthTest {

  @Test
  void missingHeader_sendsChallengeAndReturnsFalse() throws Exception {
    Headers req = new Headers();
    Headers resp = new Headers();
    AtomicInteger status = new AtomicInteger(Integer.MIN_VALUE);
    AtomicBoolean closed = new AtomicBoolean(false);

    HttpExchange ex = Mockito.mock(HttpExchange.class);
    when(ex.getRequestHeaders()).thenReturn(req);
    when(ex.getResponseHeaders()).thenReturn(resp);
    doAnswer(inv -> {
      status.set((int) inv.getArgument(0));
      return null;
    }).when(ex).sendResponseHeaders(anyInt(), anyLong());
    doAnswer(inv -> {
      closed.set(true);
      return null;
    }).when(ex).close();

    BasicAuth auth = new BasicAuth("user", "pass", "realm1");
    boolean result = auth.authorize(ex);

    assertFalse(result, "authorize should return false");
    assertEquals(401, status.get(), "Should respond with 401");
    assertTrue(resp.getFirst("WWW-Authenticate").contains("Basic realm=\"realm1\""));
    assertTrue(closed.get(), "Exchange should be closed");
  }

  @Test
  void wrongScheme_sendsChallengeAndReturnsFalse() throws Exception {
    Headers req = new Headers();
    req.add("Authorization", "Bearer abc");
    Headers resp = new Headers();
    AtomicInteger status = new AtomicInteger(Integer.MIN_VALUE);
    AtomicBoolean closed = new AtomicBoolean(false);

    HttpExchange ex = Mockito.mock(HttpExchange.class);
    when(ex.getRequestHeaders()).thenReturn(req);
    when(ex.getResponseHeaders()).thenReturn(resp);
    doAnswer(inv -> {
      status.set((int) inv.getArgument(0));
      return null;
    }).when(ex).sendResponseHeaders(anyInt(), anyLong());
    doAnswer(inv -> {
      closed.set(true);
      return null;
    }).when(ex).close();

    BasicAuth auth = new BasicAuth("user", "pass", "realm2");
    boolean result = auth.authorize(ex);

    assertFalse(result);
    assertEquals(401, status.get());
    assertTrue(resp.getFirst("WWW-Authenticate").contains("Basic realm=\"realm2\""));
    assertTrue(closed.get());
  }

  @Test
  void validHeader_returnsTrueAndNoChallenge() throws Exception {
    String creds = Base64.getEncoder().encodeToString("user:pass".getBytes(StandardCharsets.UTF_8));
    Headers req = new Headers();
    req.add("Authorization", "Basic " + creds);
    Headers resp = new Headers();
    AtomicInteger status = new AtomicInteger(Integer.MIN_VALUE);
    AtomicBoolean closed = new AtomicBoolean(false);

    HttpExchange ex = Mockito.mock(HttpExchange.class);
    when(ex.getRequestHeaders()).thenReturn(req);
    when(ex.getResponseHeaders()).thenReturn(resp);
    doAnswer(inv -> {
      status.set((int) inv.getArgument(0));
      return null;
    }).when(ex).sendResponseHeaders(anyInt(), anyLong());
    doAnswer(inv -> {
      closed.set(true);
      return null;
    }).when(ex).close();

    BasicAuth auth = new BasicAuth("user", "pass", "realm");
    boolean result = auth.authorize(ex);

    assertTrue(result, "authorize should return true");
    assertEquals(Integer.MIN_VALUE, status.get(), "Should not set a 401 status");
    assertNull(resp.getFirst("WWW-Authenticate"), "No authenticate header should be set");
    assertFalse(closed.get(), "Exchange should not be closed");
  }
}
