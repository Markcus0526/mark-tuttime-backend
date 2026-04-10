package com.tuttime.restaurant.service;

import com.tuttime.restaurant.dto.ScheduleRequest;
import com.tuttime.restaurant.dto.ScheduleResponse;

import java.util.List;
import java.util.UUID;

public interface ScheduleService {

	List<ScheduleResponse> getSchedulesByRestaurantId(UUID restaurantId);

	ScheduleResponse createScheduleForRestaurant(UUID restaurantId, ScheduleRequest request, UUID currentUserId, String userRoles);

	ScheduleResponse updateRestaurantSchedule(UUID scheduleId, ScheduleRequest request, UUID currentUserId, String userRoles);

	void deleteRestaurantSchedule(UUID scheduleId, UUID currentUserId, String userRoles);


	List<ScheduleResponse> getSchedulesByBranchId(UUID branchId);

	ScheduleResponse createScheduleForBranch(UUID branchId, ScheduleRequest request, UUID currentUserId, String userRoles);

	ScheduleResponse updateBranchSchedule(UUID scheduleId, ScheduleRequest request, UUID currentUserId, String userRoles);

	void deleteBranchSchedule(UUID scheduleId, UUID currentUserId, String userRoles);

}
