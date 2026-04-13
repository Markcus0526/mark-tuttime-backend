package com.tuttime.restaurant.service.impl;

import com.tuttime.common.auth.enums.UserRoleKind;
import com.tuttime.common.exception.ConflictException;
import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.common.proto.UserValidResponse;
import com.tuttime.restaurant.dto.StaffDto;
import com.tuttime.restaurant.entity.Media;
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
import org.apache.tomcat.websocket.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
		if (!isAdmin) {
			isOwner = validateRestaurantOwner(restaurantId, currentUserId);
		}
		if (!isAdmin && !isOwner) {
			throw new AccessDeniedException("You do not have permission to manage this restaurant.");
		}

		// 2. Prevent duplicate assignments
		if (staffRepository.existsByRestaurantIdAndUserId(restaurantId, request.userId())) {
			throw new ConflictException("User is already assigned to this restaurant");
		}

		RestaurantStaff staff = staffMapper.toEntity(request);
		staff.setRestaurantId(restaurantId);
		staff.setRoleName(request.roleName().toUpperCase());

		return staffMapper.toResponse(staffRepository.save(staff));
	}

	@Override
	@Transactional(readOnly = true)
	public List<StaffDto.StaffResponse> getStaffByRestaurant(UUID restaurantId, UUID currentUserId, String userRoles) {
		Restaurant restaurant = restaurantRepository.findByRestaurantIdAndDeletedFalse(restaurantId)
				.orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + restaurantId));

		boolean isAdmin = userRoles.contains(UserRoleKind.ADMIN.name());
		boolean isOwner = false;
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

		staffMapper.updateEntityFromRequest(request, staff);

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
	public void validateAdminAccess(UUID currentUserId) {
		if (currentUserId == null)
			throw new AccessDeniedException("You do not have permission to manage this restaurant.");

		UserValidResponse response = userGrpClient.validateUserById(currentUserId);
		if (!response.getIsValid() || !response.getRoles().contains(UserRoleKind.ADMIN.name()))
			throw new ResourceNotFoundException("Current User not found: " + currentUserId);
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
