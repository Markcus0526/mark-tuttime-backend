/**
 * Security Filters
 *
 * <p>This package contains HTTP request filters for security and cross-cutting concerns.
 *
 * <h3>Responsibilities:</h3>
 * <ul>
 *   <li>Intercept HTTP requests before they reach controllers</li>
 *   <li>Implement security checks (authentication, authorization)</li>
 *   <li>Apply rate limiting</li>
 *   <li>Enforce IP whitelisting</li>
 * </ul>
 *
 * <h3>Available Filters:</h3>
 * <ul>
 *   <li><strong>{@link com.tuttime.admin.filter.AdminJwtAuthenticationFilter}</strong>
 *       <ul>
 *         <li>Validates JWT tokens from Authorization header</li>
 *         <li>Checks token whitelist/blacklist</li>
 *         <li>Sets Spring Security context</li>
 *       </ul>
 *   </li>
 *   <li><strong>{@link com.tuttime.admin.filter.AdminIpWhitelistFilter}</strong>
 *       <ul>
 *         <li>Enforces IP-based access control</li>
 *         <li>Blocks requests from non-whitelisted IPs</li>
 *       </ul>
 *   </li>
 *   <li><strong>{@link com.tuttime.admin.filter.AdminRateLimitFilter}</strong>
 *       <ul>
 *         <li>Implements rate limiting per endpoint</li>
 *         <li>Prevents abuse and DoS attacks</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <h3>Filter Order:</h3>
 * <p>Filters are registered in Spring Security filter chain in this order:
 * <ol>
 *   <li>IP Whitelist Filter (first check)</li>
 *   <li>Rate Limit Filter</li>
 *   <li>JWT Authentication Filter (before Spring Security filters)</li>
 * </ol>
 *
 * @see com.tuttime.admin.config.SecurityConfig
 */
package com.tuttime.admin.filter;

