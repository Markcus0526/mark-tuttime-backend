package com.tuttime.restaurant.service;

import com.tuttime.restaurant.dto.ScheduleRequest;
import com.tuttime.restaurant.dto.ScheduleResponse;

import java.util.List;
import java.util.UUID;

public interface ScheduleService {

	List<ScheduleResponse> getSchedulesByRestaurantId(UUID restaurantId);

	ScheduleResponse createScheduleForRestaurant(UUID restaurantId, ScheduleRequest request);

	List<ScheduleResponse> getSchedulesByBranchId(UUID branchId);

	ScheduleResponse createScheduleForBranch(UUID branchId, ScheduleRequest request);

	ScheduleResponse updateSchedule(UUID scheduleId, UUID restaurantId, UUID branchId, ScheduleRequest request);

	void deleteSchedule(UUID scheduleId, UUID restaurantId, UUID branchId);
}
