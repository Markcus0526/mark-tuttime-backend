package com.tuttime.user.controller;

import com.tuttime.common.dto.ApiResponse;
import com.tuttime.common.exception.BusinessException;
import com.tuttime.common.exception.RateLimitExceededException;
import com.tuttime.user.dto.*;
import com.tuttime.user.service.RateLimitService;
import com.tuttime.user.service.UserLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * User Location Controller
 * 
 * <p>Provides REST endpoints for user location management and map queries.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Location", description = "User location management and map queries")
public class UserLocationController {

    private final UserLocationService userLocationService;
    private final RateLimitService rateLimitService;

    /**
     * Updates the current user's location.
     * 
     * @param userId User ID from authentication
     * @param request Location update request
     * @return Updated location response
     */
    @PatchMapping("/me/location")
    @Operation(
        summary = "Update user location",
        description = "Updates the current authenticated user's location coordinates. Rate limit: 10 updates/hour."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "Location updated successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "Invalid coordinates or update too frequent"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", 
            description = "Unauthorized"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "429", 
            description = "Rate limit exceeded"
        )
    })
    public ResponseEntity<ApiResponse<UserLocationResponse>> updateLocation(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateLocationRequest request,
            HttpServletRequest httpRequest) {
        log.info("Location update request for user: {}", userId);
        
        // Check rate limit
        if (!rateLimitService.isLocationUpdateAllowed(userId)) {
            int limit = rateLimitService.getLocationUpdateLimit();
            throw new RateLimitExceededException(
                String.format("Rate limit exceeded. Maximum %d location updates per hour.", limit), 3600);
        }
        
        UserLocationResponse response = userLocationService.updateLocation(userId, request);
        int remaining = rateLimitService.getRemainingLocationUpdates(userId);
        int limit = rateLimitService.getLocationUpdateLimit();
        return ResponseEntity.ok()
                .header("X-RateLimit-Limit", String.valueOf(limit))
                .header("X-RateLimit-Remaining", String.valueOf(remaining))
                .body(ApiResponse.success("Location updated successfully", response));
    }

    /**
     * Gets the current user's location.
     * 
     * @param userId User ID from authentication
     * @return User location response
     */
    @GetMapping("/me/location")
    @Operation(
        summary = "Get user location",
        description = "Retrieves the current authenticated user's location information."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "Location retrieved successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", 
            description = "Unauthorized"
        )
    })
    public ResponseEntity<ApiResponse<UserLocationResponse>> getMyLocation(
            @AuthenticationPrincipal Long userId) {
        log.debug("Getting location for user: {}", userId);
        
        UserLocationResponse response = userLocationService.getMyLocation(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Updates location privacy settings.
     * 
     * @param userId User ID from authentication
     * @param request Privacy settings request
     * @return Updated privacy settings
     */
    @PatchMapping("/me/location/privacy")
    @Operation(
        summary = "Update location privacy",
        description = "Updates location sharing and privacy level settings."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "Privacy settings updated successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "Invalid privacy level"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", 
            description = "Unauthorized"
        )
    })
    public ResponseEntity<ApiResponse<UserLocationResponse>> updateLocationPrivacy(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody LocationPrivacyRequest request) {
        log.info("Privacy update request for user: {}", userId);
        
        UserLocationResponse response = userLocationService.updateLocationPrivacy(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Privacy settings updated successfully", response));
    }

    /**
     * Gets users on map within bounding box.
     * 
     * @param bounds Bounding box: "minLat,minLng,maxLat,maxLng"
     * @param limit Maximum number of results (default: 100, max: 500)
     * @param offset Pagination offset (default: 0)
     * @return Map users response
     */
    @GetMapping("/map")
    @Operation(
        summary = "Get users on map (bounds)",
        description = "Retrieves users within a bounding box for map display. Supports bounds, center+radius, country/city, or nearby queries. Rate limit: Configurable via RATELIMIT_MAP_QUERY_LIMIT (default: 1000 requests/minute)."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "Users retrieved successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "Invalid bounds or parameters"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "429", 
            description = "Rate limit exceeded"
        )
    })
    public ResponseEntity<ApiResponse<MapUsersResponse>> getUsersOnMap(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Parameter(description = "Bounding box: minLat,minLng,maxLat,maxLng")
            @RequestParam(required = false) String bounds,
            @Parameter(description = "Center point and radius: lat,lng,radiusMeters")
            @RequestParam(required = false) String center,
            @Parameter(description = "Nearby search: lat,lng,radiusMeters")
            @RequestParam(required = false) String nearby,
            @Parameter(description = "Country name")
            @RequestParam(required = false) String country,
            @Parameter(description = "City name")
            @RequestParam(required = false) String city,
            @Parameter(description = "Maximum number of results (default: 100, max: 500)")
            @RequestParam(defaultValue = "100") int limit,
            @Parameter(description = "Pagination offset (default: 0)")
            @RequestParam(defaultValue = "0") int offset,
            HttpServletRequest httpRequest) {
        
        log.debug("Map query: bounds={}, center={}, nearby={}, country={}, city={}", 
            bounds, center, nearby, country, city);

        // Extract IP address for rate limiting
        String ip = extractIpAddress(httpRequest);
        
        // Check rate limit
        if (!rateLimitService.isMapQueryAllowed(userId, ip)) {
            int rateLimit = rateLimitService.getMapQueryLimit();
            throw new RateLimitExceededException(
                String.format("Rate limit exceeded. Maximum %d map queries per minute.", rateLimit), 60);
        }

        MapUsersResponse response;

        // Priority: bounds > center > nearby > country/city
        if (bounds != null && !bounds.trim().isEmpty()) {
            // Parse bounds: "minLat,minLng,maxLat,maxLng"
            String[] parts = bounds.split(",");
            if (parts.length != 4) {
                throw new com.tuttime.common.exception.BusinessException(
                    "Invalid bounds format. Expected: minLat,minLng,maxLat,maxLng");
            }
            double minLat = Double.parseDouble(parts[0].trim());
            double minLng = Double.parseDouble(parts[1].trim());
            double maxLat = Double.parseDouble(parts[2].trim());
            double maxLng = Double.parseDouble(parts[3].trim());
            
            response = userLocationService.getUsersInBounds(minLat, minLng, maxLat, maxLng, limit, offset, userId);
            
        } else if (center != null && !center.trim().isEmpty()) {
            // Parse center: "lat,lng,radiusMeters"
            String[] parts = center.split(",");
            if (parts.length != 3) {
                throw new com.tuttime.common.exception.BusinessException(
                    "Invalid center format. Expected: lat,lng,radiusMeters");
            }
            double lat = Double.parseDouble(parts[0].trim());
            double lng = Double.parseDouble(parts[1].trim());
            int radius = Integer.parseInt(parts[2].trim());
            
            response = userLocationService.getUsersNearby(lat, lng, radius, limit, offset, userId);
            
        } else if (nearby != null && !nearby.trim().isEmpty()) {
            // Parse nearby: "lat,lng,radiusMeters"
            String[] parts = nearby.split(",");
            if (parts.length != 3) {
                throw new com.tuttime.common.exception.BusinessException(
                    "Invalid nearby format. Expected: lat,lng,radiusMeters");
            }
            double lat = Double.parseDouble(parts[0].trim());
            double lng = Double.parseDouble(parts[1].trim());
            int radius = Integer.parseInt(parts[2].trim());
            
            response = userLocationService.getUsersNearby(lat, lng, radius, limit, offset, userId);
            
        } else if (country != null && !country.trim().isEmpty()) {
            // Query by country/city
            response = userLocationService.getUsersByLocation(country, city, limit, offset, userId);
            
        } else {
            throw new BusinessException(
                "One of bounds, center, nearby, or country parameter is required");
        }

        int remaining = rateLimitService.getRemainingMapQueries(userId, ip);
        int rateLimit = rateLimitService.getMapQueryLimit();
        return ResponseEntity.ok()
                .header("X-RateLimit-Limit", String.valueOf(rateLimit))
                .header("X-RateLimit-Remaining", String.valueOf(remaining))
                .body(ApiResponse.success(response));
    }

    /**
     * Extracts IP address from HTTP request.
     * 
     * @param request HTTP request
     * @return IP address
     */
    private String extractIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        // Check X-Forwarded-For header (for proxies/load balancers)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            String[] ips = xForwardedFor.split(",");
            return ips[0].trim();
        }

        // Check X-Real-IP header
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp.trim();
        }

        // Fallback to remote address
        return request.getRemoteAddr();
    }

    /**
     * Gets another user's location (respects privacy settings).
     * 
     * @param requestingUserId User ID requesting the location
     * @param targetUserId User ID whose location is requested
     * @return User location response (may be blurred based on privacy)
     */
    @GetMapping("/{userId}/location")
    @Operation(
        summary = "Get user location",
        description = "Retrieves another user's location. Coordinates are blurred based on privacy level."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "Location retrieved successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", 
            description = "User location is private"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", 
            description = "User not found"
        )
    })
    public ResponseEntity<ApiResponse<UserLocationResponse>> getUserLocation(
            @AuthenticationPrincipal Long requestingUserId,
            @Parameter(description = "User ID whose location is requested")
            @PathVariable Long userId) {
        log.debug("Getting location for user {} requested by {}", userId, requestingUserId);
        
        UserLocationResponse response = userLocationService.getUserLocation(requestingUserId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

