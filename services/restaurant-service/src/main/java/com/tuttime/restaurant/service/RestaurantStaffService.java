package com.tuttime.restaurant.service;

import com.tuttime.restaurant.dto.StaffDto;

import java.util.List;
import java.util.UUID;

public interface RestaurantStaffService {
	StaffDto.StaffResponse assignStaff(UUID restaurantId, StaffDto.StaffRequest request, UUID currentUserId, String userRoles);

	List<StaffDto.StaffResponse> getStaffByRestaurant(UUID restaurantId, UUID currentUserId, String userRoles);

	StaffDto.StaffResponse updateStaff(UUID restaurantId, UUID staffId, StaffDto.StaffUpdateRequest request, UUID currentUserId, String userRoles);

	void removeStaff(UUID restaurantId, UUID staffId, UUID currentUserId, String userRoles);

	void validateAdminAccess(UUID currentUserId);

	void validateRestaurantAccess(UUID restaurantId, UUID currentUserId, String userRoles);

	void validateBranchAccess(UUID branchId, UUID currentUserId, String userRoles);

	void validatePermissions(UUID entityId, UUID currentUserId, String userRoles);
}