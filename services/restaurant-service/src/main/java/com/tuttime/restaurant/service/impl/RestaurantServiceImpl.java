package com.tuttime.restaurant.service.impl;

import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.exception.BusinessException;
import com.tuttime.common.proto.UserByIdRequest;
import com.tuttime.common.proto.UserResponse;
import com.tuttime.common.proto.UserServiceGrpc;
import com.tuttime.restaurant.dto.RestaurantRequest;
import com.tuttime.restaurant.dto.RestaurantResponse;
import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.entity.Restaurant;
import com.tuttime.restaurant.mapper.BranchMapper;
import com.tuttime.restaurant.mapper.RestaurantMapper;
import com.tuttime.restaurant.repository.BranchRepository;
import com.tuttime.restaurant.repository.RestaurantRepository;
import com.tuttime.restaurant.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static com.tuttime.restaurant.util.GeometryUtil.createPoint;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RestaurantServiceImpl implements RestaurantService {

	private final RestaurantRepository restaurantRepository;
	private final BranchRepository branchRepository;
	private final RestaurantMapper restaurantMapper;
	private final BranchMapper branchMapper;

	@GrpcClient("user-service")
	private UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub;

	@Override
	public Page<RestaurantResponse> findRestaurantAll(UUID ownerUserId, Double minRating, Double maxRating, int page, int size) {
		Pageable pageable = PageRequest.of(page, size);

		Page<Restaurant> restaurantPage;

		if (ownerUserId != null && minRating != null && maxRating != null) {
			restaurantPage = restaurantRepository.findByOwnerUserIdAndRatingBetween(ownerUserId, BigDecimal.valueOf(minRating), BigDecimal.valueOf(maxRating), pageable);
		} else if (ownerUserId != null) {
			restaurantPage = restaurantRepository.findByOwnerUserId(ownerUserId, pageable);
		} else if (minRating != null && maxRating != null) {
			restaurantPage = restaurantRepository.findByRatingBetween(BigDecimal.valueOf(minRating), BigDecimal.valueOf(maxRating), pageable);
		} else {
			restaurantPage = restaurantRepository.findAll(pageable);
		}

		return restaurantPage.map(restaurantMapper::toResponse);
	}

	@Override
	public RestaurantResponse getRestaurantById(UUID restaurantId) {
		UserByIdRequest request = UserByIdRequest.newBuilder()
				.setUserId(UserContext.getUserId())
				.build();
		// Make the gRPC network call
		UserResponse response = userServiceBlockingStub.getUserById(request);

		Restaurant restaurant = restaurantRepository.findByRestaurantIdAndIsDeletedFalse(restaurantId)
				.orElseThrow(() -> new IllegalArgumentException("Restaurant not found: " + restaurantId));
		return restaurantMapper.toResponse(restaurant);
	}

	@Override
	@Transactional
	public RestaurantResponse createRestaurant(RestaurantRequest request) {
		// Map request DTO to entity
		Restaurant restaurant = restaurantMapper.toEntity(request);

		// Persist restaurant
		restaurant.setLocation(createPoint(request.getLongitude(), request.getLatitude()));

		Restaurant savedRestaurant = restaurantRepository.save(restaurant);

		// Create default branch linked to restaurant
		Branch defaultBranch = new Branch();
		defaultBranch.setRestaurantId(savedRestaurant.getRestaurantId());
		defaultBranch.setManagerUserId(savedRestaurant.getOwnerUserId());
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
	public RestaurantResponse updateRestaurant(UUID restaurantId, RestaurantRequest request) {
		Restaurant existing = restaurantRepository.findById(restaurantId)
				.orElseThrow(() -> new IllegalArgumentException("Restaurant not found: " + restaurantId));

		// Map fields (Standard fields like name, address, phone)
		restaurantMapper.updateEntityFromRequest(request, existing);

		// Manual mapping for the PostGIS Point
		if (request.getLongitude() != null && request.getLatitude() != null) {
			existing.setLocation(createPoint(request.getLongitude(), request.getLatitude()));
		}

		// No need for .save(existing) - Hibernate dirty checking handles it
		return restaurantMapper.toResponse(existing);
	}

	@Override
	@Transactional
	public void deleteRestaurant(UUID restaurantId) {
		if (!restaurantRepository.existsById(restaurantId)) {
			throw new IllegalArgumentException("Restaurant not found: " + restaurantId);
		}
		restaurantRepository.deleteById(restaurantId);
	}

	public Page<RestaurantResponse> findRestaurantsNearLocation(
			double latitude,
			double longitude,
			double radiusMeters,
			int page,
			int size) {
		Pageable pageable = PageRequest.of(page, size);
		Page<Restaurant> entities = restaurantRepository.findByLocationNear(latitude, longitude, radiusMeters, pageable);
		return entities.map(this::mapToResponse);
	}

	@Transactional(readOnly = true)
	public void validateRestaurantOwner(UUID restaurantId) {
		if (UserContext.getUserId() == null)
			throw new BusinessException("You do not manage this specific restaurant or branch");

		UUID currentUserId = UUID.fromString(UserContext.getUserId());

		if (UserContext.isRoleAdmin()) return;

		boolean isOwner = restaurantRepository.existsByRestaurantIdAndOwnerUserIdAndIsDeletedFalse(restaurantId, currentUserId);

		if (!isOwner) {
			throw new BusinessException("You do not manage this specific restaurant");
		}
	}

	private RestaurantResponse mapToResponse(Restaurant entity) {
		// map entity fields to DTO fields
		RestaurantResponse response = new RestaurantResponse();
		response.setRestaurantId(entity.getRestaurantId());
		response.setName(entity.getName());
		response.setDescription(entity.getDescription());
		response.setAddress(entity.getAddress());
		response.setPhone(entity.getPhone());
		response.setRating(entity.getRating());
		// etc.
		return response;
	}

}