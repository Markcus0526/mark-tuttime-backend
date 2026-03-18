package com.tuttime.user.entity;

/**
 * Location Privacy Level Enumeration
 * 
 * <p>Defines the privacy levels and update modes for user location:
 * <ul>
 *   <li>EXACT - Auto-detect mode: Location automatically updated from device/app with exact coordinates</li>
 *   <li>PRIVATE - No location sharing, location data is cleared</li>
 *   <li>STATIC - Static mode: Location manually selected from map, only updatable on profile page</li>
 * </ul>
 */
public enum LocationPrivacyLevel {
    /** Auto-detect mode: Exact coordinates, automatically updated from device/app */
    EXACT,
    
    /** No location sharing - location not visible on map, location data cleared */
    PRIVATE,
    
    /** Static mode: Location manually selected from map, only updatable on profile page */
    STATIC
}

