package com.tuttime.common.auth.enums;

public enum UserRoleKind {
	ADMIN, // -- for Admin panel
	OWNER, // -- for Restaurant
	MANAGER, // -- for Restaurant
	HOSTESS, // -- for Restaurant
	USER, // -- for Mobile user
	GUEST; // -- for Mobile user, not signed

	public static boolean isValid(String name) {
		for (UserRoleKind status : values()) {
			if (status.name().equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}
}

