package com.tuttime.restaurant.service.impl;

import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.restaurant.dto.BranchDto;
import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.entity.Restaurant;
import com.tuttime.restaurant.mapper.BranchMapper;
import com.tuttime.restaurant.repository.BranchRepository;
import com.tuttime.restaurant.repository.RestaurantRepository;
import com.tuttime.restaurant.repository.StaffRepository;
import com.tuttime.restaurant.service.*;
import jakarta.persistence.criteria.Predicate;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class BranchServiceImpl implements BranchService {

	private final StaffService staffService;
	private final BranchRepository branchRepository;
	private final RestaurantRepository restaurantRepository;
	private final BranchMapper branchMapper;
	private final ZoneService zoneService;
	private final MediaService mediaService;
	private final ScheduleService scheduleService;

	@Override
	@Transactional
	public BranchDto.BranchResponse createBranch(BranchDto.BranchRequest request, UUID currentUserId, String userRoles) {
		Restaurant restaurant = restaurantRepository.findByIdAndDeletedFalse(request.restaurantId())
				.orElseThrow(() -> new InvalidRequestException("Restaurant not found: " + request.restaurantId()));

		staffService.validatePermissions(restaurant.getId(), currentUserId, userRoles);

		Branch entity = branchMapper.toEntity(request);
		entity.setId(UUID.randomUUID());
		entity.setRestaurant(restaurant);
		entity = branchRepository.save(entity);

		return branchMapper.toResponse(entity);
	}

	@Override
	@Transactional(readOnly = true)
	public BranchDto.BranchDetailedResponse getBranchById(UUID branchId) {
		Branch entity = branchRepository.findByIdAndDeletedFalse(branchId)
				.orElseThrow(() -> new InvalidRequestException("Branch not found: " + branchId));
		return branchMapper.toDetailedResponse(entity);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<BranchDto.BranchResponse> findAllBranches(UUID restaurantId, String query, Double minRating, Double maxRating, Pageable pageable) {
		Specification<Branch> spec = createSpecification(restaurantId, query, minRating, maxRating);

		return branchRepository.findAll(spec, pageable)
				.map(branchMapper::toResponse);
	}

	@Override
	@Transactional
	public BranchDto.BranchResponse updateBranch(UUID branchId, BranchDto.BranchRequest request, UUID currentUserId, String userRoles) {
		Branch branch = branchRepository.findByIdAndDeletedFalse(branchId)
				.orElseThrow(() -> new InvalidRequestException("Branch not found: " + branchId));

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
				.orElseThrow(() -> new InvalidRequestException("Branch not found: " + branchId));

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

	private Specification<Branch> createSpecification(UUID restaurantId, String query, Double minRating, Double maxRating) {
		return (root, criteriaQuery, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (restaurantId != null) {
				predicates.add(cb.equal(root.get("restaurant").get("id"), restaurantId));
			}

			// 2. Keyword search (Name or Address)
			if (query != null && !query.isBlank()) {
				String pattern = "%" + query.toLowerCase() + "%";
				predicates.add(cb.or(
						cb.like(cb.lower(root.get("name")), pattern),
						cb.like(cb.lower(root.get("address")), pattern)
				));
			}

			// 3. Rating range
			if (minRating != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), minRating));
			}
			if (maxRating != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("rating"), maxRating));
			}

			// 4. Soft-delete filter (Always exclude deleted by default)
			predicates.add(cb.equal(root.get("deleted"), false));

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}
}