package com.tuttime.restaurant.repository.projection;

import java.math.BigDecimal;
import java.util.UUID;

public interface BranchProjection {
    UUID getBranchId();

    UUID getRestaurantId();

    String getBranchName();

    String getBranchAddress();

    String getBranchPhone();

    String getRestaurantName();

    Double getDistanceInMeters();

    Long getAvailableTables(); // COUNT returns Long in SQL

    BigDecimal getMinPrice();

    BigDecimal getMaxPrice();
}