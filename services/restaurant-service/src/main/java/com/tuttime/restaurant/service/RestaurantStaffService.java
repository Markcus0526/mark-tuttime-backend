package com.tuttime.restaurant.service;

import com.tuttime.restaurant.dto.StaffRequest;
import com.tuttime.restaurant.dto.StaffResponse;
import com.tuttime.restaurant.dto.StaffUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface RestaurantStaffService {
	StaffResponse assignStaff(UUID restaurantId, StaffRequest request, UUID currentUserId, String userRoles);

	List<StaffResponse> getStaffByRestaurant(UUID restaurantId, UUID currentUserId, String userRoles);

	StaffResponse updateStaff(UUID restaurantId, UUID staffId, StaffUpdateRequest request, UUID currentUserId, String userRoles);

	void removeStaff(UUID restaurantId, UUID staffId, UUID currentUserId, String userRoles);

	boolean validateAdminAccess(UUID currentUserId);

	void validateRestaurantAccess(UUID restaurantId, UUID currentUserId, String userRoles);

	void validateBranchAccess(UUID branchId, UUID currentUserId, String userRoles);

	void validatePermissions(UUID entityId, UUID currentUserId, String userRoles);
}