package com.tuttime.restaurant.service.impl;

import com.tuttime.common.exception.MethodArgumentNotValidException;
import com.tuttime.restaurant.dto.BranchDto;
import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.entity.Restaurant;
import com.tuttime.restaurant.mapper.BranchMapper;
import com.tuttime.restaurant.repository.BranchRepository;
import com.tuttime.restaurant.repository.RestaurantRepository;
import com.tuttime.restaurant.repository.StaffRepository;
import com.tuttime.restaurant.service.*;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.tuttime.restaurant.util.GeometryUtil.createPoint;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class BranchServiceImpl implements BranchService {

	private final StaffService staffService;
	private final BranchRepository branchRepository;
	private final RestaurantRepository restaurantRepository;
	private final StaffRepository staffRepository;
	private final BranchMapper branchMapper;
	private final ZoneService zoneService;
	private final MediaService mediaService;
	private final ScheduleService scheduleService;

	@Override
	@Transactional
	public BranchDto.BranchResponse createBranch(BranchDto.BranchRequest request, UUID currentUserId, String userRoles) {
		Restaurant restaurant = restaurantRepository.findByIdAndDeletedFalse(request.restaurantId())
				.orElseThrow(() -> new MethodArgumentNotValidException("Restaurant not found: " + request.restaurantId()));

		staffService.validatePermissions(restaurant.getId(), currentUserId, userRoles);

		Branch entity = branchMapper.toEntity(request);
		entity.setId(UUID.randomUUID());
		entity.setRestaurant(restaurant);
		entity = branchRepository.save(entity);

		return branchMapper.toResponse(entity);
	}

	@Override
	public BranchDto.BranchResponse getBranchById(UUID branchId) {
		Branch entity = branchRepository.findByIdAndDeletedFalse(branchId)
				.orElseThrow(() -> new MethodArgumentNotValidException("Branch not found: " + branchId));
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
	@Transactional
	public BranchDto.BranchResponse updateBranch(UUID branchId, BranchDto.BranchRequest request, UUID currentUserId, String userRoles) {
		Branch branch = branchRepository.findByIdAndDeletedFalse(branchId)
				.orElseThrow(() -> new MethodArgumentNotValidException("Branch not found: " + branchId));

		// 1. Unified Permission Check
		staffService.validatePermissions(branch.getId(), currentUserId, userRoles);

		// 2. Map updates
		branchMapper.updateEntityFromRequest(request, branch);

		// No need for .save(branch) - Hibernate dirty checking handles it
		return branchMapper.toResponse(branch);
	}

	@Override
	@Transactional
	public void deleteBranch(UUID branchId, UUID currentUserId, String userRoles) {
		Branch branch = branchRepository.findByIdAndDeletedFalse(branchId)
				.orElseThrow(() -> new MethodArgumentNotValidException("Branch not found: " + branchId));

		// 1. Unified Permission Check
		staffService.validatePermissions(branch.getId(), currentUserId, userRoles);

		// 2. Soft delete
		branch.setDeleted(true);
	}

	@Override
	@Transactional
	public boolean deleteAllByRestaurantId(UUID restaurantId) {
		List<Branch> branches = branchRepository.findAllByRestaurantIdAndDeletedFalse(restaurantId);
		if (branches.isEmpty()) {
			return true;
		}

		for (Branch branch : branches) {
			if (!zoneService.deleteAllByBranchId(branch.getId())) {
				return false;
			}
			mediaService.deleteAllByEntityId(branch.getId());
			scheduleService.deleteAllByEntityId(branch.getId());
		}
		branchRepository.deleteAllByRestaurantId(restaurantId);

		return true;
	}
}