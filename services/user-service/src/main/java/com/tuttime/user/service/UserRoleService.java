package com.tuttime.user.service;

import java.util.UUID;

/**
 * Service interface for managing user roles.
 */
public interface UserRoleService {

	/**
	 * Assigns a role to a user.
	 *
	 * @param userId   UUID of the user
	 * @param roleName Name of the role to assign (e.g., ADMIN, USER)
	 * @throws IllegalArgumentException if user or role does not exist
	 */
	void assignRole(UUID userId, String roleName);

	/**
	 * Removes a role from a user.
	 *
	 * @param userId   UUID of the user
	 * @param roleName Name of the role to remove
	 * @throws IllegalArgumentException if user or role does not exist
	 */
	void removeRole(UUID userId, String roleName);
}