package com.tuttime.restaurant.repository.projection;

import java.util.UUID;

public interface TableDetailProjection {
    UUID getTableId();

    UUID getZoneId();

    String getTableLabel();

    String getZoneName();

    Integer getSeats();
}
