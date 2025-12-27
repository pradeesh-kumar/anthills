package org.anthills.core;

/**
 * Functional contract used to renew a lease while a task is running.
 * Implementations should attempt a best-effort renewal and return whether
 * the lease remains valid after the attempt.
 */
@FunctionalInterface
public interface LeaseRenewer {
    /**
     * Attempts to renew the lease.
     *
     * @return true if the lease is still valid after renewal; false to signal that
     *         the task should stop because the lease could not be renewed
     */
    boolean renew();
}
