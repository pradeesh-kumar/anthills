package org.anthills.core;

import org.anthills.core.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class LeaseService {

  private static final Logger log =  LoggerFactory.getLogger(LeaseService.class);

  private final LeaseRepository leaseRepository;

  public LeaseService(LeaseRepository leaseRepository) {
    this.leaseRepository = leaseRepository;
  }

  @Transactional
  public boolean acquire(String owner, String object, Duration period) {
    log.info("Trying to Acquire lease for owner {} on the object {}", owner, object);
    Instant now = Instant.now();
    Instant expiresAt = now.plus(period);

    // Step 1: Try to insert lease if no existing row
    Lease newLease = new Lease(object, owner, expiresAt);
    if (leaseRepository.insertIfAbsent(newLease)) {
      return true;
    }

    // Step 2: Check if existing lease is expired
    log.debug("Existing lease found for the object {}. Checking if expired", object);
    Optional<Lease> existing = leaseRepository.findByObject(object);
    if (existing.isPresent()) {
      Lease lease = existing.get();
      if (lease.expiresAt().isBefore(now)) {
        log.debug("Found expired lease {}. Updating the owner and lease period", lease);
        // Try to take over expired lease
        return leaseRepository.updateIfExpired(newLease);
      } else if (lease.owner().equals(owner)) {
        return true;
      }
    }
    log.info("Lease not available for {} for the object {}", object, owner);
    return false;
  }

  @Transactional
  public boolean extend(String owner, String object, Duration period) {
    Instant newExpiry = Instant.now().plus(period);
    Lease lease = new Lease(object, owner, newExpiry);
    log.info("Trying to extend lease for owner {} on the object {}", owner, object);
    return leaseRepository.updateIfOwned(lease);
  }

  @Transactional
  public void release(String owner, String object) {
    log.info("Releasing lease from {} on the object {}", owner, object);
    leaseRepository.deleteByOwnerAndObject(owner, object);
  }
}
