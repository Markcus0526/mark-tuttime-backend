package com.tuttime.restaurant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Service for caching image URLs with 1-day TTL.
 * 
 * <p>This service generates and caches image URLs from S3 keys.
 * URLs are cached for 1 day to avoid regenerating presigned URLs on every request,
 * while ensuring URLs don't expire (presigned URLs typically last 24 hours or more).
 * 
 * <p>Cache Strategy:
 * <ul>
 *   <li>Cache key: Image key (e.g., "users/123/abc.jpg")</li>
 *   <li>Cache value: Generated URL (presigned URL or CDN URL)</li>
 *   <li>TTL: 1 day (24 hours)</li>
 * </ul>
 * 
 * <p>Benefits:
 * <ul>
 *   <li>Reduces S3 API calls for URL generation</li>
 *   <li>Improves response times (cache hit is much faster)</li>
 *   <li>URLs are regenerated once per day, keeping them fresh</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageUrlCacheService {
    
    private final ImageStorageService imageStorageService;
    
    /**
     * Gets cached image URL or generates a new one if not cached.
     * 
     * <p>If the stored value is already a URL (legacy data), returns it as-is without caching.
     * If it's an image key, generates a fresh URL (cached for 1 day).
     * 
     * @param storedValue The stored value from database (key or URL)
     * @return Generated URL (cached) or original URL if already a URL
     */
    public String getCachedImageUrl(String storedValue) {
        if (storedValue == null || storedValue.trim().isEmpty()) {
            return null;
        }
        
        // Check if it's already a URL (legacy data or CDN URL)
        // URLs typically start with "http://" or "https://"
        if (storedValue.startsWith("http://") || storedValue.startsWith("https://")) {
            // It's already a URL - return as-is without caching (backward compatibility)
            return storedValue;
        }
        
        // It's an image key - use cached URL generation
        return getCachedUrlForKey(storedValue);
    }
    
    /**
     * Gets cached image URL for an image key, generating a new one if not cached.
     * 
     * <p>This method is cached with 1-day TTL. The cache key is the image key itself.
     * 
     * @param imageKey Image key (e.g., "users/123/abc.jpg")
     * @return Generated URL (cached for 1 day)
     */
    @Cacheable(value = "image-urls", key = "#imageKey", unless = "#result == null")
    private String getCachedUrlForKey(String imageKey) {
        try {
            String imageUrl = imageStorageService.getImageUrl(imageKey);
            log.debug("Generated image URL for key: {} (will be cached for 1 day)", imageKey);
            return imageUrl;
        } catch (Exception e) {
            log.warn("Failed to generate image URL for key: {}", imageKey, e);
            return imageKey; // Fallback to returning key
        }
    }
    
    /**
     * Generates image URL from key without caching.
     * Used when you want to force a fresh URL generation.
     * 
     * @param imageKey Image key (e.g., "users/123/abc.jpg")
     * @return Generated URL
     */
    public String generateImageUrl(String imageKey) {
        if (imageKey == null || imageKey.trim().isEmpty()) {
            return null;
        }
        
        try {
            return imageStorageService.getImageUrl(imageKey);
        } catch (Exception e) {
            log.warn("Failed to generate image URL for key: {}", imageKey, e);
            return null;
        }
    }
}

