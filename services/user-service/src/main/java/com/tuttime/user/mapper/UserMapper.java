package com.tuttime.user.mapper;

import com.tuttime.user.dto.ProfileImageUrls;
import com.tuttime.user.dto.UserResponse;
import com.tuttime.user.entity.LocationPrivacyLevel;
import com.tuttime.user.entity.User;
import com.tuttime.user.entity.readmodel.UserReadModel;
import com.tuttime.user.service.ImageUrlCacheService;
import com.tuttime.user.util.LocationUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "latitude", source = "user", qualifiedByName = "mapLatitude")
    @Mapping(target = "longitude", source = "user", qualifiedByName = "mapLongitude")
    @Mapping(target = "locationPrivacyLevel", source = "user.locationPrivacyLevel", qualifiedByName = "mapPrivacyLevel")
    @Mapping(target = "locationSharingEnabled", source = "locationSharingEnabled")
    @Mapping(target = "gender", source = "user.gender", qualifiedByName = "mapGender")
    UserResponse toResponse(User user);
    
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "latitude", source = "readModel", qualifiedByName = "mapLatitudeFromReadModel")
    @Mapping(target = "longitude", source = "readModel", qualifiedByName = "mapLongitudeFromReadModel")
    @Mapping(target = "locationPrivacyLevel", source = "readModel.locationPrivacyLevel", qualifiedByName = "mapPrivacyLevel")
    @Mapping(target = "locationSharingEnabled", source = "locationSharingEnabled")
    @Mapping(target = "statusLevel", source = "readModel.statusLevel", qualifiedByName = "mapStatusLevel")
    @Mapping(target = "gender", source = "readModel.gender", qualifiedByName = "mapGender")
    @Mapping(target = "phone", ignore = true)
    @Mapping(target = "telegram", ignore = true)
    @Mapping(target = "country", ignore = true)
    @Mapping(target = "city", ignore = true)
    @Mapping(target = "kycVerifiedAt", ignore = true)
    @Mapping(target = "blockedReason", ignore = true)
    @Mapping(target = "blockedAt", ignore = true)
    UserResponse toResponse(UserReadModel readModel);
    
    /**
     * Maps latitude with privacy blurring.
     * Only includes location if sharing is enabled and privacy level is not PRIVATE.
     */
    @Named("mapLatitude")
    default BigDecimal mapLatitude(User user) {
        if (user == null || !Boolean.TRUE.equals(user.getLocationSharingEnabled()) ||
            user.getLocationPrivacyLevel() == LocationPrivacyLevel.PRIVATE ||
            user.getLatitude() == null) {
            return null;
        }
        
        // Apply privacy blurring
        LocationUtils.BlurredCoordinates blurred = LocationUtils.blurCoordinates(
            user.getLatitude(), 
            user.getLongitude(), 
            user.getLocationPrivacyLevel()
        );
        
        return blurred != null ? blurred.getLatitude() : null;
    }
    
    /**
     * Maps longitude with privacy blurring.
     * Only includes location if sharing is enabled and privacy level is not PRIVATE.
     */
    @Named("mapLongitude")
    default BigDecimal mapLongitude(User user) {
        if (user == null || !Boolean.TRUE.equals(user.getLocationSharingEnabled()) ||
            user.getLocationPrivacyLevel() == LocationPrivacyLevel.PRIVATE ||
            user.getLongitude() == null) {
            return null;
        }
        
        // Apply privacy blurring
        LocationUtils.BlurredCoordinates blurred = LocationUtils.blurCoordinates(
            user.getLatitude(), 
            user.getLongitude(), 
            user.getLocationPrivacyLevel()
        );
        
        return blurred != null ? blurred.getLongitude() : null;
    }
    
    /**
     * Maps latitude from read model with privacy blurring.
     */
    @Named("mapLatitudeFromReadModel")
    default BigDecimal mapLatitudeFromReadModel(UserReadModel readModel) {
        if (readModel == null || !Boolean.TRUE.equals(readModel.getLocationSharingEnabled()) ||
            readModel.getLocationPrivacyLevel() == LocationPrivacyLevel.PRIVATE ||
            readModel.getLatitude() == null) {
            return null;
        }
        
        // Apply privacy blurring
        LocationUtils.BlurredCoordinates blurred = LocationUtils.blurCoordinates(
            readModel.getLatitude(), 
            readModel.getLongitude(), 
            readModel.getLocationPrivacyLevel()
        );
        
        return blurred != null ? blurred.getLatitude() : null;
    }
    
    /**
     * Maps longitude from read model with privacy blurring.
     */
    @Named("mapLongitudeFromReadModel")
    default BigDecimal mapLongitudeFromReadModel(UserReadModel readModel) {
        if (readModel == null || !Boolean.TRUE.equals(readModel.getLocationSharingEnabled()) ||
            readModel.getLocationPrivacyLevel() == LocationPrivacyLevel.PRIVATE ||
            readModel.getLongitude() == null) {
            return null;
        }
        
        // Apply privacy blurring
        LocationUtils.BlurredCoordinates blurred = LocationUtils.blurCoordinates(
            readModel.getLatitude(), 
            readModel.getLongitude(), 
            readModel.getLocationPrivacyLevel()
        );
        
        return blurred != null ? blurred.getLongitude() : null;
    }
    
    /**
     * Maps privacy level to lowercase string.
     */
    @Named("mapPrivacyLevel")
    default String mapPrivacyLevel(LocationPrivacyLevel privacyLevel) {
        return privacyLevel != null ? privacyLevel.name().toLowerCase() : null;
    }
    
    /**
     * Maps status level to string.
     */
    @Named("mapStatusLevel")
    default String mapStatusLevel(com.tuttime.user.entity.readmodel.UserReadModel.UserStatusLevel statusLevel) {
        return statusLevel != null ? statusLevel.name() : null;
    }
    
    /**
     * Maps gender enum to string.
     */
    @Named("mapGender")
    default String mapGender(com.tuttime.common.constants.Gender gender) {
        return gender != null ? gender.name() : null;
    }
    
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
    default void enrichImageUrl(UserResponse response, ImageUrlCacheService imageUrlCacheService) {
        if (response == null) {
            return;
        }
        
        // Enrich original image URL
        if (response.getProfileImageUrl() != null) {
            String storedValue = response.getProfileImageUrl();
            if (!storedValue.startsWith("http://") && !storedValue.startsWith("https://")) {
                // Legacy data: it's a key, generate URL from key (backward compatibility only)
                String imageUrl = imageUrlCacheService.getCachedImageUrl(storedValue);
                response.setProfileImageUrl(imageUrl);
            }
        }
        
        // Enrich tiny thumbnail image URL (32x32)
        if (response.getProfileImageThumbnailUrl() != null) {
            String storedValue = response.getProfileImageThumbnailUrl();
            if (!storedValue.startsWith("http://") && !storedValue.startsWith("https://")) {
                // Legacy data: it's a key, generate URL from key (backward compatibility only)
                String thumbnailUrl = imageUrlCacheService.getCachedImageUrl(storedValue);
                response.setProfileImageThumbnailUrl(thumbnailUrl);
            }
        }
        
        // Enrich small thumbnail image URL (128x128)
        if (response.getProfileImageSmallUrl() != null) {
            String storedValue = response.getProfileImageSmallUrl();
            if (!storedValue.startsWith("http://") && !storedValue.startsWith("https://")) {
                // Legacy data: it's a key, generate URL from key (backward compatibility only)
                String smallUrl = imageUrlCacheService.getCachedImageUrl(storedValue);
                response.setProfileImageSmallUrl(smallUrl);
            }
        }
        
        // Enrich medium thumbnail image URL (512x512)
        if (response.getProfileImageMediumUrl() != null) {
            String storedValue = response.getProfileImageMediumUrl();
            if (!storedValue.startsWith("http://") && !storedValue.startsWith("https://")) {
                // Legacy data: it's a key, generate URL from key (backward compatibility only)
                String mediumUrl = imageUrlCacheService.getCachedImageUrl(storedValue);
                response.setProfileImageMediumUrl(mediumUrl);
            }
        }
        
        // Populate ProfileImageUrls nested DTO for easier access
        if (response.getProfileImageUrl() != null || response.getProfileImageThumbnailUrl() != null 
                || response.getProfileImageSmallUrl() != null || response.getProfileImageMediumUrl() != null) {
            response.setProfileImages(ProfileImageUrls.builder()
                    .tiny(response.getProfileImageThumbnailUrl())
                    .small(response.getProfileImageSmallUrl())
                    .medium(response.getProfileImageMediumUrl())
                    .original(response.getProfileImageUrl())
                    .build());
        }
    }
}

