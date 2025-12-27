package org.anthills.api.work;

import java.time.Instant;
import java.util.Set;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.EnumSet;

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

  /**
   * Returns a new builder for constructing {@link WorkQuery} instances.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Fluent builder for {@link WorkQuery}.
   * Page is required; set either page(Page) or both limit(int) and offset(int).
   */
  public static final class Builder {
    private Set<String> ids;
    private String workType;
    private Set<WorkRequest.Status> statuses;
    private Instant createdAfter;
    private Instant createdBefore;
    private Page page;
    private Integer limit;
    private Integer offset;

    /**
     * Sets the set of concrete WorkRequest IDs to include.
     * If {@code null} is provided, no ID filter is applied. Providing an empty set is equivalent to no ID filter.
     *
     * @param ids set of request IDs to include; {@code null} or empty => no filter
     * @return this builder
     */
    public Builder ids(Set<String> ids) {
      this.ids = (ids == null) ? null : new LinkedHashSet<>(ids);
      return this;
    }

    /**
     * Adds a single request ID to the inclusion set.
     * A {@code null} value is ignored.
     *
     * @param id request ID to add; {@code null} is ignored
     * @return this builder
     */
    public Builder addId(String id) {
      if (id == null) {
        return this;
      }
      if (this.ids == null) {
        this.ids = new LinkedHashSet<>();
      }
      this.ids.add(id);
      return this;
    }

    /**
     * Adds multiple request IDs to the inclusion set.
     * A {@code null} collection is ignored; {@code null} elements are ignored.
     *
     * @param ids collection of IDs to add; may be {@code null}
     * @return this builder
     */
    public Builder addIds(Collection<String> ids) {
      if (ids != null) {
        ids.forEach(this::addId);
      }
      return this;
    }

    /**
     * Sets the logical routing key to filter by.
     * A {@code null} value means all work types.
     *
     * @param workType routing key; may be {@code null}
     * @return this builder
     */
    public Builder workType(String workType) {
      this.workType = workType;
      return this;
    }

    /**
     * Sets the set of statuses to include.
     * Semantics: {@code null} means all statuses; an empty set means no statuses (matches nothing).
     * The input set is defensively copied into an {@link EnumSet}.
     *
     * @param statuses statuses to include; {@code null} => all; empty => none
     * @return this builder
     */
    public Builder statuses(Set<WorkRequest.Status> statuses) {
      if (statuses == null) {
        this.statuses = null;
      } else if (statuses.isEmpty()) {
        this.statuses = EnumSet.noneOf(WorkRequest.Status.class);
      } else {
        this.statuses = EnumSet.copyOf(statuses);
      }
      return this;
    }

    /**
     * Adds a single status to include.
     * A {@code null} value is ignored.
     *
     * @param status status to add; {@code null} is ignored
     * @return this builder
     */
    public Builder addStatus(WorkRequest.Status status) {
      if (status == null) {
        return this;
      }
      if (this.statuses == null) {
        this.statuses = EnumSet.noneOf(WorkRequest.Status.class);
      }
      this.statuses.add(status);
      return this;
    }

    /**
     * Sets the lower bound (exclusive) for creation time.
     * A {@code null} value disables this filter.
     *
     * @param createdAfter include requests strictly after this instant; may be {@code null}
     * @return this builder
     */
    public Builder createdAfter(Instant createdAfter) {
      this.createdAfter = createdAfter;
      return this;
    }

    /**
     * Sets the upper bound (exclusive) for creation time.
     * A {@code null} value disables this filter.
     *
     * @param createdBefore include requests strictly before this instant; may be {@code null}
     * @return this builder
     */
    public Builder createdBefore(Instant createdBefore) {
      this.createdBefore = createdBefore;
      return this;
    }

    /**
     * Sets the paging configuration explicitly.
     * If set, it takes precedence over {@link #limit(int)} and {@link #offset(int)}.
     *
     * @param page page configuration; may be {@code null} to clear (requires {@code limit} and {@code offset} to be set before {@link #build()})
     * @return this builder
     */
    public Builder page(Page page) {
      this.page = page;
      return this;
    }

    /**
     * Sets the page size (maximum number of results to return).
     *
     * @param limit maximum number of results; must be {@code >= 0}
     * @return this builder
     * @throws IllegalArgumentException if {@code limit < 0}
     */
    public Builder limit(int limit) {
      if (limit < 0) {
        throw new IllegalArgumentException("limit must be >= 0");
      }
      this.limit = limit;
      return this;
    }

    /**
     * Sets the number of results to skip.
     *
     * @param offset number of results to skip; must be {@code >= 0}
     * @return this builder
     * @throws IllegalArgumentException if {@code offset < 0}
     */
    public Builder offset(int offset) {
      if (offset < 0) {
        throw new IllegalArgumentException("offset must be >= 0");
      }
      this.offset = offset;
      return this;
    }

    /**
     * Builds an immutable {@link WorkQuery} instance.
     * Precedence rule: if {@link #page(Page)} is set, it is used. Otherwise both {@link #limit(int)} and {@link #offset(int)} must be set.
     * IDs: if none provided, defaults to an empty set (no specific ID filter). Statuses: {@code null} means all statuses.
     *
     * @return a new {@link WorkQuery}
     * @throws IllegalStateException if neither {@code page} nor both {@code limit} and {@code offset} are provided
     */
    public WorkQuery build() {
      Page p = this.page;
      if (p == null) {
        if (this.limit == null || this.offset == null) {
          throw new IllegalStateException("page is required; set page(Page) or both limit(int) and offset(int)");
        }
        p = Page.of(this.limit, this.offset);
      }

      Set<String> idsCopy = (this.ids == null) ? Set.of() : Set.copyOf(this.ids);
      Set<WorkRequest.Status> statusesCopy = (this.statuses == null) ? null : Set.copyOf(this.statuses);

      return new WorkQuery(idsCopy, this.workType, statusesCopy, this.createdAfter, this.createdBefore, p);
    }
  }
}
