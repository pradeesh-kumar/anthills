package org.anthills.core;

import java.time.Duration;

public class DatabaseLeaseManager implements LeaseManager {

  private static final DatabaseLeaseManager INSTANCE = new DatabaseLeaseManager();

  public DatabaseLeaseManager() {

  }

  public static DatabaseLeaseManager getInstance() {
    return INSTANCE;
  }

  @Override
  public boolean acquire(String owner, String object, Duration period) {
    System.out.println("Trying to acquire lease for: " + owner + " object: " + object + " For duration: " + period.toString()); // TODO log.debug
    return false;
  }

  @Override
  public boolean extend(String owner, String object, Duration period) {
    System.out.println("Trying to extend lease for: " + owner + " object: " + object  + " For duration: " + period.toString());
    return false;
  }

  @Override
  public void release(String owner, String object) {
    System.out.println("Releasing lease for: " + owner + " object: " + object);
  }
}
