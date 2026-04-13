package com.tuttime.restaurant.service.impl;

import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.restaurant.dto.BranchDto;
import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.entity.Restaurant;
import com.tuttime.restaurant.mapper.BranchMapper;
import com.tuttime.restaurant.repository.BranchRepository;
import com.tuttime.restaurant.repository.RestaurantRepository;
import com.tuttime.restaurant.repository.RestaurantStaffRepository;
import com.tuttime.restaurant.service.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
	public BranchDto.BranchResponse createBranch(BranchDto.BranchRequest request, UUID currentUserId, String userRoles) {
		Restaurant restaurant = restaurantRepository.findByRestaurantIdAndDeletedFalse(request.restaurantId())
				.orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + request.restaurantId()));

		restaurantStaffService.validatePermissions(restaurant.getRestaurantId(), currentUserId, userRoles);

		Branch entity = branchMapper.toEntity(request);
		entity.setBranchId(UUID.randomUUID());
		entity.setRestaurantId(entity.getRestaurantId());
		entity = branchRepository.save(entity);

		return branchMapper.toResponse(entity);
	}

	@Override
	public BranchDto.BranchResponse getBranchById(UUID branchId) {
		Branch entity = branchRepository.findByBranchIdAndDeletedFalse(branchId)
				.orElseThrow(() -> new EntityNotFoundException("Branch not found: " + branchId));
		return branchMapper.toResponse(entity);
	}

	@Override
	public Page<BranchDto.BranchResponse> findAllBranches(UUID restaurantId, int page, int size) {
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
	public BranchDto.BranchResponse updateBranch(UUID branchId, BranchDto.BranchRequest request, UUID currentUserId, String userRoles) {
		Branch branch = branchRepository.findByBranchIdAndDeletedFalse(branchId)
				.orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));

		// 1. Unified Permission Check
		restaurantStaffService.validatePermissions(branch.getBranchId(), currentUserId, userRoles);

		// 2. Map updates
		branchMapper.updateEntityFromRequest(request, branch);

		// Manual mapping for the PostGIS Point
		if (request.longitude() != null && request.latitude() != null) {
			branch.setLocation(createPoint(request.longitude(), request.latitude()));
		}

		// No need for .save(branch) - Hibernate dirty checking handles it
		return branchMapper.toResponse(branch);
	}

	@Override
	public void deleteBranch(UUID branchId, UUID currentUserId, String userRoles) {
		Branch branch = branchRepository.findByBranchIdAndDeletedFalse(branchId)
				.orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));

		// 1. Unified Permission Check
		restaurantStaffService.validatePermissions(branch.getBranchId(), currentUserId, userRoles);

		// 2. Soft delete
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
			mediaService.deleteAllByEntityId(branch.getBranchId());
			scheduleService.deleteAllByEntityId(branch.getBranchId());
		}
		branchRepository.deleteAllByRestaurantId(restaurantId);

		return true;
	}
}