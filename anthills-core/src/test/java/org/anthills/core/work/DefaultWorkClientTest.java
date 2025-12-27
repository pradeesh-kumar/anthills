package org.anthills.core.work;

import org.anthills.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DefaultWorkClientTest {

  @Mock
  WorkStore store;

  @Mock
  PayloadCodec codec;

  @Test
  void constructorNulls() {
    assertThrows(NullPointerException.class, () -> new DefaultWorkClient(null, codec));
    assertThrows(NullPointerException.class, () -> new DefaultWorkClient(store, null));
  }

  @Test
  void submitUsesDefaultsAndDecodes() {
    DefaultWorkClient client = new DefaultWorkClient(store, codec);

    String workType = "email";
    String payload = "hello";
    int version = SubmissionOptions.defaults().payloadVersion(); // 1
    String codecName = SubmissionOptions.defaults().codec();     // json
    Integer maxAttempts = SubmissionOptions.defaults().maxAttempts(); // null

    byte[] encoded = new byte[]{1, 2, 3};
    when(codec.encode(payload, version)).thenReturn(encoded);

    // WorkRecord returned by store
    WorkRecord record = WorkRecord.builder()
      .id("id-1")
      .workType(workType)
      .payload(encoded)
      .payloadType(String.class.getName())
      .payloadVersion(version)
      .codec(codecName)
      .status(WorkRequest.Status.NEW)
      .attemptCount(0)
      .createdTs(Instant.now())
      .build();

    when(store.createWork(eq(workType), same(encoded), eq(version), eq(codecName), isNull())).thenReturn(record);
    when(codec.decode(encoded, String.class, version)).thenReturn(payload);

    WorkRequest<String> result = client.submit(workType, payload);

    assertNotNull(result);
    assertEquals("id-1", result.id());
    assertEquals(workType, result.workType());
    assertEquals(payload, result.payload());
    assertEquals(version, result.payloadVersion());
    assertEquals(codecName, result.codec());

    // Verify interactions
    verify(codec).encode(payload, version);
    verify(store).createWork(eq(workType), same(encoded), eq(version), eq(codecName), isNull());
    verify(codec).decode(encoded, String.class, version);
  }

  @Test
  void submitWithCustomOptionsForwardsOptions() {
    DefaultWorkClient client = new DefaultWorkClient(store, codec);

    String workType = "task";
    String payload = "p";
    SubmissionOptions options = new SubmissionOptions(2, "bin", 5);

    byte[] encoded = new byte[]{9, 8};
    when(codec.encode(payload, 2)).thenReturn(encoded);

    WorkRecord record = WorkRecord.builder()
      .id("id-2")
      .workType(workType)
      .payload(encoded)
      .payloadType(String.class.getName())
      .payloadVersion(2)
      .codec("bin")
      .status(WorkRequest.Status.NEW)
      .attemptCount(0)
      .createdTs(Instant.now())
      .build();

    when(store.createWork(eq(workType), same(encoded), eq(2), eq("bin"), eq(5))).thenReturn(record);
    when(codec.decode(encoded, String.class, 2)).thenReturn(payload);

    WorkRequest<String> result = client.submit(workType, payload, options);
    assertEquals("id-2", result.id());
    assertEquals(payload, result.payload());

    verify(store).createWork(eq(workType), same(encoded), eq(2), eq("bin"), eq(5));
  }

  @Test
  void submitWrapsEncodeFailure() {
    DefaultWorkClient client = new DefaultWorkClient(store, codec);

    when(codec.encode(any(), anyInt())).thenThrow(new RuntimeException("boom"));

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
      () -> client.submit("x", "payload"));

    assertTrue(ex.getMessage().contains("Failed to encode payload of type java.lang.String"));
    assertNotNull(ex.getCause());
    assertEquals("boom", ex.getCause().getMessage());
    verify(store, never()).createWork(anyString(), any(), anyInt(), anyString(), any());
  }

  @Test
  void submitNullParamChecks() {
    DefaultWorkClient client = new DefaultWorkClient(store, codec);
    assertThrows(NullPointerException.class, () -> client.submit(null, "p"));
    assertThrows(NullPointerException.class, () -> client.submit("type", null));
    assertThrows(NullPointerException.class, () -> client.submit("type", "p", null));
  }

  @Test
  void getReturnsDecodedWorkRequest() {
    DefaultWorkClient client = new DefaultWorkClient(store, codec);

    byte[] encoded = new byte[]{4, 5};
    WorkRecord record = WorkRecord.builder()
      .id("g1")
      .workType("t")
      .payload(encoded)
      .payloadType(String.class.getName())
      .payloadVersion(1)
      .codec("json")
      .status(WorkRequest.Status.NEW)
      .attemptCount(0)
      .createdTs(Instant.now())
      .build();

    when(store.getWork("g1")).thenReturn(Optional.of(record));
    when(codec.decode(encoded, String.class, 1)).thenReturn("value");

    Optional<WorkRequest<String>> result = client.get("g1", String.class);
    assertTrue(result.isPresent());
    assertEquals("value", result.get().payload());
  }

  @Test
  void getEmptyReturnsEmpty() {
    DefaultWorkClient client = new DefaultWorkClient(store, codec);

    when(store.getWork("missing")).thenReturn(Optional.empty());
    Optional<WorkRequest<String>> result = client.get("missing", String.class);
    assertTrue(result.isEmpty());
  }

  @Test
  void getNullParamChecks() {
    DefaultWorkClient client = new DefaultWorkClient(store, codec);
    assertThrows(NullPointerException.class, () -> client.get(null, String.class));
    assertThrows(NullPointerException.class, () -> client.get("id", null));
  }

  @Test
  void listMapsAndDecodes() {
    DefaultWorkClient client = new DefaultWorkClient(store, codec);

    byte[] p1 = new byte[]{1};
    byte[] p2 = new byte[]{2};

    WorkRecord r1 = WorkRecord.builder()
      .id("l1").workType("t").payload(p1).payloadType(Object.class.getName())
      .payloadVersion(1).codec("json").status(WorkRequest.Status.NEW).attemptCount(0).createdTs(Instant.now()).build();

    WorkRecord r2 = WorkRecord.builder()
      .id("l2").workType("t").payload(p2).payloadType(Object.class.getName())
      .payloadVersion(1).codec("json").status(WorkRequest.Status.NEW).attemptCount(0).createdTs(Instant.now()).build();

    WorkQuery q = WorkQuery.defaults("t");
    when(store.listWork(q)).thenReturn(List.of(r1, r2));

    when(codec.decode(p1, Object.class, 1)).thenReturn("s");
    when(codec.decode(p2, Object.class, 1)).thenReturn(42);

    List<WorkRequest<?>> out = client.list(q);
    assertEquals(2, out.size());
    assertEquals("s", out.get(0).payload());
    assertEquals(42, out.get(1).payload());
  }

  @Test
  void listNullParamChecks() {
    DefaultWorkClient client = new DefaultWorkClient(store, codec);
    assertThrows(NullPointerException.class, () -> client.list(null));
  }

  @Test
  void cancelDelegatesToStore() {
    DefaultWorkClient client = new DefaultWorkClient(store, codec);
    client.cancel("id-123");

    ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
    verify(store).markCancelled(idCaptor.capture());
    assertEquals("id-123", idCaptor.getValue());
  }
}
