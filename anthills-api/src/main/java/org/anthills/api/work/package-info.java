/**
 * Work processing API for submitting, querying, and handling typed units of work.
 *
 * <p>This package provides the core abstractions to:
 * <ul>
 *   <li>submit work with a typed payload via {@link org.anthills.api.work.WorkClient}</li>
 *   <li>persist and lease work items via a {@link org.anthills.api.work.WorkStore}</li>
 *   <li>process work using {@link org.anthills.api.work.WorkRequestProcessor} with
 *       application-defined {@link org.anthills.api.work.WorkHandler} or
 *       {@link org.anthills.api.work.WorkRequestHandler} implementations</li>
 *   <li>configure runtime behavior via {@link org.anthills.api.work.ProcessorConfig}</li>
 * </ul>
 *
 * <h2>Typical flow</h2>
 * <ol>
 *   <li>A producer uses {@link org.anthills.api.work.WorkClient#submit(String, Object)} to create work.</li>
 *   <li>A processor registers handlers with {@link org.anthills.api.work.WorkRequestProcessor#registerHandler(String, Class, WorkHandler)}.</li>
 *   <li>The processor polls a {@link org.anthills.api.work.WorkStore} to claim, decode, and dispatch work to handlers.</li>
 * </ol>
 *
 * <p>See also {@link org.anthills.api.codec} for payload encoding/decoding concerns.</p>
 */
package org.anthills.api.work;
