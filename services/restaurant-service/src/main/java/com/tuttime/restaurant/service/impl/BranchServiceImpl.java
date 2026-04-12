package com.tuttime.restaurant.service.impl;

import com.tuttime.common.auth.enums.UserRoleKind;
import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.restaurant.dto.*;
import com.tuttime.restaurant.entity.*;
import com.tuttime.restaurant.entity.enums.EntityType;
import com.tuttime.restaurant.mapper.*;
import com.tuttime.restaurant.repository.*;
import com.tuttime.restaurant.service.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static com.tuttime.restaurant.util.GeometryUtil.createPoint;

@Service
@AllArgsConstructor
public class BranchServiceImpl implements BranchService {

	private final RestaurantStaffService restaurantStaffService;
	private final BranchRepository branchRepository;
	private final RestaurantRepository restaurantRepository;
	private final RestaurantStaffRepository staffRepository;
	private final BranchMapper branchMapper;
	private final ZoneService zoneService;
	private final MediaService mediaService;
	private final ScheduleService scheduleService;

	@Override
	public BranchResponse createBranch(BranchRequest request, UUID currentUserId, String userRoles) {
		Restaurant restaurant = restaurantRepository.findByRestaurantIdAndDeletedFalse(request.getRestaurantId())
				.orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + request.getRestaurantId()));

		boolean isAdmin = userRoles.contains(UserRoleKind.ADMIN.name());
		boolean isOwner = false;
		if (!isAdmin) {
			isOwner = restaurantStaffService.validateRestaurantOwner(restaurant.getRestaurantId(), currentUserId);
		}
		if (!isAdmin && !isOwner) {
			throw new AccessDeniedException("You do not have permission to manage this restaurant.");
		}

		Branch entity = branchMapper.toEntity(request);
		entity.setBranchId(UUID.randomUUID());
		entity.setRestaurantId(entity.getRestaurantId());
		entity = branchRepository.save(entity);

		return branchMapper.toResponse(entity);
	}

	@Override
	public BranchResponse getBranchById(UUID branchId) {
		Branch entity = branchRepository.findByBranchIdAndDeletedFalse(branchId)
				.orElseThrow(() -> new EntityNotFoundException("Branch not found: " + branchId));
		return branchMapper.toResponse(entity);
	}

	@Override
	public Page<BranchResponse> findAllBranches(UUID restaurantId, int page, int size) {
		Pageable pageable = PageRequest.of(page, size);
		Page<Branch> branchPage;

		if (restaurantId != null) {
			branchPage = branchRepository.findPagesByRestaurantIdAndDeletedFalse(restaurantId, pageable);
		} else {
			branchPage = branchRepository.findByDeletedFalse(pageable);
		}

		return branchPage.map(branchMapper::toResponse);
	}

	@Override
	public BranchResponse updateBranch(UUID branchId, BranchRequest request, UUID currentUserId, String userRoles) {
		Branch branch = branchRepository.findByBranchIdAndDeletedFalse(branchId)
				.orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));

		boolean isAdmin = userRoles.contains(UserRoleKind.ADMIN.name());
		boolean isOwner = false;
		boolean isManager = false;
		if (!isAdmin) {
			isOwner = restaurantStaffService.validateRestaurantOwner(branch.getRestaurantId(), currentUserId);
			if (!isOwner) isManager = restaurantStaffService.validateBranchManager(branch.getBranchId(), currentUserId);
		}
		if (!isAdmin && !isOwner && !isManager) {
			throw new AccessDeniedException("You do not have permission to manage this restaurant.");
		}

		branch.setAddress(request.getAddress());
		branch.setPhone(request.getPhone());
		branch.setTimezone(request.getTimezone());
		branch.setRestaurantId(request.getRestaurantId());

		// Manual mapping for the PostGIS Point
		if (request.getLongitude() != null && request.getLatitude() != null) {
			branch.setLocation(createPoint(request.getLongitude(), request.getLatitude()));
		}

		branch = branchRepository.save(branch);
		return branchMapper.toResponse(branch);
	}

	@Override
	public void deleteBranch(UUID branchId, UUID currentUserId, String userRoles) {
		Branch branch = branchRepository.findByBranchIdAndDeletedFalse(branchId)
				.orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));

		boolean isAdmin = userRoles.contains(UserRoleKind.ADMIN.name());
		boolean isOwner = false;
		boolean isManager = false;
		if (!isAdmin) {
			isOwner = restaurantStaffService.validateRestaurantOwner(branch.getRestaurantId(), currentUserId);
			if (!isOwner) isManager = restaurantStaffService.validateBranchManager(branch.getBranchId(), currentUserId);
		}
		if (!isAdmin && !isOwner && !isManager) {
			throw new AccessDeniedException("You do not have permission to manage this restaurant.");
		}

		branch.setDeleted(true);
		branchRepository.save(branch);
	}

	@Override
	public boolean deleteAllByRestaurantId(UUID restaurantId) {
		List<Branch> branches = branchRepository.findAllByRestaurantIdAndDeletedFalse(restaurantId);
		if (branches.isEmpty()) {
			return true;
		}

		for (Branch branch : branches) {
			if (!zoneService.deleteAllByBranchId(branch.getBranchId())) {
				return false;
			}
			mediaService.deleteAllByEntityId(EntityType.BRANCH, branch.getBranchId());
			scheduleService.deleteAllByBranchId(branch.getBranchId());
		}
		branchRepository.deleteAllByRestaurantId(restaurantId);

		return true;
	}
}