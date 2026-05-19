package com.tuttime.restaurant.repository.projection;

import java.util.UUID;

public interface TableHierarchyProjection {
    UUID getTableId();

    UUID getZoneId();

    UUID getBranchId();

    UUID getRestaurantId();

    String getTableLabel();

    String getZoneName();

    String getBranchName();

    String getBranchAddress();

    String getBranchPhone();

    String getRestaurantName();

    Integer getTotalTables();   // matches the subquery count
}

