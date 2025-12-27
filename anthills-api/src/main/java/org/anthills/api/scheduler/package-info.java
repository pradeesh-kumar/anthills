/**
 * Scheduling API for running clustered, lease-protected jobs.
 *
 * <p>This package defines abstractions for scheduling recurring jobs with
 * distributed lease coordination so that, in a multi-node deployment, only one
 * node runs a given job at a time.</p>
 *
 * <h2>Key types</h2>
 * <ul>
 *   <li>{@link org.anthills.api.scheduler.Job} — a unit of work to execute.</li>
 *   <li>{@link org.anthills.api.scheduler.Schedule} — describes when to run, via fixed-rate or CRON.</li>
 *   <li>{@link org.anthills.api.scheduler.LeasedScheduler} — coordinates execution with leases.</li>
 *   <li>{@link org.anthills.api.scheduler.SchedulerConfig} — runtime tuning parameters.</li>
 * </ul>
 *
 * <h2>Typical usage</h2>
 * <pre>{@code
 * LeasedScheduler scheduler = ...;
 * scheduler.schedule(
 *     "daily-report",
 *     Schedule.Cron.parse("0 0 9 * * ?"), // every day at 09:00
 *     () -> reportService.generate());   // your job
 * scheduler.start();
 * }</pre>
 */
package org.anthills.api.scheduler;
