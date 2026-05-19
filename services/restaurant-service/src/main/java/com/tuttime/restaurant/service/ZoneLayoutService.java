package com.tuttime.restaurant.service;

import com.tuttime.restaurant.dto.ZoneLayoutDto;

import java.util.UUID;

public interface ZoneLayoutService {

    ZoneLayoutDto.LayoutDto getZoneLayout(UUID zoneId);

    void syncLayout(UUID zoneId, ZoneLayoutDto.LayoutDto dto, UUID currentUserId, String userRoles);
}
