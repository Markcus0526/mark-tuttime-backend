/**
 * Admin Domain Entities
 *
 * <p>This package contains JPA entities for the Admin Service's own database.
 *
 * <h3>Responsibilities:</h3>
 * <ul>
 *   <li>Define database schema for admin-specific data</li>
 *   <li>Map to database tables via JPA annotations</li>
 *   <li>Provide domain model for admin entities</li>
 * </ul>
 *
 * <h3>Current Entities:</h3>
 * <ul>
 *   <li><strong>{@link com.tuttime.admin.entity.AdminUser}</strong> - Admin user accounts
 *       <ul>
 *         <li>Stores admin credentials, roles, and status</li>
 *         <li>Used for authentication and authorization</li>
 *       </ul>
 *   </li>
 *   <li><strong>{@link com.tuttime.admin.entity.AdminAuditLog}</strong> - Audit trail
 *       <ul>
 *         <li>Logs all admin actions for compliance and security</li>
 *         <li>Includes action type, admin ID, target resource, timestamp</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <h3>Design Principles:</h3>
 * <ul>
 *   <li><strong>Admin-Specific Only</strong> - Only entities that belong to admin service</li>
 *   <li><strong>Domain Entities Stay in Domain Services</strong> - User, Token, etc. entities are in their respective services</li>
 *   <li><strong>JPA Auditing</strong> - Use {@code @CreatedDate}, {@code @LastModifiedDate}</li>
 * </ul>
 *
 * <h3>Database Schema:</h3>
 * <p>All entities are stored in the {@code admin_service} schema:
 * <ul>
 *   <li>{@code admin_service.admin_users} - Admin user accounts</li>
 *   <li>{@code admin_service.admin_audit_logs} - Audit trail</li>
 * </ul>
 *
 * @see com.tuttime.admin.repository
 */
package com.tuttime.admin.entity;

