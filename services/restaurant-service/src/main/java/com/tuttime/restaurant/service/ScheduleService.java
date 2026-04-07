package com.tuttime.restaurant.service;

import com.tuttime.restaurant.dto.ScheduleRequest;
import com.tuttime.restaurant.dto.ScheduleResponse;

import java.util.List;
import java.util.UUID;

public interface ScheduleService {

	List<ScheduleResponse> getSchedulesByRestaurantId(UUID restaurantId);

	ScheduleResponse createScheduleForRestaurant(UUID restaurantId, ScheduleRequest request);

	ScheduleResponse updateRestaurantSchedule(UUID scheduleId, UUID restaurantId, ScheduleRequest request);

	void deleteRestaurantSchedule(UUID scheduleId, UUID restaurantId);


	List<ScheduleResponse> getSchedulesByBranchId(UUID branchId);

	ScheduleResponse createScheduleForBranch(UUID branchId, ScheduleRequest request);

	ScheduleResponse updateBranchSchedule(UUID scheduleId, UUID branchId, ScheduleRequest request);

	void deleteBranchSchedule(UUID scheduleId, UUID branchId);

}
