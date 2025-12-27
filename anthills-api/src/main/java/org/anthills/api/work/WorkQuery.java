package org.anthills.api.work;

import java.time.Instant;
import java.util.Set;

/**
 * Criteria for listing {@link WorkRequest}s.
 *
 * @param ids           optional set of specific request IDs to include
 * @param workType      logical routing key to filter by; may be {@code null} to include all
 * @param statuses      optional set of statuses to include; {@code null} means all
 * @param createdAfter  include requests created strictly after this instant; {@code null} to ignore
 * @param createdBefore include requests created strictly before this instant; {@code null} to ignore
 * @param page          paging configuration (limit/offset), required
 */
public record WorkQuery(
  Set<String> ids,
  String workType,
  Set<WorkRequest.Status> statuses,
  Instant createdAfter,
  Instant createdBefore,
  Page page
) {

  /**
   * Paging configuration with fixed limit and offset.
   *
   * @param limit  maximum number of results to return (must be >= 0)
   * @param offset number of results to skip (must be >= 0)
   */
  public record Page(int limit, int offset) {

    /**
     * Factory method for creating a {@link Page} instance.
     *
     * @param limit  maximum number of results to return
     * @param offset number of results to skip
     * @return a page configuration
     */
    public static Page of(int limit, int offset) {
      return new Page(limit, offset);
    }
  }

  /**
   * Provides a minimal query for a given {@code workType} returning a single item (limit=1) at offset 0.
   *
   * @param workType routing key to filter by
   * @return a default query for the given work type
   */
  public static WorkQuery defaults(String workType) {
    return new WorkQuery(Set.of(), workType, null, null, null, Page.of(1, 0));
  }
}
