package com.tuttime.user.service.impl;

import com.tuttime.common.exception.BusinessException;
import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.user.dto.*;
import com.tuttime.user.entity.LocationPrivacyLevel;
import com.tuttime.user.entity.User;
// Note: Mining and Token service clients can be added later for enhanced map markers
// import com.tuttime.user.grpc.client.MiningServiceGrpcClient;
// import com.tuttime.user.grpc.client.TokenServiceGrpcClient;
import com.tuttime.user.repository.UserRepository;
import com.tuttime.user.service.ImageUrlCacheService;
import com.tuttime.user.service.MapQueryCountService;
import com.tuttime.user.service.UserLocationService;
import com.tuttime.user.util.LocationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

/**
 * User Location Service Implementation
 * 
 * <p>Handles user location management and geospatial queries for map feature.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserLocationServiceImpl implements UserLocationService {

    private final UserRepository userRepository;
    private final MapQueryCountService mapQueryCountService;
    private final ImageUrlCacheService imageUrlCacheService;
    // Optional: Add gRPC clients for mining/token status in map markers
    // private final MiningServiceGrpcClient miningServiceGrpcClient;
    // private final TokenServiceGrpcClient tokenServiceGrpcClient;

    @Value("${map.query.use-approximate-count:false}")
    private boolean useApproximateCount;

    private static final int MAX_RESULTS = 500;
    private static final int DEFAULT_RESULTS = 100;
    private static final int MAX_RADIUS_METERS = 50000; // 50km max radius

    @Override
    @Transactional
    @CacheEvict(value = {"users", "map-queries"}, allEntries = true)
    public UserLocationResponse updateLocation(Long userId, UpdateLocationRequest request) {
        log.info("Updating location for user: {}", userId);

        // Validate coordinates
        LocationUtils.validateLatitude(request.getLatitude());
        LocationUtils.validateLongitude(request.getLongitude());
        LocationUtils.validateAccuracy(request.getAccuracy());

        // Get user
        User user = userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Check location privacy level - reject updates if PRIVATE or STATIC
        if (user.getLocationPrivacyLevel() == LocationPrivacyLevel.PRIVATE) {
            throw new BusinessException("Location updates are not allowed when privacy level is 'private'. " +
                "Please update your privacy settings first.");
        }
        
        if (user.getLocationPrivacyLevel() == LocationPrivacyLevel.STATIC) {
            throw new BusinessException("Location updates via API are not allowed when privacy level is 'static'. " +
                "Please update your location on the profile page.");
        }
        
        // Only allow updates when privacy level is EXACT (auto-detect mode)
        if (user.getLocationPrivacyLevel() != LocationPrivacyLevel.EXACT) {
            throw new BusinessException("Location updates are only allowed when privacy level is 'exact' (auto-detect mode).");
        }

        // Update location fields
        user.setLatitude(request.getLatitude());
        user.setLongitude(request.getLongitude());
        user.setLocationAccuracy(request.getAccuracy());
        user.setLocationUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        // Update privacy level if provided (only allow EXACT for auto-detect mode)
        if (request.getPrivacyLevel() != null) {
            try {
                LocationPrivacyLevel privacyLevel = LocationPrivacyLevel.valueOf(
                    request.getPrivacyLevel().toUpperCase());
                
                // Only allow setting to EXACT via location update API (auto-detect mode)
                if (privacyLevel != LocationPrivacyLevel.EXACT) {
                    throw new BusinessException("Privacy level can only be set to 'exact' via location update API. " +
                        "Use the privacy update endpoint to change to 'private' or 'static'.");
                }
                
                user.setLocationPrivacyLevel(privacyLevel);
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid privacy level: " + request.getPrivacyLevel() + 
                    ". Valid values are: exact");
            }
        }

        // Create PostGIS point
        user.setLocationPoint(LocationUtils.createPoint(
            request.getLatitude(), 
            request.getLongitude()
        ));

        // Enable location sharing if coordinates are provided
        if (!user.getLocationSharingEnabled()) {
            user.setLocationSharingEnabled(true);
        }

        // Save user
        user = userRepository.save(user);

        log.info("Location updated for user: {} at ({}, {}), map cache invalidated", 
            userId, request.getLatitude(), request.getLongitude());

        // Invalidate COUNT cache as well
        mapQueryCountService.invalidateCountCache();

        return buildLocationResponse(user, true);
    }

    @Override
    @Transactional(readOnly = true)
    public UserLocationResponse getMyLocation(Long userId) {
        log.debug("Getting location for user: {}", userId);

        User user = userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        return buildLocationResponse(user, true);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"users", "map-queries"}, allEntries = true)
    public UserLocationResponse updateLocationPrivacy(Long userId, LocationPrivacyRequest request) {
        log.info("Updating location privacy for user: {}", userId);

        User user = userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Validate privacy level
        LocationPrivacyLevel privacyLevel;
        try {
            privacyLevel = LocationPrivacyLevel.valueOf(request.getPrivacyLevel().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid privacy level: " + request.getPrivacyLevel() + 
                ". Valid values are: exact, private, static");
        }

        // Handle STATIC mode: require latitude and longitude
        if (privacyLevel == LocationPrivacyLevel.STATIC) {
            if (request.getLatitude() == null || request.getLongitude() == null) {
                throw new BusinessException("Latitude and longitude are required when privacy level is 'static'");
            }
            
            // Validate coordinates
            LocationUtils.validateLatitude(request.getLatitude());
            LocationUtils.validateLongitude(request.getLongitude());
            
            // Set location for static mode
            user.setLatitude(request.getLatitude());
            user.setLongitude(request.getLongitude());
            user.setLocationPoint(LocationUtils.createPoint(
                request.getLatitude(), 
                request.getLongitude()
            ));
            user.setLocationUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
            user.setLocationSharingEnabled(true); // Static mode requires sharing enabled
            user.setLocationPrivacyLevel(privacyLevel);
            
            log.info("Location set to static mode for user: {} at ({}, {})", 
                userId, request.getLatitude(), request.getLongitude());
        } else if (privacyLevel == LocationPrivacyLevel.PRIVATE) {
            // PRIVATE mode: clear location data and disable sharing
            user.setLocationSharingEnabled(false);
            user.setLocationPrivacyLevel(privacyLevel);
            user.setLatitude(null);
            user.setLongitude(null);
            user.setLocationPoint(null);
            user.setLocationAccuracy(null);
            
            log.info("Location set to private mode for user: {}", userId);
        } else if (privacyLevel == LocationPrivacyLevel.EXACT) {
            // EXACT mode: enable sharing (auto-detect mode)
            user.setLocationSharingEnabled(request.getSharingEnabled());
            user.setLocationPrivacyLevel(privacyLevel);
            // Keep existing location if it exists, or wait for API update
            
            log.info("Location set to exact (auto-detect) mode for user: {}, sharingEnabled: {}", 
                userId, request.getSharingEnabled());
        }

        user = userRepository.save(user);

        log.info("Location privacy updated for user: {}, sharingEnabled: {}, privacyLevel: {}, map cache invalidated", 
            userId, user.getLocationSharingEnabled(), privacyLevel);

        // Invalidate COUNT cache as well
        mapQueryCountService.invalidateCountCache();

        return buildLocationResponse(user, true);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
        value = "map-queries",
        key = "'bounds:' + T(java.lang.String).format('%.6f,%.6f,%.6f,%.6f', #minLat, #minLng, #maxLat, #maxLng) + ':limit:' + #limit + ':offset:' + #offset",
        unless = "#result == null || #result.total == 0"
    )
    public MapUsersResponse getUsersInBounds(
            double minLat, double minLng, double maxLat, double maxLng, 
            int limit, int offset) {
        return getUsersInBounds(minLat, minLng, maxLat, maxLng, limit, offset, null);
    }
    
    public MapUsersResponse getUsersInBounds(
            double minLat, double minLng, double maxLat, double maxLng, 
            int limit, int offset, Long requestingUserId) {
        
        log.debug("Getting users in bounds: ({}, {}) to ({}, {}) [cache miss]", minLat, minLng, maxLat, maxLng);

        // Validate bounds
        validateBounds(minLat, minLng, maxLat, maxLng);

        // Limit results
        int pageSize = Math.min(limit > 0 ? limit : DEFAULT_RESULTS, MAX_RESULTS);
        Pageable pageable = PageRequest.of(offset / pageSize, pageSize);

        // Query users (without COUNT to avoid slow COUNT query)
        // We'll use optimized COUNT service instead
        Page<User> usersPage = userRepository.findUsersInBounds(
            minLat, minLng, maxLat, maxLng, 
            LocationPrivacyLevel.PRIVATE, 
            pageable
        );

        // Convert to map response
        List<MapUserResponse> mapUsers = usersPage.getContent().stream()
            .map(this::buildMapUserResponse)
            .collect(Collectors.toList());

        // Get optimized count (cached or approximate)
        long totalCount = mapQueryCountService.getUsersInBoundsCount(
            minLat, minLng, maxLat, maxLng,
            LocationPrivacyLevel.PRIVATE.name().toLowerCase(),
            useApproximateCount
        );

        return MapUsersResponse.builder()
            .users(mapUsers)
            .total(totalCount > 0 ? totalCount : usersPage.getTotalElements()) // Use optimized count, fallback to exact
            .limit(pageSize)
            .offset(offset)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
        value = "map-queries",
        key = "'nearby:' + T(java.lang.String).format('%.6f,%.6f', #lat, #lng) + ':radius:' + #radiusMeters + ':limit:' + #limit + ':offset:' + #offset",
        unless = "#result == null || #result.total == 0"
    )
    public MapUsersResponse getUsersNearby(
            double lat, double lng, int radiusMeters, 
            int limit, int offset) {
        return getUsersNearby(lat, lng, radiusMeters, limit, offset, null);
    }
    
    public MapUsersResponse getUsersNearby(
            double lat, double lng, int radiusMeters, 
            int limit, int offset, Long requestingUserId) {
        
        log.debug("Getting users nearby: ({}, {}) within {} meters [cache miss]", lat, lng, radiusMeters);

        // Validate coordinates
        LocationUtils.validateLatitude(BigDecimal.valueOf(lat));
        LocationUtils.validateLongitude(BigDecimal.valueOf(lng));

        // Validate radius
        if (radiusMeters < 1 || radiusMeters > MAX_RADIUS_METERS) {
            throw new BusinessException(
                String.format("Radius must be between 1 and %d meters", MAX_RADIUS_METERS));
        }

        // Limit results
        int pageSize = Math.min(limit > 0 ? limit : DEFAULT_RESULTS, MAX_RESULTS);
        Pageable pageable = PageRequest.of(offset / pageSize, pageSize);

        // Query users (without COUNT to avoid slow COUNT query)
        Page<User> usersPage = userRepository.findUsersNearby(
            lat, lng, radiusMeters,
            LocationPrivacyLevel.PRIVATE,
            pageable
        );

        // Convert to map response
        List<MapUserResponse> mapUsers = usersPage.getContent().stream()
            .map(this::buildMapUserResponse)
            .collect(Collectors.toList());

        // Get optimized count (cached or approximate)
        long totalCount = mapQueryCountService.getUsersNearbyCount(
            lat, lng, radiusMeters,
            LocationPrivacyLevel.PRIVATE.name().toLowerCase(),
            useApproximateCount
        );

        return MapUsersResponse.builder()
            .users(mapUsers)
            .total(totalCount > 0 ? totalCount : usersPage.getTotalElements()) // Use optimized count, fallback to exact
            .limit(pageSize)
            .offset(offset)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
        value = "map-queries",
        key = "'location:' + #country.toLowerCase() + ':' + (#city != null ? #city.toLowerCase() : 'all') + ':limit:' + #limit + ':offset:' + #offset",
        unless = "#result == null || #result.total == 0"
    )
    public MapUsersResponse getUsersByLocation(String country, String city, int limit, int offset) {
        return getUsersByLocation(country, city, limit, offset, null);
    }
    
    public MapUsersResponse getUsersByLocation(String country, String city, int limit, int offset, Long requestingUserId) {
        log.debug("Getting users by location: country={}, city={} [cache miss]", country, city);

        if (country == null || country.trim().isEmpty()) {
            throw new BusinessException("Country is required");
        }

        // Limit results
        int pageSize = Math.min(limit > 0 ? limit : DEFAULT_RESULTS, MAX_RESULTS);
        Pageable pageable = PageRequest.of(offset / pageSize, pageSize);

        // Query users (without COUNT to avoid slow COUNT query)
        Page<User> usersPage;
        if (city != null && !city.trim().isEmpty()) {
            usersPage = userRepository.findByCountryAndCityAndLocationSharingEnabled(
                country.trim(), city.trim(), LocationPrivacyLevel.PRIVATE, pageable);
        } else {
            usersPage = userRepository.findByCountryAndLocationSharingEnabled(
                country.trim(), LocationPrivacyLevel.PRIVATE, pageable);
        }

        // Convert to map response
        List<MapUserResponse> mapUsers = usersPage.getContent().stream()
            .map(this::buildMapUserResponse)
            .collect(Collectors.toList());

        // Get optimized count (cached or approximate)
        long totalCount = mapQueryCountService.getUsersByLocationCount(
            country.trim(),
            city != null ? city.trim() : null,
            LocationPrivacyLevel.PRIVATE.name().toLowerCase(),
            useApproximateCount
        );

        return MapUsersResponse.builder()
            .users(mapUsers)
            .total(totalCount > 0 ? totalCount : usersPage.getTotalElements()) // Use optimized count, fallback to exact
            .limit(pageSize)
            .offset(offset)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserLocationResponse getUserLocation(Long requestingUserId, Long targetUserId) 
            throws BusinessException {
        
        log.debug("Getting location for user {} requested by {}", targetUserId, requestingUserId);

        User targetUser = userRepository.findByIdAndNotDeleted(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + targetUserId));

        // Check if location sharing is enabled
        if (!targetUser.getLocationSharingEnabled()) {
            throw new BusinessException("User location is private");
        }

        // Check privacy level
        if (targetUser.getLocationPrivacyLevel() == LocationPrivacyLevel.PRIVATE) {
            throw new BusinessException("User location is private");
        }

        // Build response (coordinates will be blurred based on privacy level)
        return buildLocationResponse(targetUser, false);
    }

    /**
     * Builds location response from user entity.
     * 
     * @param user User entity
     * @param isOwnLocation Whether this is the user's own location (exact coordinates)
     * @return Location response
     */
    private UserLocationResponse buildLocationResponse(User user, boolean isOwnLocation) {
        BigDecimal latitude = user.getLatitude();
        BigDecimal longitude = user.getLongitude();

        // Apply privacy blurring if not own location
        if (!isOwnLocation && latitude != null && longitude != null) {
            LocationUtils.BlurredCoordinates blurred = LocationUtils.blurCoordinates(
                latitude, longitude, user.getLocationPrivacyLevel());
            
            if (blurred != null) {
                latitude = blurred.getLatitude();
                longitude = blurred.getLongitude();
            } else {
                // Private - return null coordinates
                latitude = null;
                longitude = null;
            }
        }

        return UserLocationResponse.builder()
            .latitude(latitude)
            .longitude(longitude)
            .city(user.getCity())
            .country(user.getCountry())
            .privacyLevel(user.getLocationPrivacyLevel().name().toLowerCase())
            .sharingEnabled(user.getLocationSharingEnabled())
            .accuracy(user.getLocationAccuracy())
            .updatedAt(user.getLocationUpdatedAt())
            .build();
    }

    /**
     * Builds map user response from user entity.
     * 
     * @param user User entity
     * @return Map user response (coordinates blurred based on privacy)
     */
    private MapUserResponse buildMapUserResponse(User user) {
        BigDecimal latitude = user.getLatitude();
        BigDecimal longitude = user.getLongitude();

        // Apply privacy blurring
        if (latitude != null && longitude != null) {
            LocationUtils.BlurredCoordinates blurred = LocationUtils.blurCoordinates(
                latitude, longitude, user.getLocationPrivacyLevel());
            
            if (blurred != null) {
                latitude = blurred.getLatitude();
                longitude = blurred.getLongitude();
            } else {
                // Private - should not appear on map, but handle gracefully
                latitude = null;
                longitude = null;
            }
        }

        // Get mining status (optional - may fail)
        Boolean miningActive = null;
        try {
            // Try to get mining status via gRPC
            // This is optional - if service unavailable, continue without it
            miningActive = false; // Placeholder - implement gRPC call if needed
        } catch (Exception e) {
            log.debug("Could not fetch mining status for user {}: {}", user.getUserId(), e.getMessage());
        }

        // Get token status (optional - may fail)
        String statusLevel = null;
        try {
            // Try to get token status via gRPC
            // This is optional - if service unavailable, continue without it
        } catch (Exception e) {
            log.debug("Could not fetch token status for user {}: {}", user.getUserId(), e.getMessage());
        }

        // Use thumbnail URL for map markers (32x32px, ~1-3KB) - optimal for mobile performance
        // Fall back to original URL if thumbnail doesn't exist (backward compatibility)
        String profileImageThumbnailUrl = user.getProfileImageThumbnailUrl();
        String profileImageUrl = user.getProfileImageUrl();
        
        if (profileImageThumbnailUrl != null) {
            profileImageThumbnailUrl = imageUrlCacheService.getCachedImageUrl(profileImageThumbnailUrl);
        }
        if (profileImageUrl != null) {
            profileImageUrl = imageUrlCacheService.getCachedImageUrl(profileImageUrl);
        }
        
        return MapUserResponse.builder()
            .userId(user.getUserId())
            .username(user.getUsername())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .latitude(latitude)
            .longitude(longitude)
            .city(user.getCity())
            .country(user.getCountry())
            .profileImageThumbnailUrl(profileImageThumbnailUrl != null ? profileImageThumbnailUrl : profileImageUrl)
            .profileImageUrl(profileImageUrl)
            .miningActive(miningActive)
            .statusLevel(statusLevel)
            .privacyLevel(user.getLocationPrivacyLevel().name().toLowerCase())
            .build();
    }

    /**
     * Validates bounding box coordinates.
     * 
     * @param minLat Minimum latitude
     * @param minLng Minimum longitude
     * @param maxLat Maximum latitude
     * @param maxLng Maximum longitude
     * @throws BusinessException if bounds are invalid
     */
    private void validateBounds(double minLat, double minLng, double maxLat, double maxLng) {
        if (minLat >= maxLat) {
            throw new BusinessException("minLat must be less than maxLat");
        }
        if (minLng >= maxLng) {
            throw new BusinessException("minLng must be less than maxLng");
        }
        if (minLat < -90 || maxLat > 90) {
            throw new BusinessException("Latitude must be between -90 and 90");
        }
        if (minLng < -180 || maxLng > 180) {
            throw new BusinessException("Longitude must be between -180 and 180");
        }
    }
}

