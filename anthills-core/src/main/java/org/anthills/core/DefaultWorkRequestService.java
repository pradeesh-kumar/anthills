package org.anthills.core;

import org.anthills.commons.WorkRequest;
import org.anthills.core.annotation.Transactional;
import org.anthills.core.contract.WorkRequestService;

import java.util.Optional;

public class DefaultWorkRequestService implements WorkRequestService {

  private final WorkRequestRepository wrRepo;

  public DefaultWorkRequestService(WorkRequestRepository wrRepo) {
    this.wrRepo = wrRepo;
  }

  @Override
  @Transactional
  public WorkRequest<?> create(WorkRequest<?> wr) {
    // TODO refine, validate and update
    return wrRepo.create(wr);
  }

  @Override
  @Transactional
  public WorkRequest<?> update(WorkRequest<?> wr) {
    // TODO refine, validate and update
    return wrRepo.update(wr);
  }

  @Override
  @Transactional
  public boolean exists(String id) {
    return false;
  }

  @Override
  @Transactional
  public Optional<WorkRequest<?>> findById(String id, Class<?> payloadClass) {
    return wrRepo.findById(id, payloadClass);
  }
}
