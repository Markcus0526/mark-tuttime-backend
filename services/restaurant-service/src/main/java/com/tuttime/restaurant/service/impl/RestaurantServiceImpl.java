package com.tuttime.restaurant.service.impl;

import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.restaurant.dto.RestaurantDto;
import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.entity.Restaurant;
import com.tuttime.restaurant.mapper.RestaurantMapper;
import com.tuttime.restaurant.repository.BranchRepository;
import com.tuttime.restaurant.repository.RestaurantRepository;
import com.tuttime.restaurant.service.*;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.tuttime.restaurant.util.GeometryUtil.createPoint;

@Slf4j
@Service
@RequiredArgsConstructor
public class RestaurantServiceImpl implements RestaurantService {

	private final StaffService staffService;
	private final RestaurantRepository restaurantRepository;
	private final BranchRepository branchRepository;
	private final RestaurantMapper restaurantMapper;
	private final BranchService branchService;
	private final MediaService mediaService;
	private final ScheduleService scheduleService;

	@Override
	@Transactional(readOnly = true)
	public Page<RestaurantDto.RestaurantResponse> findRestaurants(String query, UUID ownerUserId, Double minRating, Double maxRating, Pageable pageable) {
		Specification<Restaurant> spec = createSpecification(query, ownerUserId, minRating, maxRating);
		return restaurantRepository.findAll(spec, pageable)
				.map(restaurantMapper::toResponse);
	}

	@Override
	@Transactional(readOnly = true)
	public RestaurantDto.RestaurantDetailResponse getRestaurantById(UUID restaurantId) {
		Restaurant restaurant = restaurantRepository.findByIdAndDeletedFalse(restaurantId)
				.orElseThrow(() -> new InvalidRequestException("Restaurant not found: " + restaurantId));
		return restaurantMapper.toDetailResponse(restaurant);
	}

	@Override
	@Transactional
	public RestaurantDto.RestaurantResponse createRestaurant(RestaurantDto.RestaurantRequest request, UUID currentUserId, String userRoles) {
		staffService.validateAdminAccess(currentUserId);

		// Map request DTO to entity
		Restaurant restaurant = restaurantMapper.toEntity(request);

		// Persist restaurant
		restaurant.setLocation(createPoint(request.longitude(), request.latitude()));

		Restaurant savedRestaurant = restaurantRepository.save(restaurant);

		// Create default branch linked to restaurant
		Branch defaultBranch = new Branch();
		defaultBranch.setRestaurant(savedRestaurant);
		defaultBranch.setName(savedRestaurant.getName());
		defaultBranch.setDescription(savedRestaurant.getDescription());
		defaultBranch.setAddress(savedRestaurant.getAddress());
		defaultBranch.setTimezone("UTC"); // Default timezone; adjust as needed
		defaultBranch.setPhone(savedRestaurant.getPhone());
		defaultBranch.setRating(savedRestaurant.getRating());
		defaultBranch.setLocation(createPoint(request.longitude(), request.latitude()));
		branchRepository.save(defaultBranch);

		// Return mapped response DTO
		return restaurantMapper.toResponse(savedRestaurant);
	}

	@Override
	@Transactional
	public RestaurantDto.RestaurantResponse updateRestaurant(UUID restaurantId, RestaurantDto.RestaurantRequest request, UUID currentUserId, String userRoles) {
		Restaurant restaurant = restaurantRepository.findByIdAndDeletedFalse(restaurantId)
				.orElseThrow(() -> new InvalidRequestException("Restaurant not found: " + restaurantId));

		// 1. Unified Permission Check
		staffService.validatePermissions(restaurant.getId(), currentUserId, userRoles);

		// 2. Map fields and handle PostGIS Point
		restaurantMapper.updateEntityFromRequest(request, restaurant);

		// Hibernate dirty checking updates the DB automatically
		return restaurantMapper.toResponse(restaurant);
	}

	@Override
	@Transactional
	public void deleteRestaurant(UUID restaurantId, UUID currentUserId, String userRoles) {
		Restaurant restaurant = restaurantRepository.findByIdAndDeletedFalse(restaurantId)
				.orElseThrow(() -> new InvalidRequestException("Restaurant not found: " + restaurantId));

		// 1. Unified Permission Check
		staffService.validatePermissions(restaurant.getId(), currentUserId, userRoles);

		// 2. Soft delete related entities
		// Consider moving these to a single Batch Update query for better performance
		branchService.deleteAllByRestaurantId(restaurantId);
		mediaService.deleteAllByEntityId(restaurantId);
		scheduleService.deleteAllByEntityId(restaurantId);

		// 3. Soft delete the Restaurant
		restaurant.setDeleted(true);
		restaurantRepository.save(restaurant);
	}

	@Transactional(readOnly = true)
	public Page<RestaurantDto.RestaurantResponse> findRestaurantsNearLocation(
			double latitude,
			double longitude,
			double radiusMeters,
			int page,
			int size) {
		Pageable pageable = PageRequest.of(page, size);
		Page<Restaurant> entities = restaurantRepository.findPagesByLocationNear(latitude, longitude, radiusMeters, pageable);
		return entities.map(restaurantMapper::toResponse);
	}

	private Specification<Restaurant> createSpecification(String query, UUID ownerId, Double minRating, Double maxRating) {
		return (root, criteriaQuery, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			// 1. Keyword search (Name or Address)
			if (query != null && !query.isBlank()) {
				String pattern = "%" + query.toLowerCase() + "%";
				predicates.add(cb.or(
						cb.like(cb.lower(root.get("name")), pattern),
						cb.like(cb.lower(root.get("address")), pattern)
				));
			}

			// 2. Owner filter TODO
//			if (ownerId != null) {
//				predicates.add(cb.equal(root.get("ownerUserId"), ownerId));
//			}

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