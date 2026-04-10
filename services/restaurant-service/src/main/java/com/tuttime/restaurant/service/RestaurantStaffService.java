package com.tuttime.restaurant.service;

import com.tuttime.restaurant.entity.StaffRequest;
import com.tuttime.restaurant.entity.StaffResponse;
import com.tuttime.restaurant.entity.StaffUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface RestaurantStaffService {
	StaffResponse assignStaff(UUID restaurantId, StaffRequest request, UUID currentUserId, String userRoles);

	List<StaffResponse> getStaffByRestaurant(UUID restaurantId, UUID currentUserId, String userRoles);

	StaffResponse updateStaff(UUID restaurantId, UUID staffId, StaffUpdateRequest request, UUID currentUserId, String userRoles);

	void removeStaff(UUID restaurantId, UUID staffId, UUID currentUserId, String userRoles);

	boolean validateAdmin(UUID currentUserId);

	boolean validateRestaurantOwner(UUID restaurantId, UUID currentUserId);

	boolean validateBranchManager(UUID branchId, UUID currentUserId);
}