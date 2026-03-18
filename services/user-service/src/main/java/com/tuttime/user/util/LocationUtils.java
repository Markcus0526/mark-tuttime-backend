package com.tuttime.user.util;

import com.tuttime.user.entity.LocationPrivacyLevel;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Location Utility Class
 * 
 * <p>Provides utility methods for location operations including:
 * <ul>
 *   <li>Coordinate blurring based on privacy level</li>
 *   <li>Point creation from coordinates</li>
 *   <li>Distance calculations</li>
 * </ul>
 */
@Slf4j
public class LocationUtils {
    
    private static final GeometryFactory geometryFactory = new GeometryFactory();
    
    /**
     * Creates a PostGIS Point from latitude and longitude.
     * 
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @return Point geometry (SRID 4326)
     */
    public static Point createPoint(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            return null;
        }
        
        Coordinate coordinate = new Coordinate(
            longitude.doubleValue(),
            latitude.doubleValue()
        );
        
        Point point = geometryFactory.createPoint(coordinate);
        point.setSRID(4326); // WGS84
        
        return point;
    }
    
    /**
     * Blurs coordinates based on privacy level.
     * 
     * <p>Privacy levels:
     * <ul>
     *   <li>EXACT - Returns exact coordinates (auto-detect mode)</li>
     *   <li>STATIC - Returns exact coordinates (static mode, manually selected)</li>
     *   <li>PRIVATE - Returns null (no location sharing)</li>
     * </ul>
     * 
     * @param latitude Original latitude
     * @param longitude Original longitude
     * @param privacyLevel Privacy level
     * @return Blurred coordinates (or null if private)
     */
    public static BlurredCoordinates blurCoordinates(
            BigDecimal latitude, 
            BigDecimal longitude, 
            LocationPrivacyLevel privacyLevel) {
        
        if (latitude == null || longitude == null) {
            return null;
        }
        
        switch (privacyLevel) {
            case EXACT:
                // Auto-detect mode: exact coordinates
                return new BlurredCoordinates(latitude, longitude);
                
            case STATIC:
                // Static mode: exact coordinates (manually selected by user)
                return new BlurredCoordinates(latitude, longitude);
                
            case PRIVATE:
                // Private mode: no location sharing
                return null;
                
            default:
                log.warn("Unknown privacy level: {}, defaulting to EXACT", privacyLevel);
                return new BlurredCoordinates(latitude, longitude);
        }
    }
    
    /**
     * Calculates distance between two coordinates using Haversine formula.
     * 
     * @param lat1 Latitude of first point
     * @param lng1 Longitude of first point
     * @param lat2 Latitude of second point
     * @param lng2 Longitude of second point
     * @return Distance in meters
     */
    public static double calculateDistance(
            double lat1, double lng1, 
            double lat2, double lng2) {
        
        final int EARTH_RADIUS_METERS = 6371000;
        
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLng = Math.toRadians(lng2 - lng1);
        
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS_METERS * c;
    }
    
    /**
     * Validates latitude coordinate.
     * 
     * @param latitude Latitude to validate
     * @throws BusinessException if latitude is invalid
     */
    public static void validateLatitude(BigDecimal latitude) {
        if (latitude == null) {
            throw new com.tuttime.common.exception.BusinessException("Latitude is required");
        }
        
        if (latitude.compareTo(BigDecimal.valueOf(-90)) < 0 || 
            latitude.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new com.tuttime.common.exception.BusinessException(
                "Latitude must be between -90 and 90");
        }
    }
    
    /**
     * Validates longitude coordinate.
     * 
     * @param longitude Longitude to validate
     * @throws BusinessException if longitude is invalid
     */
    public static void validateLongitude(BigDecimal longitude) {
        if (longitude == null) {
            throw new com.tuttime.common.exception.BusinessException("Longitude is required");
        }
        
        if (longitude.compareTo(BigDecimal.valueOf(-180)) < 0 || 
            longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new com.tuttime.common.exception.BusinessException(
                "Longitude must be between -180 and 180");
        }
    }
    
    /**
     * Validates location accuracy.
     * 
     * @param accuracy Accuracy in meters
     * @throws BusinessException if accuracy is invalid
     */
    public static void validateAccuracy(Integer accuracy) {
        if (accuracy != null && (accuracy < 1 || accuracy > 10000)) {
            throw new com.tuttime.common.exception.BusinessException(
                "Location accuracy must be between 1 and 10000 meters");
        }
    }
    
    /**
     * Blurred coordinates result
     */
    public static class BlurredCoordinates {
        private final BigDecimal latitude;
        private final BigDecimal longitude;
        
        public BlurredCoordinates(BigDecimal latitude, BigDecimal longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
        
        public BigDecimal getLatitude() {
            return latitude;
        }
        
        public BigDecimal getLongitude() {
            return longitude;
        }
    }
}

