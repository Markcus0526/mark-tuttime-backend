/**
 * REST Clients
 *
 * <p>This package contains REST clients for services that don't yet have gRPC support.
 *
 * <h3>Responsibilities:</h3>
 * <ul>
 *   <li>Execute HTTP REST calls to domain services</li>
 *   <li>Handle HTTP-specific concerns (headers, status codes)</li>
 *   <li>Transform REST responses to domain models</li>
 * </ul>
 *
 * <h3>Current Clients:</h3>
 * <ul>
 *   <li><strong>{@link com.tuttime.admin.rest.client.UserServiceRestClient}</strong>
 *       <ul>
 *         <li>REST client for user-service (legacy, will be migrated to gRPC)</li>
 *         <li>Uses WebClient for async HTTP calls</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <h3>Migration Path:</h3>
 * <p>REST clients are temporary. The goal is to migrate all inter-service communication to gRPC.
 * Once a service has gRPC support, its REST client should be removed and replaced with a
 * gRPC client in {@link com.tuttime.admin.grpc.client}.
 *
 * <h3>Design Principles:</h3>
 * <ul>
 *   <li><strong>WebClient</strong> - Use reactive WebClient for async calls</li>
 *   <li><strong>Error Handling</strong> - Transform HTTP errors to domain exceptions</li>
 *   <li><strong>Circuit Breakers</strong> - Use Resilience4j for fault tolerance</li>
 * </ul>
 *
 * @see com.tuttime.admin.grpc.client
 */
package com.tuttime.admin.rest.client;

