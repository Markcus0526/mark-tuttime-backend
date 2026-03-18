/**
 * Admin REST Controllers
 *
 * <p>This package contains REST controllers for the Admin Service API.
 *
 * <h3>Responsibilities:</h3>
 * <ul>
 *   <li>Handle HTTP requests from admin-gateway</li>
 *   <li>Validate request parameters and bodies</li>
 *   <li>Extract authentication context (admin user ID, roles)</li>
 *   <li>Delegate business logic to service layer</li>
 *   <li>Format responses according to API contract</li>
 * </ul>
 *
 * <h3>Design Principles:</h3>
 * <ul>
 *   <li><strong>Thin Controllers</strong> - Controllers should be thin and delegate to services</li>
 *   <li><strong>No Business Logic</strong> - Business logic belongs in the service layer</li>
 *   <li><strong>Request Validation</strong> - Use {@code @Valid} for request validation</li>
 *   <li><strong>Error Handling</strong> - Let exceptions bubble up to global exception handler</li>
 * </ul>
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * @RestController
 * @RequestMapping("/api/admin/users")
 * public class AdminUserManagementController {
 *     private final AdminUserManagementService userService;
 *
 *     @GetMapping("/{id}")
 *     public ResponseEntity<ApiResponse<UserDetailResponse>> getUser(@PathVariable Long id) {
 *         UserDetailResponse user = userService.getUserById(id);
 *         return ResponseEntity.ok(ApiResponse.success(user));
 *     }
 * }
 * }</pre>
 *
 * @see com.tuttime.admin.service
 */
package com.tuttime.admin.controller;

