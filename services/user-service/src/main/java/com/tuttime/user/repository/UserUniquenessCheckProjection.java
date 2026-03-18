package com.tuttime.user.repository;

/**
 * Projection interface for user uniqueness check results.
 * Used to map native query results to a type-safe interface.
 */
public interface UserUniquenessCheckProjection {
    boolean isEmailExists();
    boolean isPhoneExists();
    boolean isUsernameExists();
}

