package com.tuttime.restaurant.service.impl;

import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.exception.BusinessException;
import com.tuttime.restaurant.dto.BranchRequest;
import com.tuttime.restaurant.dto.BranchResponse;
import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.mapper.BranchMapper;
import com.tuttime.restaurant.repository.BranchRepository;
import com.tuttime.restaurant.repository.RestaurantRepository;
import com.tuttime.restaurant.service.BranchService;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.tuttime.restaurant.util.GeometryUtil.createPoint;

@Service
@AllArgsConstructor
public class BranchServiceImpl implements BranchService {

	private final BranchRepository branchRepository;
	private final RestaurantRepository restaurantRepository;
	private final BranchMapper branchMapper;

	@Override
	public BranchResponse createBranch(UUID restaurantId, BranchRequest request) {
		Branch entity = branchMapper.toEntity(request);
		entity.setBranchId(UUID.randomUUID());
		entity.setRestaurantId(restaurantId);
		entity = branchRepository.save(entity);
		return branchMapper.toResponse(entity);
	}

	@Override
	public BranchResponse getBranchById(UUID branchId) {
		Branch entity = branchRepository.findById(branchId)
				.orElseThrow(() -> new EntityNotFoundException("Branch not found: " + branchId));
		return branchMapper.toResponse(entity);
	}

	@Override
	public Page<BranchResponse> findAllBranches(UUID restaurantId, int page, int size) {
		Pageable pageable = PageRequest.of(page, size);
		Page<Branch> branchPage;

		if (restaurantId != null) {
			branchPage = branchRepository.findByRestaurantIdAndIsDeletedFalse(restaurantId, pageable);
		} else {
			branchPage = branchRepository.findByIsDeletedFalse(pageable);
		}

		return branchPage.map(branchMapper::toResponse);
	}

	@Override
	public BranchResponse updateBranch(UUID branchId, BranchRequest request) {
		Branch existing = branchRepository.findById(branchId)
				.orElseThrow(() -> new EntityNotFoundException("Branch not found: " + branchId));

		existing.setAddress(request.getAddress());
		existing.setPhone(request.getPhone());
		existing.setTimezone(request.getTimezone());
		existing.setRestaurantId(request.getRestaurantId());

		// Manual mapping for the PostGIS Point
		if (request.getLongitude() != null && request.getLatitude() != null) {
			existing.setLocation(createPoint(request.getLongitude(), request.getLatitude()));
		}

		existing = branchRepository.save(existing);
		return branchMapper.toResponse(existing);
	}

	@Override
	public void deleteBranch(UUID branchId) {
		if (!branchRepository.existsById(branchId)) {
			throw new EntityNotFoundException("Branch not found: " + branchId);
		}
		branchRepository.deleteById(branchId);
	}

	@Transactional(readOnly = true)
	public void validateBranchManager(UUID branchId, UUID restaurantId) {
		if (UserContext.getUserId() == null)
			throw new BusinessException("You do not manage this specific restaurant or branch");

		UUID currentUserId = UUID.fromString(UserContext.getUserId());

		if (UserContext.isRoleAdmin()) return;

		boolean isAuthorized = false;

		if (UserContext.isRoleOwner()) {
			isAuthorized = restaurantRepository.existsByRestaurantIdAndOwnerUserIdAndIsDeletedFalse(
					restaurantId, currentUserId);
		} else if (UserContext.isRoleManager()) {
			isAuthorized = branchRepository.existsByBranchIdAndRestaurantIdAndManagerUserIdAndIsDeletedFalse(
					branchId, restaurantId, currentUserId);
		}

		if (!isAuthorized) {
			throw new BusinessException("You do not manage this specific restaurant or branch");
		}
	}

}