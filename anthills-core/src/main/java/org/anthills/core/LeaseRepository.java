package org.anthills.core;

import java.util.Optional;

public interface LeaseRepository {
  Optional<Lease> findByObject(String object);
  boolean insertIfAbsent(Lease lease);
  boolean updateIfExpired(Lease lease);
  boolean updateIfOwned(Lease lease);
  void deleteByOwnerAndObject(String owner, String object);
  boolean existsByOwnerAndObject(String owner, String object);
}
