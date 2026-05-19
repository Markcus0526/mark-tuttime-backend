package com.tuttime.restaurant.service;

import com.tuttime.restaurant.dto.ScheduleDto;

import java.util.List;
import java.util.UUID;

public interface ScheduleService {

    List<ScheduleDto.ScheduleResponse> getSchedules(UUID entityId);

    ScheduleDto.ScheduleResponse createSchedule(ScheduleDto.ScheduleRequest request, UUID currentUserId, String userRoles);

    ScheduleDto.ScheduleResponse updateSchedule(UUID scheduleId, ScheduleDto.ScheduleRequest request, UUID currentUserId, String userRoles);

    void deleteSchedule(UUID scheduleId, UUID currentUserId, String userRoles);

    void deleteAllByEntityId(UUID entityId);

}
