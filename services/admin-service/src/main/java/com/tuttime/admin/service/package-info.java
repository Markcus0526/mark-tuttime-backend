/**
 * Admin Business Logic Services
 *
 * <p>This package contains service interfaces and implementations that contain
 * all admin business logic and orchestrate interactions with domain services.
 *
 * <h3>Responsibilities:</h3>
 * <ul>
 *   <li>Implement admin business rules and validation</li>
 *   <li>Orchestrate multiple gRPC calls to domain services</li>
 *   <li>Aggregate data from multiple services</li>
 *   <li>Manage transactions and consistency</li>
 *   <li>Log admin actions via {@link com.tuttime.admin.service.AdminAuditService}</li>
 * </ul>
 *
 * <h3>Design Principles:</h3>
 * <ul>
 *   <li><strong>Business Logic Here</strong> - All admin business logic lives in services</li>
 *   <li><strong>gRPC for Domain Calls</strong> - Use gRPC clients to call domain services</li>
 *   <li><strong>Circuit Breakers</strong> - All gRPC calls should use circuit breakers</li>
 *   <li><strong>Audit Logging</strong> - Log all admin actions</li>
 *   <li><strong>Error Handling</strong> - Handle and transform domain service errors</li>
 * </ul>
 *
 * <h3>Service Pattern:</h3>
 * <pre>{@code
 * @Service
 * @RequiredArgsConstructor
 * public class AdminUserManagementServiceImpl implements AdminUserManagementService {
 *     private final UserServiceGrpcClient userServiceClient;
 *     private final AdminAuditService auditService;
 *
 *     public UserDetailResponse getUserById(Long userId) {
 *         // Call domain service via gRPC
 *         UserResponse user = userServiceClient.getUserById(userId);
 *
 *         // Transform and return
 *         return UserDetailResponse.builder()
 *             .id(user.getId())
 *             .username(user.getUsername())
 *             .build();
 *     }
 * }
 * }</pre>
 *
 * @see com.tuttime.admin.grpc.client
 * @see com.tuttime.admin.service.AdminAuditService
 */
package com.tuttime.admin.service;

