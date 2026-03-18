package com.tuttime.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for image URL
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageUrlResponse {
    private String imageUrl;
}

