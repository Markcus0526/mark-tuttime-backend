package com.tuttime.restaurant.repository;

/**
 * Projection interface for user uniqueness check results.
 * Used to map native query results to a type-safe interface.
 */
public interface RestaurantUniquenessCheckProjection {
    boolean isEmailExists();
    boolean isPhoneExists();
    boolean isNameExists();
}

