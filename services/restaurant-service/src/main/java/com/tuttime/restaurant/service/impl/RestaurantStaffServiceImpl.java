package com.tuttime.restaurant.service.impl;

import com.tuttime.common.auth.enums.UserRoleKind;
import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.common.proto.UserValidResponse;
import com.tuttime.restaurant.dto.StaffDto;
import com.tuttime.restaurant.entity.Restaurant;
import com.tuttime.restaurant.entity.RestaurantStaff;
import com.tuttime.restaurant.grpc.client.UserGrpcClient;
import com.tuttime.restaurant.mapper.RestaurantStaffMapper;
import com.tuttime.restaurant.repository.BranchRepository;
import com.tuttime.restaurant.repository.RestaurantRepository;
import com.tuttime.restaurant.repository.RestaurantStaffRepository;
import com.tuttime.restaurant.service.RestaurantStaffService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
	public StaffDto.StaffResponse assignStaff(UUID restaurantId, StaffDto.StaffRequest request, UUID currentUserId, String userRoles) {
		boolean isAdmin = userRoles.contains(UserRoleKind.ADMIN.name());
		boolean isOwner = false;
		boolean isManager = false;
		if (!isAdmin) {
			isOwner = validateRestaurantOwner(restaurantId, currentUserId);
		}
		if (!isAdmin && !isOwner && !isManager) {
			throw new AccessDeniedException("You do not have permission to manage this restaurant.");
		}
//		if (isManager && (request.roleName().equals(UserRoleKind.ADMIN.name())
//				|| request.roleName().equals(UserRoleKind.OWNER.name())
//				|| request.roleName().equals(UserRoleKind.MANAGER.name()))) {
//			throw new AccessDeniedException("Only the Branch Manager can appoint Branch Managers.");
//		}

		// 2. Prevent duplicate assignments
		if (staffRepository.existsByRestaurantIdAndUserId(restaurantId, request.userId())) {
			throw new IllegalStateException("User is already assigned to this restaurant");
		}

		RestaurantStaff staff = new RestaurantStaff();
		staff.setRestaurantId(restaurantId);
		staff.setUserId(request.userId());
		staff.setRoleName(request.roleName().toUpperCase());

		// 3. Link to Branch if specified (Hierarchy: Manager -> Branch)
//		if (isManager) {
//			Branch branch = branchRepository.findAllByBranchIdAndDeletedFalse(request.branchId())
//					.orElseThrow(() -> new EntityNotFoundException("Branch not found"));
//			staff.setBranchId(branch.getBranchId());
//		}

		return staffMapper.toResponse(staffRepository.save(staff));
	}

	@Override
	@Transactional(readOnly = true)
	public List<StaffDto.StaffResponse> getStaffByRestaurant(UUID restaurantId, UUID currentUserId, String userRoles) {
		Restaurant restaurant = restaurantRepository.findByRestaurantIdAndDeletedFalse(restaurantId)
				.orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + restaurantId));

		boolean isAdmin = userRoles.contains(UserRoleKind.ADMIN.name());
		boolean isOwner = false;
		boolean isManager = false;
		if (!isAdmin) {
			isOwner = validateRestaurantOwner(restaurant.getRestaurantId(), currentUserId);
		}
		if (!isAdmin && !isOwner) {
			throw new AccessDeniedException("You do not have permission to manage this restaurant.");
		}

		List<RestaurantStaff> staffs = staffRepository.findAllByRestaurantId(restaurantId);
		return staffs.stream().map(staffMapper::toResponse).toList();
	}

	@Override
	public StaffDto.StaffResponse updateStaff(UUID restaurantId, UUID staffId, StaffDto.StaffUpdateRequest request, UUID currentUserId, String userRoles) {
		Restaurant restaurant = restaurantRepository.findByRestaurantIdAndDeletedFalse(restaurantId)
				.orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + restaurantId));

		RestaurantStaff staff = staffRepository.findByStaffId(staffId)
				.orElseThrow(() -> new EntityNotFoundException("Staff record not found"));

		boolean isAdmin = userRoles.contains(UserRoleKind.ADMIN.name());
		boolean isOwner = false;
		if (!isAdmin) {
			isOwner = validateRestaurantOwner(restaurant.getRestaurantId(), currentUserId);
		}
		if (!isAdmin && !isOwner) {
			throw new AccessDeniedException("You do not have permission to manage this restaurant.");
		}

		staff.setRoleName(request.roleName().toUpperCase());
		if (request.active() != null) staff.setActive(request.active());

		return staffMapper.toResponse(staffRepository.save(staff));
	}

	@Override
	public void removeStaff(UUID restaurantId, UUID staffId, UUID currentUserId, String userRoles) {
		Restaurant restaurant = restaurantRepository.findByRestaurantIdAndDeletedFalse(restaurantId)
				.orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + restaurantId));

		RestaurantStaff staff = staffRepository.findByStaffId(staffId)
				.orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + staffId));

		boolean isAdmin = userRoles.contains(UserRoleKind.ADMIN.name());
		boolean isOwner = false;
		if (!isAdmin) {
			isOwner = validateRestaurantOwner(restaurant.getRestaurantId(), currentUserId);
		}
		if (!isAdmin && !isOwner) {
			throw new AccessDeniedException("You do not have permission to manage this restaurant.");
		}

		staffRepository.delete(staff);
	}

	@Override
	public boolean validateAdminAccess(UUID currentUserId) {
		if (currentUserId == null) return false;

		UserValidResponse response = userGrpClient.validateUserById(currentUserId);
		if (!response.getIsValid() || !response.getRoles().contains(UserRoleKind.ADMIN.name()))
			throw new ResourceNotFoundException("Current User not found: " + currentUserId);

		return true;
	}

	/**
	 * Shared helper to validate if a user can manage a specific restaurant
	 */
	@Override
	public void validateRestaurantAccess(UUID restaurantId, UUID currentUserId, String userRoles) {
		if (userRoles.contains(UserRoleKind.ADMIN.name())) return;

		if (!validateRestaurantOwner(restaurantId, currentUserId)) {
			throw new AccessDeniedException("You do not have permission to manage this restaurant.");
		}
	}

	/**
	 * Shared helper to validate if a user can manage a specific branch
	 */
	@Override
	public void validateBranchAccess(UUID branchId, UUID currentUserId, String userRoles) {
		if (userRoles.contains(UserRoleKind.ADMIN.name())) return;

		// Check if entity is a Branch
		var branch = branchRepository.findByBranchIdAndDeletedFalse(branchId)
				.orElseThrow(() -> new AccessDeniedException("Entity not found or access denied."));
		boolean isOwner = validateRestaurantOwner(branch.getRestaurantId(), currentUserId);
		if (isOwner) return;

		boolean isManager = validateBranchManager(branchId, currentUserId);
		if (!isManager) {
			throw new AccessDeniedException("You do not have permission to manage this branch.");
		}
	}

	@Override
	public void validatePermissions(UUID entityId, UUID currentUserId, String userRoles) {
		// Early exit for Admins
		if (userRoles.contains(UserRoleKind.ADMIN.name())) return;

		// Check if entity is a Restaurant
		var restaurantOpt = restaurantRepository.findByRestaurantIdAndDeletedFalse(entityId);
		if (restaurantOpt.isPresent()) {
			if (!validateRestaurantOwner(entityId, currentUserId)) {
				throw new AccessDeniedException("You do not have permission to manage this restaurant.");
			}
			return;
		}

		// Check if entity is a Branch
		var branch = branchRepository.findByBranchIdAndDeletedFalse(entityId)
				.orElseThrow(() -> new AccessDeniedException("Entity not found or access denied."));

		boolean isOwner = validateRestaurantOwner(branch.getRestaurantId(), currentUserId);
		boolean isManager = isOwner || validateBranchManager(branch.getBranchId(), currentUserId);

		if (!isManager) {
			throw new AccessDeniedException("You do not have permission to manage this branch.");
		}
	}

	private boolean validateRestaurantOwner(UUID restaurantId, UUID currentUserId) {
		if (restaurantId == null || currentUserId == null) return false;

		UserValidResponse response = userGrpClient.validateUserById(currentUserId);
		if (!response.getIsValid() || !response.getRoles().contains(UserRoleKind.OWNER.name()))
			return false;

		return staffRepository.existsByRestaurantIdAndUserIdAndRoleName(
				restaurantId, currentUserId, UserRoleKind.OWNER.name());
	}

	private boolean validateBranchManager(UUID branchId, UUID currentUserId) {
		if (branchId == null || currentUserId == null) return false;

		UserValidResponse response = userGrpClient.validateUserById(currentUserId);
		if (!response.getIsValid() || !response.getRoles().contains(UserRoleKind.MANAGER.name()))
			return false;

		return staffRepository.existsByBranchIdAndUserIdAndRoleName(
				branchId, currentUserId, UserRoleKind.MANAGER.name());
	}
}
