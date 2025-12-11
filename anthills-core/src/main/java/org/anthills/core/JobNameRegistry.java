package org.anthills.core;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class JobNameRegistry {

  private static final Set<String> jobSet = ConcurrentHashMap.newKeySet();

  static void register(String dataSourceIdentity, String jobName) {
    String key = dataSourceIdentity + ":" + jobName;
    if (jobSet.contains(key)) {
      throw new IllegalArgumentException("Job name already exists: " + jobName);
    }
    jobSet.add(key);
  }
}
