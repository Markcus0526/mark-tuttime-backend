/**
 * Admin Data Transfer Objects (DTOs)
 *
 * <p>This package contains request and response DTOs for the Admin Service API.
 *
 * <h3>Responsibilities:</h3>
 * <ul>
 *   <li>Define API contract (request/response models)</li>
 *   <li>Validate request data using Bean Validation</li>
 *   <li>Transform between API models and domain models</li>
 * </ul>
 *
 * <h3>Naming Conventions:</h3>
 * <ul>
 *   <li><strong>Request DTOs</strong> - End with {@code Request} (e.g., {@code AdminLoginRequest})</li>
 *   <li><strong>Response DTOs</strong> - End with {@code Response} (e.g., {@code UserDetailResponse})</li>
 * </ul>
 *
 * <h3>Validation:</h3>
 * <p>Use Bean Validation annotations for request validation:
 * <pre>{@code
 * public class AdminLoginRequest {
 *     @NotBlank(message = "Username is required")
 *     private String username;
 *
 *     @NotBlank(message = "Password is required")
 *     @Size(min = 8, message = "Password must be at least 8 characters")
 *     private String password;
 * }
 * }</pre>
 *
 * <h3>DTO Categories:</h3>
 * <ul>
 *   <li><strong>Auth</strong> - {@code AdminLoginRequest}, {@code AdminLoginResponse}</li>
 *   <li><strong>User</strong> - {@code UserDetailResponse}, {@code UserStatsResponse}, etc.</li>
 *   <li><strong>Token</strong> - {@code TokenBalanceResponse}, {@code TokenStatsResponse}, etc.</li>
 *   <li><strong>Mining</strong> - {@code MiningStatsResponse}, {@code MiningSessionResponse}, etc.</li>
 *   <li><strong>Staking</strong> - {@code StakingPoolResponse}, {@code StakingPositionsResponse}</li>
 *   <li><strong>Analytics</strong> - {@code DashboardStatsResponse}, {@code ConversionStatsResponse}</li>
 * </ul>
 */
package com.tuttime.admin.dto;

