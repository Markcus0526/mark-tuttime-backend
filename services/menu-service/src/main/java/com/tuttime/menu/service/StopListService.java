package com.tuttime.menu.service;

import com.tuttime.menu.dto.StopListDto;

import java.util.List;
import java.util.UUID;

public interface StopListService {
    StopListDto.StopListResponse addToStopList(StopListDto.StopListRequest request, UUID currentUserId, String userRoles);

    List<StopListDto.StopListResponse> getStopListByBranch(UUID branchId);

    void removeFromStopList(UUID id, UUID currentUserId, String userRoles);
}
