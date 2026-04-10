package com.tuttime.restaurant.service.impl;

import com.drew.lang.StringUtil;
import com.tuttime.common.auth.enums.UserRoleKind;
import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.common.proto.UserValidResponse;
import com.tuttime.common.util.UuidUtil;
import com.tuttime.restaurant.entity.*;
import com.tuttime.restaurant.mapper.RestaurantStaffMapper;
import com.tuttime.restaurant.repository.*;
import com.tuttime.restaurant.service.*;
import com.tuttime.restaurant.grpc.client.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class RestaurantStaffServiceImpl implements RestaurantStaffService {

	private final RestaurantRepository restaurantRepository;
	private final RestaurantStaffRepository staffRepository;
	private final BranchRepository branchRepository;
	private final RestaurantStaffMapper staffMapper;
	private final UserGrpcClient userGrpClient;

	@Override
	public StaffResponse assignStaff(UUID restaurantId, StaffRequest request, UUID currentUserId, String userRoles) {
		boolean isAdmin = userRoles.contains(UserRoleKind.ADMIN.name());
		boolean isOwner = false;
		boolean isManager = false;
		if (!isAdmin) {
			isOwner = validateRestaurantOwner(restaurantId, currentUserId);
			if (!isOwner) isManager = validateBranchManager(request.branchId(), currentUserId);
		}
		if (!isAdmin && !isOwner && !isManager) {
			throw new AccessDeniedException("You do not have permission to manage this restaurant.");
		}
		if (!UserRoleKind.isValid(request.roleName())) {
			throw new IllegalStateException("You do not have valid user role to manage this restaurant.");
		}
		if (isManager && (request.roleName().equals(UserRoleKind.ADMIN.name())
				|| request.roleName().equals(UserRoleKind.OWNER.name())
				|| request.roleName().equals(UserRoleKind.MANAGER.name()))) {
			throw new AccessDeniedException("Only the Branch Manager can appoint Branch Managers.");
		}

		// 2. Prevent duplicate assignments
		if (staffRepository.existsByRestaurantIdAndUserId(restaurantId, request.userId())) {
			throw new IllegalStateException("User is already assigned to this restaurant");
		}

		RestaurantStaff staff = new RestaurantStaff();
		staff.setRestaurantId(restaurantId);
		staff.setUserId(request.userId());
		staff.setRoleName(request.roleName().toUpperCase());

		// 3. Link to Branch if specified (Hierarchy: Manager -> Branch)
		if (isManager) {
			Branch branch = branchRepository.findByBranchIdAndIsDeletedFalse(request.branchId())
					.orElseThrow(() -> new EntityNotFoundException("Branch not found"));
			staff.setBranchId(branch.getBranchId());
		}

		return staffMapper.toResponse(staffRepository.save(staff));
	}

	@Override
	@Transactional(readOnly = true)
	public List<StaffResponse> getStaffByRestaurant(UUID restaurantId, UUID currentUserId, String userRoles) {
		Restaurant restaurant = restaurantRepository.findByRestaurantIdAndIsDeletedFalse(restaurantId)
				.orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + restaurantId));

		boolean isAdmin = userRoles.contains(UserRoleKind.ADMIN.name());
		boolean isOwner = staffRepository.existsByRestaurantIdAndUserIdAndRoleName(
				restaurant.getRestaurantId(), currentUserId, UserRoleKind.OWNER.name());
		if (!isAdmin && !isOwner) {
			throw new AccessDeniedException("You do not have permission to manage this restaurant.");
		}

		List<RestaurantStaff> staffs = staffRepository.findByRestaurantId(restaurantId);
		return staffs.stream().map(staffMapper::toResponse).toList();
	}

	@Override
	public StaffResponse updateStaff(UUID restaurantId, UUID staffId, StaffUpdateRequest request, UUID currentUserId, String userRoles) {
		Restaurant restaurant = restaurantRepository.findByRestaurantIdAndIsDeletedFalse(restaurantId)
				.orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + restaurantId));

		boolean isAdmin = userRoles.contains(UserRoleKind.ADMIN.name());
		boolean isOwner = staffRepository.existsByRestaurantIdAndUserIdAndRoleName(
				restaurant.getRestaurantId(), currentUserId, UserRoleKind.OWNER.name());
		if (!isAdmin && !isOwner) {
			throw new AccessDeniedException("You do not have permission to manage this restaurant.");
		}
		if (!UserRoleKind.isValid(request.roleName().toUpperCase())) {
			throw new IllegalStateException("You do not have valid user role to manage this restaurant.");
		}

		RestaurantStaff staff = staffRepository.findByStaffId(staffId)
				.orElseThrow(() -> new EntityNotFoundException("Staff record not found"));

		staff.setRoleName(request.roleName().toUpperCase());
		if (request.isActive() != null) staff.setActive(request.isActive());

		return staffMapper.toResponse(staffRepository.save(staff));
	}

	@Override
	public void removeStaff(UUID restaurantId, UUID staffId, UUID currentUserId, String userRoles) {
		RestaurantStaff staff = staffRepository.findByStaffId(staffId)
				.orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + staffId));

		Restaurant restaurant = restaurantRepository.findByRestaurantIdAndIsDeletedFalse(restaurantId)
				.orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + restaurantId));

		boolean isAdmin = userRoles.contains(UserRoleKind.ADMIN.name());
		boolean isOwner = staffRepository.existsByRestaurantIdAndUserId(restaurant.getRestaurantId(), currentUserId);
		if (!isAdmin && !isOwner) {
			throw new AccessDeniedException("You do not have permission to manage this restaurant.");
		}

		if (!staffRepository.existsById(staffId)) {
			throw new EntityNotFoundException("Staff record not found");
		}

		staffRepository.delete(staff);
	}

	@Override
	public boolean validateAdmin(UUID currentUserId) {
		if (currentUserId == null) return false;

		UserValidResponse response = userGrpClient.validateUserById(currentUserId);
		if (!response.getIsValid() || !response.getRoles().contains(UserRoleKind.ADMIN.name()))
			throw new ResourceNotFoundException("Current User not found: " + currentUserId);

		return true;
	}

	@Override
	public boolean validateRestaurantOwner(UUID restaurantId, UUID currentUserId) {
		if (restaurantId == null || currentUserId == null) return false;

		restaurantRepository.findByRestaurantIdAndIsDeletedFalse(restaurantId)
				.orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + restaurantId));

		UserValidResponse response = userGrpClient.validateUserById(currentUserId);
		if (!response.getIsValid() || !response.getRoles().contains(UserRoleKind.ADMIN.name()))
			throw new ResourceNotFoundException("Current User not found: " + currentUserId);

		return staffRepository.existsByRestaurantIdAndUserIdAndRoleName(
				restaurantId, currentUserId, UserRoleKind.OWNER.name());
	}

	@Override
	public boolean validateBranchManager(UUID branchId, UUID currentUserId) {
		if (branchId == null || currentUserId == null) return false;

		branchRepository.findByBranchIdAndIsDeletedFalse(branchId)
				.orElseThrow(() -> new ResourceNotFoundException("Current Branch not found: " + branchId));

		UserValidResponse response = userGrpClient.validateUserById(currentUserId);
		if (!response.getIsValid() || !response.getRoles().contains(UserRoleKind.ADMIN.name()))
			throw new ResourceNotFoundException("Current User not found: " + currentUserId);

		return staffRepository.existsByBranchIdAndUserIdAndRoleName(
				branchId, currentUserId, UserRoleKind.MANAGER.name());
	}
}
