package com.tuttime.restaurant.service.impl;

import com.tuttime.common.auth.enums.UserRoleKind;
import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.common.proto.UserServiceGrpc;
import com.tuttime.restaurant.dto.RestaurantRequest;
import com.tuttime.restaurant.dto.RestaurantResponse;
import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.entity.Restaurant;
import com.tuttime.restaurant.entity.enums.EntityType;
import com.tuttime.restaurant.mapper.RestaurantMapper;
import com.tuttime.restaurant.repository.BranchRepository;
import com.tuttime.restaurant.repository.RestaurantRepository;
import com.tuttime.restaurant.service.*;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.tuttime.restaurant.util.GeometryUtil.createPoint;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RestaurantServiceImpl implements RestaurantService {

	private final RestaurantStaffService restaurantStaffService;
	private final RestaurantRepository restaurantRepository;
	private final BranchRepository branchRepository;
	private final RestaurantMapper restaurantMapper;
	private final BranchService branchService;
	private final MediaService mediaService;
	private final ScheduleService scheduleService;

	@GrpcClient("user-service")
	private UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub;

	@Override
	@Transactional(readOnly = true)
	public Page<RestaurantResponse> findRestaurants(String query, UUID ownerUserId, Double minRating, Double maxRating, Pageable pageable) {
		Specification<Restaurant> spec = createSpecification(query, ownerUserId, minRating, maxRating);
		return restaurantRepository.findAll(spec, pageable)
				.map(restaurantMapper::toResponse);
	}

	@Override
	public RestaurantResponse getRestaurantById(UUID restaurantId) {
		Restaurant restaurant = restaurantRepository.findByRestaurantIdAndDeletedFalse(restaurantId)
				.orElseThrow(() -> new IllegalArgumentException("Restaurant not found: " + restaurantId));
		return restaurantMapper.toResponse(restaurant);
	}

	@Override
	@Transactional
	public RestaurantResponse createRestaurant(RestaurantRequest request, UUID currentUserId, String userRoles) {
		boolean isAdmin = userRoles.contains(UserRoleKind.ADMIN.name());
		if (!isAdmin) {
			throw new AccessDeniedException("You do not have permission to manage this restaurant.");
		}

		// Map request DTO to entity
		Restaurant restaurant = restaurantMapper.toEntity(request);

		// Persist restaurant
		restaurant.setLocation(createPoint(request.getLongitude(), request.getLatitude()));

		Restaurant savedRestaurant = restaurantRepository.save(restaurant);

		// Create default branch linked to restaurant
		Branch defaultBranch = new Branch();
		defaultBranch.setRestaurantId(savedRestaurant.getRestaurantId());
		defaultBranch.setAddress(savedRestaurant.getAddress());
		defaultBranch.setTimezone("UTC"); // Default timezone; adjust as needed
		defaultBranch.setPhone(null);
		defaultBranch.setLocation(createPoint(request.getLongitude(), request.getLatitude()));
		branchRepository.save(defaultBranch);

		// Return mapped response DTO
		return restaurantMapper.toResponse(savedRestaurant);
	}

	@Override
	@Transactional
	public RestaurantResponse updateRestaurant(UUID restaurantId, RestaurantRequest request, UUID currentUserId, String userRoles) {
		Restaurant restaurant = restaurantRepository.findByRestaurantIdAndDeletedFalse(restaurantId)
				.orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + restaurantId));

		boolean isAdmin = userRoles.contains(UserRoleKind.ADMIN.name());
		boolean isOwner = false;
		if (!isAdmin) {
			isOwner = restaurantStaffService.validateRestaurantOwner(restaurant.getRestaurantId(), currentUserId);
		}
		if (!isAdmin && !isOwner) {
			throw new AccessDeniedException("You do not have permission to manage this restaurant.");
		}

		// Map fields (Standard fields like name, address, phone)
		restaurantMapper.updateEntityFromRequest(request, restaurant);

		// Manual mapping for the PostGIS Point
		if (request.getLongitude() != null && request.getLatitude() != null) {
			restaurant.setLocation(createPoint(request.getLongitude(), request.getLatitude()));
		}

		// No need for .save(restaurant) - Hibernate dirty checking handles it
		return restaurantMapper.toResponse(restaurant);
	}

	@Override
	@Transactional
	public void deleteRestaurant(UUID restaurantId, UUID currentUserId, String userRoles) {
		// 1. Fetch and verify ownership
		Restaurant restaurant = restaurantRepository.findByRestaurantIdAndDeletedFalse(restaurantId)
				.orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + restaurantId));

		boolean isAdmin = userRoles.contains(UserRoleKind.ADMIN.name());
		boolean isOwner = false;
		if (!isAdmin) {
			isOwner = restaurantStaffService.validateRestaurantOwner(restaurant.getRestaurantId(), currentUserId);
		}
		if (!isAdmin && !isOwner) {
			throw new AccessDeniedException("You do not have permission to manage this restaurant.");
		}

		// 3. Soft delete related entities (Branches, Staff, Schedules)
		// Hibernate will handle the updates if you have properly mapped @OneToMany(cascade = CascadeType.ALL)
		branchService.deleteAllByRestaurantId(restaurant.getRestaurantId());
		mediaService.deleteAllByEntityId(EntityType.RESTAURANT, restaurant.getRestaurantId());
		scheduleService.deleteAllByRestaurantId(restaurant.getRestaurantId());

		// 2. Soft delete the Restaurant
		restaurant.setDeleted(true);

		restaurantRepository.save(restaurant);
	}

	public Page<RestaurantResponse> findRestaurantsNearLocation(
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