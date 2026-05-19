package com.tuttime.restaurant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class RestaurantDto {

    @Schema(description = "Restaurant request details")
    public record RestaurantRequest(

            @NotBlank(message = "Restaurant name is required")
            @Size(min = 2, max = 100, message = "Restaurant name must be at most 255 characters")
            String name,

            @Size(max = 5000, message = "Description can be up to 5000 characters")
            String description,

            @Size(max = 20, message = "Phone number must be at most 20 characters")
            @Pattern(regexp = "^\\+?[0-9\\-\\s]{8,20}$", message = "Phone must be a valid phone number")
            String phone,

            @Size(max = 255, message = "Website URL must be at most 255 characters")
            @Pattern(regexp = "^(https?://).*$", message = "Website must be a valid URL starting with http:// or https://", flags = Pattern.Flag.CASE_INSENSITIVE)
            String website,

            @NotNull(message = "If want generate branch, set it as true")
            Boolean generateBranch,

            @Size(max = 512, message = "Address must be at most 512 characters")
            String address,

            @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
            @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
            Double longitude,

            @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
            @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
            Double latitude
    ) {
    }

    public record RestaurantUpdateRequest(

            @NotBlank(message = "Restaurant name is required")
            @Size(min = 2, max = 100, message = "Restaurant name must be at most 255 characters")
            String name,

            @Size(max = 5000, message = "Description can be up to 5000 characters")
            String description,

            @Size(max = 20, message = "Phone number must be at most 20 characters")
            @Pattern(regexp = "^\\+?[0-9\\-\\s]{8,20}$", message = "Phone must be a valid phone number")
            String phone,

            @Size(max = 255, message = "Website URL must be at most 255 characters")
            @Pattern(regexp = "^(https?://).*$", message = "Website must be a valid URL starting with http:// or https://", flags = Pattern.Flag.CASE_INSENSITIVE)
            String website
    ) {
    }

    @Schema(description = "Restaurant response")
    @Builder
    public record RestaurantResponse(

            UUID restaurantId,
            String name,
            String description,
            String phone,
            String website,
            BigDecimal rating,
            String imageUrl,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }

    @Schema(description = "Restaurant response details")
    @Builder
    public record RestaurantDetailResponse(

            UUID restaurantId,
            String name,
            String description,
            String phone,
            String website,
            BigDecimal rating,
            List<MediaDto.MediaResponse> medias,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }
}
