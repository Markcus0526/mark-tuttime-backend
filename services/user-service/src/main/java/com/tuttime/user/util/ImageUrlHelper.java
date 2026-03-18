package com.tuttime.user.util;

import com.tuttime.user.service.ImageStorageService;
import com.tuttime.user.service.ImageUrlCacheService;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper utility for generating image URLs from image keys.
 * 
 * <p>Handles backward compatibility by detecting if the stored value is:
 * - An image key (e.g., "users/123/abc.jpg") - generates fresh URL (cached for 1 day)
 * - A full URL (e.g., "https://...") - returns as-is (legacy data)
 * 
 * @deprecated Use ImageUrlCacheService directly for better caching support.
 * This class is kept for backward compatibility but will delegate to ImageUrlCacheService.
 */
@Slf4j
@Deprecated
public class ImageUrlHelper {
    
    /**
     * Checks if the stored value is an image key (vs a full URL).
     * 
     * <p>Image keys have format: "users/{userId}/{filename}.jpg"
     * URLs have format: "https://..." or "http://..." or "/api/..."
     * 
     * @param storedValue The stored value from database
     * @return true if it appears to be an image key, false if it's a URL
     */
    public static boolean isImageKey(String storedValue) {
        if (storedValue == null || storedValue.trim().isEmpty()) {
            return false;
        }
        String trimmed = storedValue.trim();
        // Image keys start with "users/" and don't start with "http"
        return trimmed.startsWith("users/") && !trimmed.startsWith("http");
    }
    
    /**
     * Generates an image URL from the stored value (key or URL).
     * 
     * <p>If the stored value is a key, generates a fresh URL using the storage service.
     * If it's already a URL, returns it as-is (for backward compatibility with legacy data).
     * 
     * @deprecated Use ImageUrlCacheService.getCachedImageUrl() instead for cached URLs.
     * 
     * @param storedValue The stored value from database (key or URL)
     * @param imageStorageService The image storage service to generate URLs
     * @return Generated URL or the original URL if stored value is already a URL
     */
    @Deprecated
    public static String generateImageUrl(String storedValue, ImageStorageService imageStorageService) {
        if (storedValue == null || storedValue.trim().isEmpty()) {
            return null;
        }
        
        if (isImageKey(storedValue)) {
            // It's a key - generate fresh URL (not cached in this deprecated method)
            try {
                return imageStorageService.getImageUrl(storedValue);
            } catch (Exception e) {
                log.warn("Failed to generate URL from image key: {}", storedValue, e);
                return storedValue; // Fallback to returning key
            }
        } else {
            // It's already a URL - return as-is (legacy data, backward compatibility)
            return storedValue;
        }
    }
    
    /**
     * Extracts image key from a URL (for backward compatibility).
     * 
     * <p>Tries to extract the key from URLs like:
     * - "https://s3.../users/123/abc.jpg" -> "users/123/abc.jpg"
     * - "https://cdn.../users/123/abc.jpg" -> "users/123/abc.jpg"
     * - "users/123/abc.jpg" -> "users/123/abc.jpg" (already a key)
     * 
     * @param urlOrKey URL or key
     * @return Extracted key, or null if extraction fails
     */
    public static String extractImageKeyFromUrl(String urlOrKey) {
        if (urlOrKey == null || urlOrKey.trim().isEmpty()) {
            return null;
        }
        
        // If it's already a key, return as-is
        if (isImageKey(urlOrKey)) {
            return urlOrKey;
        }
        
        // Try to extract key from URL
        int usersIndex = urlOrKey.indexOf("users/");
        if (usersIndex >= 0) {
            return urlOrKey.substring(usersIndex);
        }
        
        return null;
    }
}

