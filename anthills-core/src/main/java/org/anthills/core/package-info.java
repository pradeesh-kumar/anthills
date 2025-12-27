/**
 * Core runtime implementations and factories for Anthills.
 *
 * <p>This module provides production-ready implementations that back the public API
 * defined in {@code anthills-api}. Highlights include:</p>
 *
 * <h2>Scheduling</h2>
 * <ul>
 *   <li>{@link org.anthills.core.scheduler.DefaultLeasedScheduler} — a lease-protected scheduler
 *       used to run clustered jobs so only one node executes a job at a time.</li>
 *   <li>Factory: {@link org.anthills.core.factory.Schedulers}</li>
 * </ul>
 *
 * <h2>Work processing</h2>
 * <ul>
 *   <li>{@link org.anthills.core.work.DefaultWorkClient} — submit, query, and cancel work items.</li>
 *   <li>{@link org.anthills.core.work.DefaultWorkRequestProcessor} — poll, claim, and dispatch
 *       work to handlers with automatic lease renewal and retry/backoff.</li>
 *   <li>Factories: {@link org.anthills.core.factory.WorkClients},
 *       {@link org.anthills.core.factory.WorkRequestProcessors}</li>
 * </ul>
 *
 * <h2>Concurrency utilities</h2>
 * <ul>
 *   <li>{@link org.anthills.core.concurrent.LeaseBoundExecutor} — runs tasks while periodically
 *       renewing a lease.</li>
 *   <li>{@link org.anthills.core.concurrent.NamedThreadFactory} — consistent thread naming and setup.</li>
 * </ul>
 *
 * <h2>Codec</h2>
 * <ul>
 *   <li>{@link org.anthills.core.JsonPayloadCodec} — JSON {@code PayloadCodec} backed by Gson.</li>
 * </ul>
 *
 * <h2>Utilities</h2>
 * <ul>
 *   <li>{@link org.anthills.core.util.Backoff} — fixed/exponential backoff with optional jitter.</li>
 *   <li>{@link org.anthills.core.util.Utils} — small common helpers.</li>
 * </ul>
 *
 * <p>Most types in this module are thread-safe and designed for use in multi-threaded and
 * distributed deployments.</p>
 */
package org.anthills.core;
