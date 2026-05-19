package com.tuttime.common.auth.security;

import com.tuttime.common.enums.UserRoleKind;
import com.tuttime.common.util.UuidUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class UserContext {
    private static final ThreadLocal<String> currentUserId = new ThreadLocal<>();
    private static final ThreadLocal<Set<String>> currentUserRoles = new ThreadLocal<>();

    public static void setUserId(String userId) {
        currentUserId.set(userId);
    }

    public static String getUserId() {
        return currentUserId.get();
    }

    public static void setRoles(String roles) {
        if (roles == null || roles.isBlank()) {
            currentUserRoles.set(Collections.emptySet());
        } else {
            // Split the comma-separated string from the Gateway into a Set
            Set<String> rolesSet = Arrays.stream(roles.split(","))
                    .map(String::trim)
                    .collect(Collectors.toSet());
            currentUserRoles.set(rolesSet);
        }
    }

    public static String getRoles() {
        Set<String> roles = currentUserRoles.get();
        return (roles != null && !roles.isEmpty()) ? String.join(",", roles) : null;
    }

    private static boolean hasRole(UserRoleKind roleKind) {
        Set<String> roles = currentUserRoles.get();
        // Exact match prevents substring collisions (e.g., "HOSTESS" matching "HOST")
        return roles != null && roles.contains(roleKind.name());
    }

    public static boolean isRoleAdmin() {
        return hasRole(UserRoleKind.ADMIN);
    }

    public static boolean isRoleOwner() {
        return hasRole(UserRoleKind.OWNER);
    }

    public static boolean isRoleManager() {
        return hasRole(UserRoleKind.MANAGER);
    }

    public static boolean isRoleHostess() {
        return hasRole(UserRoleKind.HOSTESS);
    }

    public static boolean isRoleUser() {
        return hasRole(UserRoleKind.USER);
    }

    public static boolean isRoleGuest() {
        return hasRole(UserRoleKind.GUEST);
    }

    public static boolean isValidUUID() {
        return UuidUtil.toUuid(currentUserId.get()) != null;
    }

    public static void clear() {
        currentUserId.remove();
        currentUserRoles.remove();
    }
}
