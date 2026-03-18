package com.tuttime.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Profile image URLs in different sizes.
 * 
 * <p>This DTO contains URLs for different image sizes optimized for various use cases:
 * <ul>
 *   <li>tiny (32x32px) - for map markers, compact lists, small avatars (~1-3KB)</li>
 *   <li>small (128x128px) - for chat headers, profile cards, medium avatars (~5-10KB)</li>
 *   <li>medium (512x512px) - for profile previews, modals, large cards (~30-50KB)</li>
 *   <li>original (1024x1024px) - for full profile view, detailed display (~100-300KB)</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Profile image URLs in different sizes")
public class ProfileImageUrls {

    @Schema(description = "Tiny thumbnail (32x32px) for map markers, compact lists, small avatars", example = "https://...")
    private String tiny;

    @Schema(description = "Small thumbnail (128x128px) for chat headers, profile cards, medium avatars", example = "https://...")
    private String small;

    @Schema(description = "Medium thumbnail (512x512px) for profile previews, modals, large cards", example = "https://...")
    private String medium;

    @Schema(description = "Original image (1024x1024px) for full profile view, detailed display", example = "https://...")
    private String original;
}

