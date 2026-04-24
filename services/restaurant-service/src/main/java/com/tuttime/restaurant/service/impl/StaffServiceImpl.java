package com.tuttime.restaurant.service.impl;

import com.tuttime.common.enums.UserRoleKind;
import com.tuttime.common.event.kafka.EventProducer;
import com.tuttime.common.event.kafka.RestaurantEvent;
import com.tuttime.common.exception.ConflictException;
import com.tuttime.common.exception.ForbiddenException;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.common.event.proto.*;
import com.tuttime.restaurant.dto.StaffDto;
import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.entity.Restaurant;
import com.tuttime.restaurant.entity.Staff;
import com.tuttime.restaurant.grpc.client.UserGrpcClient;
import com.tuttime.restaurant.mapper.StaffMapper;
import com.tuttime.restaurant.repository.BranchRepository;
import com.tuttime.restaurant.repository.RestaurantRepository;
import com.tuttime.restaurant.repository.StaffRepository;
import com.tuttime.restaurant.service.StaffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffServiceImpl implements StaffService {

	private final RestaurantRepository restaurantRepository;
	private final StaffRepository staffRepository;
	private final BranchRepository branchRepository;
	private final StaffMapper staffMapper;
	private final UserGrpcClient userGrpClient;
	private final EventProducer eventProducer;

	@Override
	@Transactional
	public StaffDto.StaffResponse assignStaff(UUID restaurantId, StaffDto.StaffRequest request, UUID currentUserId, String userRoles) {
		boolean isAdmin = userRoles.contains(UserRoleKind.ADMIN.name());
		boolean isOwner = false;
		if (!isAdmin) {
			isOwner = validateRestaurantOwner(restaurantId, currentUserId);
		}
		if (!isAdmin && !isOwner) {
			throw new ForbiddenException("You do not have permission to manage this restaurant.");
		}

		// 2. Prevent duplicate assignments
		if (staffRepository.existsByRestaurantIdAndUserId(restaurantId, request.userId())) {
			throw new ConflictException("User is already assigned to this restaurant");
		}

		Restaurant restaurant = restaurantRepository.findById(restaurantId)
				.orElseThrow(() -> new InvalidRequestException("Restaurant not found"));

		Staff staff = staffMapper.toEntity(request);
		staff.setRestaurant(restaurant);
		if (request.branchId() != null) {
			Branch branch = branchRepository.findById(request.branchId())
					.orElseThrow(() -> new InvalidRequestException("Branch not found"));
			staff.setBranch(branch);
		}
		staff.setRoleName(request.roleName().toUpperCase());

		Staff savedStaff = staffRepository.save(staff);

		// send staff updated event
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				UUID restaurantId = (staff.getRestaurant() != null) ? staff.getRestaurant().getId() : null;
				UUID branchId = (staff.getBranch() != null) ? staff.getBranch().getId() : null;

				RestaurantEvent.StaffUpdatedEvent event = RestaurantEvent.StaffUpdatedEvent.builder()
						.staffId(staff.getId())
						.userId(staff.getUserId())
						.restaurantId(restaurantId)
						.branchId(branchId)
						.roleName(staff.getRoleName())
						.active(staff.getActive())
						.build();
				eventProducer.sendRestaurantStaffUpdatedEvent(event);
			}
		});

		return staffMapper.toResponse(savedStaff);
	}

	@Override
	@Transactional(readOnly = true)
	public List<StaffDto.StaffResponse> getStaffByRestaurant(UUID restaurantId, UUID currentUserId, String userRoles) {
		// 1. Permission Check
		validateRestaurantAccess(restaurantId, currentUserId, userRoles);

		// 2. Fetch staff mappings
		List<Staff> staffs = staffRepository.findAllByRestaurantId(restaurantId);
		if (staffs.isEmpty()) return List.of();

		// 3. Collect User IDs and fetch profiles via gRPC
		List<String> userIds = staffs.stream()
				.map(s -> s.getUserId().toString())
				.toList();

		// This calls the gRPC method we discussed to get user details
		Map<String, UserDetailResponse> profileMap = fetchUserProfiles(userIds);

		// 4. Construct the Records
		return staffs.stream().map(staff -> {
			UserDetailResponse profile = profileMap.get(staff.getUserId().toString());

			UUID branchId = (staff.getBranch() != null) ? staff.getBranch().getId() : null;

			return new StaffDto.StaffResponse(
					staff.getId(),
					staff.getRestaurant().getId(),
					branchId,
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
	@Transactional
	public StaffDto.StaffResponse updateStaff(UUID restaurantId, UUID staffId, StaffDto.StaffUpdateRequest request, UUID currentUserId, String userRoles) {
		Restaurant restaurant = restaurantRepository.findByIdAndDeletedFalse(restaurantId)
				.orElseThrow(() -> new InvalidRequestException("Restaurant not found: " + restaurantId));

		Staff staff = staffRepository.findById(staffId)
				.orElseThrow(() -> new InvalidRequestException("Staff record not found"));

		boolean isAdmin = userRoles.contains(UserRoleKind.ADMIN.name());
		boolean isOwner = false;
		if (!isAdmin) {
			isOwner = validateRestaurantOwner(restaurant.getId(), currentUserId);
		}
		if (!isAdmin && !isOwner) {
			throw new ForbiddenException("You do not have permission to manage this restaurant.");
		}

		staffMapper.updateEntityFromRequest(request, staff);
		Staff savedStaff = staffRepository.save(staff);

		// send staff updated event
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				UUID restaurantId = (savedStaff.getRestaurant() != null) ? savedStaff.getRestaurant().getId() : null;
				UUID branchId = (savedStaff.getBranch() != null) ? savedStaff.getBranch().getId() : null;

				RestaurantEvent.StaffUpdatedEvent event = RestaurantEvent.StaffUpdatedEvent.builder()
						.staffId(staff.getId())
						.userId(staff.getUserId())
						.restaurantId(restaurantId)
						.branchId(branchId)
						.roleName(staff.getRoleName())
						.active(staff.getActive())
						.build();
				eventProducer.sendRestaurantStaffUpdatedEvent(event);
			}
		});

		return staffMapper.toResponse(savedStaff);
	}

	@Override
	@Transactional
	public void removeStaff(UUID restaurantId, UUID staffId, UUID currentUserId, String userRoles) {
		Restaurant restaurant = restaurantRepository.findByIdAndDeletedFalse(restaurantId)
				.orElseThrow(() -> new InvalidRequestException("Restaurant not found: " + restaurantId));

		Staff staff = staffRepository.findById(staffId)
				.orElseThrow(() -> new InvalidRequestException("Staff not found: " + staffId));

		boolean isAdmin = userRoles.contains(UserRoleKind.ADMIN.name());
		boolean isOwner = false;
		if (!isAdmin) {
			isOwner = validateRestaurantOwner(restaurant.getId(), currentUserId);
		}
		if (!isAdmin && !isOwner) {
			throw new ForbiddenException("You do not have permission to manage this restaurant.");
		}

		// send staff updated event
		staff.setActive(false);

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				UUID restaurantId = (staff.getRestaurant() != null) ? staff.getRestaurant().getId() : null;
				UUID branchId = (staff.getBranch() != null) ? staff.getBranch().getId() : null;

				RestaurantEvent.StaffUpdatedEvent event = RestaurantEvent.StaffUpdatedEvent.builder()
						.staffId(staff.getId())
						.userId(staff.getUserId())
						.restaurantId(restaurantId)
						.branchId(branchId)
						.roleName(staff.getRoleName())
						.active(staff.getActive())
						.build();
				eventProducer.sendRestaurantStaffUpdatedEvent(event);
			}
		});

		staffRepository.delete(staff);
	}

	@Override
	@Transactional(readOnly = true)
	public void validateAdminAccess(UUID currentUserId) {
		if (currentUserId == null)
			throw new ForbiddenException("You do not have permission to manage this restaurant.");

		UserValidRequest request = UserValidRequest.newBuilder()
				.setUserId(currentUserId.toString())
				.build();

		UserValidResponse response = userGrpClient.validateUser(request);
		if (!response.getValid() || !response.getRoles().contains(UserRoleKind.ADMIN.name()))
			throw new InvalidRequestException("Current User not found: " + currentUserId);
	}

	@Override
	@Transactional(readOnly = true)
	public void validateAdminOrOwnerAccess(UUID currentUserId) {
		if (currentUserId == null)
			throw new ForbiddenException("You do not have permission to manage this restaurant.");

		UserValidRequest request = UserValidRequest.newBuilder()
				.setUserId(currentUserId.toString())
				.build();

		UserValidResponse response = userGrpClient.validateUser(request);
		if (!response.getValid() || !response.getRoles().contains(UserRoleKind.OWNER.name()))
			throw new InvalidRequestException("Current User not found: " + currentUserId);
	}

	/**
	 * Shared helper to validate if a user can manage a specific restaurant
	 */
	@Override
	@Transactional(readOnly = true)
	public void validateRestaurantAccess(UUID restaurantId, UUID currentUserId, String userRoles) {
		if (userRoles.contains(UserRoleKind.ADMIN.name())) return;

		if (!validateRestaurantOwner(restaurantId, currentUserId)) {
			throw new ForbiddenException("You do not have permission to manage this restaurant.");
		}
	}

	/**
	 * Shared helper to validate if a user can manage a specific branch
	 */
	@Override
	@Transactional(readOnly = true)
	public void validateBranchAccess(UUID branchId, UUID currentUserId, String userRoles) {
		if (userRoles.contains(UserRoleKind.ADMIN.name())) return;

		// Check if entity is a Branch
		var branch = branchRepository.findByIdAndDeletedFalse(branchId)
				.orElseThrow(() -> new InvalidRequestException("Entity not found or access denied."));
		boolean isOwner = validateRestaurantOwner(branch.getRestaurant().getId(), currentUserId);
		if (isOwner) return;

		boolean isManager = validateBranchManager(branchId, currentUserId);
		if (!isManager) {
			throw new ForbiddenException("You do not have permission to manage this branch.");
		}
	}

	@Override
	@Transactional(readOnly = true)
	public void validatePermissions(UUID entityId, UUID currentUserId, String userRoles) {
		// Early exit for Admins
		if (userRoles.contains(UserRoleKind.ADMIN.name())) return;

		// Check if entity is a Restaurant
		var restaurantOpt = restaurantRepository.findByIdAndDeletedFalse(entityId);
		if (restaurantOpt.isPresent()) {
			if (!validateRestaurantOwner(entityId, currentUserId)) {
				throw new ForbiddenException("You do not have permission to manage this restaurant.");
			}
			return;
		}

		// Check if entity is a Branch
		var branch = branchRepository.findByIdAndDeletedFalse(entityId)
				.orElseThrow(() -> new InvalidRequestException("Entity not found or access denied."));

		boolean isOwner = validateRestaurantOwner(branch.getRestaurant().getId(), currentUserId);
		boolean isManager = isOwner || validateBranchManager(branch.getId(), currentUserId);

		if (!isManager) {
			throw new ForbiddenException("You do not have permission to manage this branch.");
		}
	}

	private boolean validateRestaurantOwner(UUID restaurantId, UUID currentUserId) {
		if (restaurantId == null || currentUserId == null) return false;

		UserValidRequest request = UserValidRequest.newBuilder()
				.setUserId(currentUserId.toString())
				.build();
		UserValidResponse response = userGrpClient.validateUser(request);
		if (!response.getValid() || !response.getRoles().contains(UserRoleKind.OWNER.name()))
			return false;

		return staffRepository.existsByRestaurantIdAndUserIdAndRoleName(
				restaurantId, currentUserId, UserRoleKind.OWNER.name());
	}

	private boolean validateBranchManager(UUID branchId, UUID currentUserId) {
		if (branchId == null || currentUserId == null) return false;

		UserValidRequest request = UserValidRequest.newBuilder()
				.setUserId(currentUserId.toString())
				.build();
		UserValidResponse response = userGrpClient.validateUser(request);
		if (!response.getValid() || !response.getRoles().contains(UserRoleKind.MANAGER.name()))
			return false;

		return staffRepository.existsByBranchIdAndUserIdAndRoleName(
				branchId, currentUserId, UserRoleKind.MANAGER.name());
	}

	private Map<String, UserDetailResponse> fetchUserProfiles(List<String> userIds) {
		try {
			UserDetailsRequest request = UserDetailsRequest.newBuilder().addAllUserIds(userIds).build();
			UserDetailsResponse response = userGrpClient.getUserDetails(request);

			return response.getProfilesList().stream()
					.collect(Collectors.toMap(UserDetailResponse::getUserId, p -> p));
		} catch (Exception e) {
			log.error("Failed to fetch user profiles via gRPC", e);
			return Collections.emptyMap();
		}
	}
}
