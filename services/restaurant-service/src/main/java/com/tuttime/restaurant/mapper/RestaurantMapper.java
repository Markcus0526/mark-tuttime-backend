package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.AboutImageUrls;
import com.tuttime.restaurant.dto.RestaurantResponse;
import com.tuttime.restaurant.entity.Restaurant;
import com.tuttime.restaurant.entity.readmodel.RestaurantReadModel;
import com.tuttime.restaurant.service.ImageUrlCacheService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface RestaurantMapper {
    @Mapping(target = "restaurantId", source = "restaurantId")
    RestaurantResponse toResponse(Restaurant restaurant);
    
    @Mapping(target = "restaurantId", source = "restaurantId")
    @Mapping(target = "phone", ignore = true)
    @Mapping(target = "telegram", ignore = true)
    @Mapping(target = "country", ignore = true)
    @Mapping(target = "city", ignore = true)
    @Mapping(target = "blockedReason", ignore = true)
    @Mapping(target = "blockedAt", ignore = true)
    RestaurantResponse toResponse(RestaurantReadModel readModel);

    /**
     * Enriches UserResponse with image URLs (all sizes).
     * 
     * <p>URLs are stored directly in the database (presigned URLs generated at upload time).
     * Returns the URL from database directly without regeneration.
     * For backward compatibility with legacy data that might have keys, generates URL from key.
     * 
     * @param response The UserResponse to enrich
     * @param imageUrlCacheService The image URL cache service (for backward compatibility with legacy keys)
     */
    default void enrichImageUrl(RestaurantResponse response, ImageUrlCacheService imageUrlCacheService) {
        if (response == null) {
            return;
        }
        
        // Enrich original image URL
        if (response.getAboutImageUrl() != null) {
            String storedValue = response.getAboutImageUrl();
            if (!storedValue.startsWith("http://") && !storedValue.startsWith("https://")) {
                // Legacy data: it's a key, generate URL from key (backward compatibility only)
                String imageUrl = imageUrlCacheService.getCachedImageUrl(storedValue);
                response.setAboutImageUrl(imageUrl);
            }
        }
        
        // Enrich tiny thumbnail image URL (32x32)
        if (response.getAboutImageThumbnailUrl() != null) {
            String storedValue = response.getAboutImageThumbnailUrl();
            if (!storedValue.startsWith("http://") && !storedValue.startsWith("https://")) {
                // Legacy data: it's a key, generate URL from key (backward compatibility only)
                String thumbnailUrl = imageUrlCacheService.getCachedImageUrl(storedValue);
                response.setAboutImageThumbnailUrl(thumbnailUrl);
            }
        }
        
        // Enrich small thumbnail image URL (128x128)
        if (response.getAboutImageSmallUrl() != null) {
            String storedValue = response.getAboutImageSmallUrl();
            if (!storedValue.startsWith("http://") && !storedValue.startsWith("https://")) {
                // Legacy data: it's a key, generate URL from key (backward compatibility only)
                String smallUrl = imageUrlCacheService.getCachedImageUrl(storedValue);
                response.setAboutImageSmallUrl(smallUrl);
            }
        }
        
        // Enrich medium thumbnail image URL (512x512)
        if (response.getAboutImageMediumUrl() != null) {
            String storedValue = response.getAboutImageMediumUrl();
            if (!storedValue.startsWith("http://") && !storedValue.startsWith("https://")) {
                // Legacy data: it's a key, generate URL from key (backward compatibility only)
                String mediumUrl = imageUrlCacheService.getCachedImageUrl(storedValue);
                response.setAboutImageMediumUrl(mediumUrl);
            }
        }
        
        // Populate AboutImageUrls nested DTO for easier access
        if (response.getAboutImageUrl() != null || response.getAboutImageThumbnailUrl() != null 
                || response.getAboutImageSmallUrl() != null || response.getAboutImageMediumUrl() != null) {
            response.setAboutImages(AboutImageUrls.builder()
                    .tiny(response.getAboutImageThumbnailUrl())
                    .small(response.getAboutImageSmallUrl())
                    .medium(response.getAboutImageMediumUrl())
                    .original(response.getAboutImageUrl())
                    .build());
        }
    }
}

