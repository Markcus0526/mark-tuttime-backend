package com.tuttime.restaurant.service;

import com.tuttime.restaurant.dto.ScheduleRequest;
import com.tuttime.restaurant.dto.ScheduleResponse;

import java.util.List;
import java.util.UUID;

public interface ScheduleService {

	List<ScheduleResponse> getSchedules(UUID entityId);

	ScheduleResponse createSchedule(ScheduleRequest request, UUID currentUserId, String userRoles);

	ScheduleResponse updateSchedule(UUID scheduleId, ScheduleRequest request, UUID currentUserId, String userRoles);

	void deleteSchedule(UUID scheduleId, UUID currentUserId, String userRoles);

	void deleteAllByEntityId(UUID entityId);

}
