package com.tuttime.restaurant.service.impl;

import com.tuttime.common.auth.enums.UserRoleKind;
import com.tuttime.common.exception.AuthenticationException;
import com.tuttime.common.exception.ConflictException;
import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.common.proto.*;
import com.tuttime.restaurant.dto.StaffDto;
import com.tuttime.restaurant.entity.Restaurant;
import com.tuttime.restaurant.entity.RestaurantStaff;
import com.tuttime.restaurant.grpc.client.UserGrpcClient;
import com.tuttime.restaurant.kafka.RestaurantEventProducer;
import com.tuttime.restaurant.mapper.RestaurantStaffMapper;
import com.tuttime.restaurant.repository.BranchRepository;
import com.tuttime.restaurant.repository.RestaurantRepository;
import com.tuttime.restaurant.repository.RestaurantStaffRepository;
import com.tuttime.restaurant.service.RestaurantStaffService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class RestaurantStaffServiceImpl implements RestaurantStaffService {

	private final RestaurantRepository restaurantRepository;
	private final RestaurantStaffRepository staffRepository;
	private final BranchRepository branchRepository;
	private final RestaurantStaffMapper staffMapper;
	private final UserGrpcClient userGrpClient;
	private final RestaurantEventProducer restaurantEventProducer;

	@Override
	public StaffDto.StaffResponse assignStaff(UUID restaurantId, StaffDto.StaffRequest request, UUID currentUserId, String userRoles) {
		boolean isAdmin = userRoles.contains(UserRoleKind.ADMIN.name());
		boolean isOwner = false;
		if (!isAdmin) {
			isOwner = validateRestaurantOwner(restaurantId, currentUserId);
		}
		if (!isAdmin && !isOwner) {
			throw new AuthenticationException("You do not have permission to manage this restaurant.");
		}

		// 2. Prevent duplicate assignments
		if (staffRepository.existsByRestaurantIdAndUserId(restaurantId, request.userId())) {
			throw new ConflictException("User is already assigned to this restaurant");
		}

		RestaurantStaff staff = staffMapper.toEntity(request);
		staff.setRestaurantId(restaurantId);
		staff.setRoleName(request.roleName().toUpperCase());

		RestaurantStaff savedStaff = staffRepository.save(staff);

		// send staff updated event
		restaurantEventProducer.sendStaffUpdated(savedStaff);

		return staffMapper.toResponse(savedStaff);
	}

	@Override
	@Transactional(readOnly = true)
	public List<StaffDto.StaffResponse> getStaffByRestaurant(UUID restaurantId, UUID currentUserId, String userRoles) {
		// 1. Permission Check
		validateRestaurantAccess(restaurantId, currentUserId, userRoles);

		// 2. Fetch staff mappings
		List<RestaurantStaff> staffs = staffRepository.findAllByRestaurantId(restaurantId);
		if (staffs.isEmpty()) return List.of();

		// 3. Collect User IDs and fetch profiles via gRPC
		List<String> userIds = staffs.stream()
				.map(s -> s.getUserId().toString())
				.toList();

		// This calls the gRPC method we discussed to get user details
		Map<String, UserProfileResponse> profileMap = fetchUserProfiles(userIds);

		// 4. Construct the Records
		return staffs.stream().map(staff -> {
			UserProfileResponse profile = profileMap.get(staff.getUserId().toString());

			return new StaffDto.StaffResponse(
					staff.getStaffId(),
					staff.getRestaurantId(),
					staff.getBranchId(),
					staff.getUserId(),
					profile != null ? profile.getFullName() : "Unknown",
					profile != null ? profile.getPhoneNumber() : null,
					profile != null ? profile.getEmail() : null,
					staff.getRoleName(),
					staff.getActive(),
					staff.getCreatedAt()
			);
		}).toList();
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
			throw new AuthenticationException("You do not have permission to manage this restaurant.");
		}

		staffMapper.updateEntityFromRequest(request, staff);
		RestaurantStaff savedStaff = staffRepository.save(staff);

		// send staff updated event
		restaurantEventProducer.sendStaffUpdated(savedStaff);

		return staffMapper.toResponse(savedStaff);
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
			throw new AuthenticationException("You do not have permission to manage this restaurant.");
		}

		// send staff updated event
		staff.setActive(false);
		restaurantEventProducer.sendStaffUpdated(staff);

		staffRepository.delete(staff);
	}

	@Override
	public void validateAdminAccess(UUID currentUserId) {
		if (currentUserId == null)
			throw new AuthenticationException("You do not have permission to manage this restaurant.");

		UserByIdRequest request = UserByIdRequest.newBuilder()
				.setUserId(currentUserId.toString())
				.build();

		UserValidResponse response = userGrpClient.validateUserById(request);
		if (!response.getValid() || !response.getRoles().contains(UserRoleKind.ADMIN.name()))
			throw new ResourceNotFoundException("Current User not found: " + currentUserId);
	}

	/**
	 * Shared helper to validate if a user can manage a specific restaurant
	 */
	@Override
	public void validateRestaurantAccess(UUID restaurantId, UUID currentUserId, String userRoles) {
		if (userRoles.contains(UserRoleKind.ADMIN.name())) return;

		if (!validateRestaurantOwner(restaurantId, currentUserId)) {
			throw new AuthenticationException("You do not have permission to manage this restaurant.");
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
				.orElseThrow(() -> new AuthenticationException("Entity not found or access denied."));
		boolean isOwner = validateRestaurantOwner(branch.getRestaurantId(), currentUserId);
		if (isOwner) return;

		boolean isManager = validateBranchManager(branchId, currentUserId);
		if (!isManager) {
			throw new AuthenticationException("You do not have permission to manage this branch.");
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
				throw new AuthenticationException("You do not have permission to manage this restaurant.");
			}
			return;
		}

		// Check if entity is a Branch
		var branch = branchRepository.findByBranchIdAndDeletedFalse(entityId)
				.orElseThrow(() -> new AuthenticationException("Entity not found or access denied."));

		boolean isOwner = validateRestaurantOwner(branch.getRestaurantId(), currentUserId);
		boolean isManager = isOwner || validateBranchManager(branch.getBranchId(), currentUserId);

		if (!isManager) {
			throw new AuthenticationException("You do not have permission to manage this branch.");
		}
	}

	private boolean validateRestaurantOwner(UUID restaurantId, UUID currentUserId) {
		if (restaurantId == null || currentUserId == null) return false;

		UserByIdRequest request = UserByIdRequest.newBuilder()
				.setUserId(currentUserId.toString())
				.build();
		UserValidResponse response = userGrpClient.validateUserById(request);
		if (!response.getValid() || !response.getRoles().contains(UserRoleKind.OWNER.name()))
			return false;

		return staffRepository.existsByRestaurantIdAndUserIdAndRoleName(
				restaurantId, currentUserId, UserRoleKind.OWNER.name());
	}

	private boolean validateBranchManager(UUID branchId, UUID currentUserId) {
		if (branchId == null || currentUserId == null) return false;

		UserByIdRequest request = UserByIdRequest.newBuilder()
				.setUserId(currentUserId.toString())
				.build();
		UserValidResponse response = userGrpClient.validateUserById(request);
		if (!response.getValid() || !response.getRoles().contains(UserRoleKind.MANAGER.name()))
			return false;

		return staffRepository.existsByBranchIdAndUserIdAndRoleName(
				branchId, currentUserId, UserRoleKind.MANAGER.name());
	}

	private Map<String, UserProfileResponse> fetchUserProfiles(List<String> userIds) {
		try {
			UserByIdsRequest request = UserByIdsRequest.newBuilder().addAllUserIds(userIds).build();
			UserProfilesResponse response = userGrpClient.getUserProfiles(request);

			return response.getProfilesList().stream()
					.collect(Collectors.toMap(UserProfileResponse::getUserId, p -> p));
		} catch (Exception e) {
			log.error("Failed to fetch user profiles via gRPC", e);
			return Collections.emptyMap();
		}
	}
}
