/**
 * Utility Classes
 *
 * <p>This package contains utility classes for common operations.
 *
 * <h3>Responsibilities:</h3>
 * <ul>
 *   <li>Provide reusable utility methods</li>
 *   <li>Handle cross-cutting concerns (JWT, formatting, etc.)</li>
 *   <li>Encapsulate common logic</li>
 * </ul>
 *
 * <h3>Available Utilities:</h3>
 * <ul>
 *   <li><strong>{@link com.tuttime.admin.util.AdminJwtTokenUtil}</strong>
 *       <ul>
 *         <li>JWT token generation and validation</li>
 *         <li>Token claims extraction</li>
 *         <li>ISS/AUD validation</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <h3>Design Principles:</h3>
 * <ul>
 *   <li><strong>Stateless</strong> - Utility methods should be stateless when possible</li>
 *   <li><strong>Pure Functions</strong> - Prefer pure functions over stateful utilities</li>
 *   <li><strong>Single Responsibility</strong> - Each utility class has one clear purpose</li>
 * </ul>
 */
package com.tuttime.admin.util;

