package org.anthills.core;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class LeaseService {

  private final LeaseRepository leaseRepository;

  LeaseService(LeaseRepository leaseRepository) {
    this.leaseRepository = leaseRepository;
  }

  public boolean acquire(String owner, String object, Duration period) {
    Instant now = Instant.now();
    Instant expiresAt = now.plus(period);

    // Step 1: Try to insert lease if no existing row
    Lease newLease = new Lease(object, owner, expiresAt);
    if (leaseRepository.insertIfAbsent(newLease)) {
      return true;
    }

    // Step 2: Check if existing lease is expired
    Optional<Lease> existing = leaseRepository.findByObject(object);
    if (existing.isPresent()) {
      Lease lease = existing.get();
      if (lease.expiresAt().isBefore(now)) {
        // Try to take over expired lease
        return leaseRepository.updateIfOwnedAndNotExpired(newLease);
      }
    }
    return false;
  }

  public boolean extend(String owner, String object, Duration period) {
    Instant newExpiry = Instant.now().plus(period);
    Lease lease = new Lease(object, owner, newExpiry);
    return leaseRepository.updateIfOwnedAndNotExpired(lease);
  }

  public void release(String owner, String object) {
    leaseRepository.deleteByOwnerAndObject(owner, object);
  }
}
