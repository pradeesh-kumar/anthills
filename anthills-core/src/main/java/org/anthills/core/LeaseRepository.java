package org.anthills.core;

import java.util.Optional;

public interface LeaseRepository {
  Optional<Lease> findByObject(String object);
  boolean insertIfAbsent(Lease lease);
  boolean updateIfOwnedAndNotExpired(Lease lease);
  void deleteByOwnerAndObject(String owner, String object);
}
