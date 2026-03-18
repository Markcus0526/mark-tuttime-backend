package com.tuttime.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of multi-size profile image upload.
 * 
 * <p>Contains S3 keys for all image sizes:
 * <ul>
 *   <li>tinyKey - key for 32x32px thumbnail (e.g., "users/123/abc_tiny.jpg")</li>
 *   <li>smallKey - key for 128x128px thumbnail (e.g., "users/123/abc_small.jpg")</li>
 *   <li>mediumKey - key for 512x512px thumbnail (e.g., "users/123/abc_medium.jpg")</li>
 *   <li>originalKey - key for 1024x1024px original (e.g., "users/123/abc.jpg")</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileImageResult {
    
    /**
     * S3 key for tiny thumbnail (32x32px).
     * Example: "users/123/abc_tiny.jpg"
     */
    private String tinyKey;
    
    /**
     * S3 key for small thumbnail (128x128px).
     * Example: "users/123/abc_small.jpg"
     */
    private String smallKey;
    
    /**
     * S3 key for medium thumbnail (512x512px).
     * Example: "users/123/abc_medium.jpg"
     */
    private String mediumKey;
    
    /**
     * S3 key for original image (1024x1024px).
     * Example: "users/123/abc.jpg"
     */
    private String originalKey;
}

