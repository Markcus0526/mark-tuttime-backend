/**
 * JPA Repositories
 *
 * <p>This package contains Spring Data JPA repositories for admin entities.
 *
 * <h3>Responsibilities:</h3>
 * <ul>
 *   <li>Provide data access methods for admin entities</li>
 *   <li>Define custom query methods</li>
 *   <li>Handle database operations (CRUD)</li>
 * </ul>
 *
 * <h3>Available Repositories:</h3>
 * <ul>
 *   <li><strong>{@link com.tuttime.admin.repository.AdminUserRepository}</strong>
 *       <ul>
 *         <li>Manages admin user accounts</li>
 *         <li>Methods: findByUsername, findByEmail, etc.</li>
 *       </ul>
 *   </li>
 *   <li><strong>{@link com.tuttime.admin.repository.AdminAuditLogRepository}</strong>
 *       <ul>
 *         <li>Manages audit log entries</li>
 *         <li>Methods: findByAdminId, findByActionType, findByDateRange, etc.</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <h3>Design Principles:</h3>
 * <ul>
 *   <li><strong>Repository Pattern</strong> - Abstract data access layer</li>
 *   <li><strong>Spring Data JPA</strong> - Use method naming conventions for queries</li>
 *   <li><strong>Custom Queries</strong> - Use {@code @Query} for complex queries</li>
 * </ul>
 *
 * @see com.tuttime.admin.entity
 * @see com.tuttime.admin.service
 */
package com.tuttime.admin.repository;

