package com.tuttime.common.enums;

import java.util.Arrays;

public enum UserRoleKind {
	ADMIN, // -- for Admin panel
	OWNER, // -- for Restaurant
	MANAGER, // -- for Restaurant's branch
	HOSTESS, // -- for Restaurant's cache
	USER, // -- for Mobile user
	GUEST; // -- for Mobile user, not signed

	public static boolean isValid(String roleName) {
		if (roleName == null) return false;
		return Arrays.stream(UserRoleKind.values())
				.anyMatch(role -> role.name().equalsIgnoreCase(roleName));
	}
}

