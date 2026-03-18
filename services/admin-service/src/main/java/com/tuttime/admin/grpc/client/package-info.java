/**
 * gRPC Clients for Domain Services
 *
 * <p>This package contains gRPC client stubs for communicating with domain microservices.
 *
 * <h3>Responsibilities:</h3>
 * <ul>
 *   <li>Execute gRPC calls to domain services</li>
 *   <li>Handle gRPC-specific concerns (stubs, channels, deadlines)</li>
 *   <li>Integrate with circuit breakers (Resilience4j)</li>
 *   <li>Implement retry logic</li>
 *   <li>Transform gRPC errors to domain exceptions</li>
 * </ul>
 *
 * <h3>Design Principles:</h3>
 * <ul>
 *   <li><strong>No Business Logic</strong> - Clients are pure communication layer</li>
 *   <li><strong>Circuit Breakers</strong> - All methods should use {@code @CircuitBreaker}</li>
 *   <li><strong>Retry Logic</strong> - Use {@code @Retry} for transient failures</li>
 *   <li><strong>Error Transformation</strong> - Convert gRPC status to domain exceptions</li>
 * </ul>
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * @Component
 * @RequiredArgsConstructor
 * public class UserServiceGrpcClient {
 *     private final UserServiceGrpc.UserServiceBlockingStub userServiceStub;
 *
 *     @CircuitBreaker(name = "userService")
 *     @Retry(name = "userService")
 *     public UserResponse getUserById(Long userId) {
 *         GetUserRequest request = GetUserRequest.newBuilder()
 *             .setUserId(userId)
 *             .build();
 *
 *         UserResponse response = userServiceStub.getUser(request);
 *         return response;
 *     }
 * }
 * }</pre>
 *
 * <h3>Available Clients:</h3>
 * <ul>
 *   <li>{@link com.tuttime.admin.grpc.client.UserServiceGrpcClient} - User management</li>
 *   <li>{@link com.tuttime.admin.grpc.client.TokenServiceGrpcClient} - Token operations</li>
 *   <li>{@link com.tuttime.admin.grpc.client.MiningServiceGrpcClient} - Mining operations</li>
 *   <li>{@link com.tuttime.admin.grpc.client.StakingServiceGrpcClient} - Staking operations</li>
 *   <li>{@link com.tuttime.admin.grpc.client.RewardsServiceGrpcClient} - Rewards operations</li>
 *   <li>{@link com.tuttime.admin.grpc.client.ConversionServiceGrpcClient} - Conversion operations</li>
 * </ul>
 *
 * @see com.tuttime.admin.service
 */
package com.tuttime.admin.grpc.client;

