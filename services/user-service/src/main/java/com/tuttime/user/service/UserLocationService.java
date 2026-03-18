package com.tuttime.user.service;

import com.tuttime.common.exception.BusinessException;
import com.tuttime.user.dto.*;

/**
 * User Location Service Interface
 * 
 * <p>Provides methods for managing user location data and geospatial queries.
 */
public interface UserLocationService {
    
    /**
     * Updates the current user's location.
     * 
     * @param userId User ID
     * @param request Location update request
     * @return Updated location response
     * @throws BusinessException if coordinates are invalid or update fails
     */
    UserLocationResponse updateLocation(Long userId, UpdateLocationRequest request) throws BusinessException;
    
    /**
     * Gets the current user's location.
     * 
     * @param userId User ID
     * @return User location response
     * @throws BusinessException if user not found
     */
    UserLocationResponse getMyLocation(Long userId) throws BusinessException;
    
    /**
     * Updates location privacy settings.
     * 
     * @param userId User ID
     * @param request Privacy settings request
     * @return Updated privacy settings
     * @throws BusinessException if user not found or invalid privacy level
     */
    UserLocationResponse updateLocationPrivacy(Long userId, LocationPrivacyRequest request) throws BusinessException;
    
    /**
     * Gets users on map within bounding box.
     * 
     * @param minLat Minimum latitude
     * @param minLng Minimum longitude
     * @param maxLat Maximum latitude
     * @param maxLng Maximum longitude
     * @param limit Maximum number of results
     * @param offset Pagination offset
     * @return Map users response
     */
    MapUsersResponse getUsersInBounds(double minLat, double minLng, double maxLat, double maxLng, int limit, int offset);
    
    /**
     * Gets users on map within bounding box (with requesting user ID).
     * 
     * @param minLat Minimum latitude
     * @param minLng Minimum longitude
     * @param maxLat Maximum latitude
     * @param maxLng Maximum longitude
     * @param limit Maximum number of results
     * @param offset Pagination offset
     * @param requestingUserId User ID requesting the map (optional, for future use)
     * @return Map users response
     */
    MapUsersResponse getUsersInBounds(double minLat, double minLng, double maxLat, double maxLng, int limit, int offset, Long requestingUserId);
    
    /**
     * Gets users nearby a center point within radius.
     * 
     * @param lat Center latitude
     * @param lng Center longitude
     * @param radiusMeters Radius in meters
     * @param limit Maximum number of results
     * @param offset Pagination offset
     * @return Map users response
     */
    MapUsersResponse getUsersNearby(double lat, double lng, int radiusMeters, int limit, int offset);
    
    /**
     * Gets users nearby a center point within radius (with requesting user ID).
     * 
     * @param lat Center latitude
     * @param lng Center longitude
     * @param radiusMeters Radius in meters
     * @param limit Maximum number of results
     * @param offset Pagination offset
     * @param requestingUserId User ID requesting the map (optional, for future use)
     * @return Map users response
     */
    MapUsersResponse getUsersNearby(double lat, double lng, int radiusMeters, int limit, int offset, Long requestingUserId);
    
    /**
     * Gets users by country and optionally city.
     * 
     * @param country Country name
     * @param city City name (optional)
     * @param limit Maximum number of results
     * @param offset Pagination offset
     * @return Map users response
     */
    MapUsersResponse getUsersByLocation(String country, String city, int limit, int offset);
    
    /**
     * Gets users by country and optionally city (with requesting user ID).
     * 
     * @param country Country name
     * @param city City name (optional)
     * @param limit Maximum number of results
     * @param offset Pagination offset
     * @param requestingUserId User ID requesting the map (optional, for future use)
     * @return Map users response
     */
    MapUsersResponse getUsersByLocation(String country, String city, int limit, int offset, Long requestingUserId);
    
    /**
     * Gets another user's location (respects privacy settings).
     * 
     * @param requestingUserId User ID requesting the location
     * @param targetUserId User ID whose location is requested
     * @return User location response (may be blurred based on privacy)
     * @throws BusinessException if user not found or location is private
     */
    UserLocationResponse getUserLocation(Long requestingUserId, Long targetUserId) throws BusinessException;
}

