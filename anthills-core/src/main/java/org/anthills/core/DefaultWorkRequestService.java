package org.anthills.core;

import org.anthills.api.WorkRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class DefaultWorkRequestService implements WorkRequestService {

  private static final Logger log = LoggerFactory.getLogger(DefaultWorkRequestService.class);

  private final WorkRequestRepository wrRepo;

  public DefaultWorkRequestService(WorkRequestRepository wrRepo) {
    this.wrRepo = wrRepo;
  }

  @Override
  @Transactional
  public <T> WorkRequest<T> create(T payload, int maxRetries) {
    Objects.requireNonNull(payload, "payload is required");
    if (maxRetries <= 0) {
      throw new IllegalArgumentException("maxRetries must be a positive integer");
    }
    Instant now = Instant.now();
    WorkRequest<T> toCreate = WorkRequest.<T>builder()
      .setId(IdGenerator.generateRandomId())
      .setPayloadClass(payload.getClass().getName())
      .setPayload(payload)
      .setStatus(WorkRequest.Status.New)
      .setMaxRetries(maxRetries)
      .setCreatedTs(now)
      .setUpdatedTs(now)
      .build();
    return wrRepo.create(toCreate);
  }

  @Override
  public void markSucceeded(WorkRequest<?> wr) {
    if (!wrRepo.exists(wr.id())) {
      throw new IllegalArgumentException("WorkRequest does not exist: " + wr.id());
    }
    if (!wrRepo.updateStatus(wr.id(), WorkRequest.Status.Succeeded)) {
      throw new IllegalArgumentException("WorkRequest " + wr.id() + " already Succeeded");
    }
  }

  @Override
  public void markFailedOrRetry(WorkRequest<?> wr) {
    if (!wrRepo.exists(wr.id())) {
      throw new IllegalArgumentException("WorkRequest does not exist: " + wr.id());
    }
    if (!wrRepo.incrementAttempt(wr.id())) {
      wrRepo.updateStatus(wr.id(), WorkRequest.Status.Failed);
    }
  }

  @Override
  @Transactional
  public boolean exists(String id) {
    return wrRepo.exists(id);
  }

  @Override
  @Transactional
  public <T> Optional<WorkRequest<T>> findById(String id, Class<T> payloadClass) {
    return wrRepo.findById(id, payloadClass);
  }

  @Transactional
  public boolean extendLease(String wrId, String owner, Duration leasePeriod) {
    return wrRepo.extendLease(wrId, owner, leasePeriod);
  }

  private <T> WorkRequest<T> findByIdOrFail(String id, Class<T> payloadClass) {
    return findById(id, payloadClass).orElseThrow(() -> new IllegalArgumentException("WorkRequest with id " + id + " does not exist"));
  }

  private static final class IdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    // ~72 bits entropy → collision-safe for billions of IDs
    static String generateRandomId() {
      UUID uuid = UUID.randomUUID();
      byte[] bytes = new byte[9]; // 72 bits
      long msb = uuid.getMostSignificantBits();
      for (int i = 0; i < 8; i++) {
        bytes[i] = (byte) (msb >>> (8 * (7 - i)));
      }
      bytes[8] = (byte) RANDOM.nextInt();
      return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(bytes);
    }
  }
}
