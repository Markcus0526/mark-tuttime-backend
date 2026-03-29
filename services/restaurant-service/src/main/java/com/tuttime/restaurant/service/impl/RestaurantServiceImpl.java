package com.tuttime.restaurant.service.impl;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final BranchRepository branchRepository;
    private final RestaurantMapper restaurantMapper;
    private final BranchMapper branchMapper;


    @Override
    public Page<RestaurantResponse> findRestaurantAll(UUID ownerUserId, Double minRating, Double maxRating, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Restaurant> restaurantPage;

        if (ownerUserId != null && minRating != null && maxRating != null) {
            restaurantPage = restaurantRepository.findByOwnerUserIdAndRatingBetween(ownerUserId, minRating, maxRating, pageable);
        } else if (ownerUserId != null) {
            restaurantPage = restaurantRepository.findByOwnerUserId(ownerUserId, pageable);
        } else if (minRating != null && maxRating != null) {
            restaurantPage = restaurantRepository.findByRatingBetween(minRating, maxRating, pageable);
        } else {
            restaurantPage = restaurantRepository.findAll(pageable);
        }

        return restaurantPage.map(restaurantMapper::toResponse);
    }

    @Override
    public RestaurantResponse getRestaurantById(UUID restaurantId) {
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
        Restaurant savedRestaurant = restaurantRepository.save(restaurant);

        // Create default branch linked to restaurant
        Branch defaultBranch = new Branch();
        defaultBranch.setRestaurantId(savedRestaurant.getRestaurantId());
        defaultBranch.setAddress(savedRestaurant.getAddress());
        defaultBranch.setTimezone("UTC"); // Default timezone; adjust as needed
        defaultBranch.setPhone(null);
        branchRepository.save(defaultBranch);

        // Return mapped response DTO
        return restaurantMapper.toResponse(savedRestaurant);
    }

    @Override
    @Transactional
    public RestaurantResponse updateRestaurant(UUID restaurantId, RestaurantRequest request) {
        Restaurant existing = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found: " + restaurantId));

        restaurantMapper.updateEntityFromRequest(request, existing);

        Restaurant updated = restaurantRepository.save(existing);
        return restaurantMapper.toResponse(updated);
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