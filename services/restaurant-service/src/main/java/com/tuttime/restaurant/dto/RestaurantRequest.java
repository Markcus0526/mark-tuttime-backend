package com.tuttime.restaurant.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantRequest {

	@NotNull(message = "Owner user ID is required")
	private UUID ownerUserId;

	@NotBlank(message = "Restaurant name is required")
	@Size(max = 255, message = "Restaurant name must be at most 255 characters")
	private String name;

	@Size(max = 5000, message = "Description can be up to 5000 characters")
	private String description;

	@NotBlank(message = "Address is required")
	@Size(max = 512, message = "Address must be at most 512 characters")
	private String address;

	@DecimalMin(value = "-180.0", inclusive = true, message = "Longitude must be >= -180")
	@DecimalMax(value = "180.0", inclusive = true, message = "Longitude must be <= 180")
	private Double longitude;

	@DecimalMin(value = "-90.0", inclusive = true, message = "Latitude must be >= -90")
	@DecimalMax(value = "90.0", inclusive = true, message = "Latitude must be <= 90")
	private Double latitude;

	@Size(max = 20, message = "Phone number must be at most 20 characters")
	@Pattern(regexp = "^\\+?[0-9\\-\\s]{7,20}$", message = "Phone must be a valid phone number")
	private String phone;

	@Size(max = 255, message = "Website URL must be at most 255 characters")
	@Pattern(regexp = "^(https?://).*$", message = "Website must be a valid URL starting with http:// or https://", flags = Pattern.Flag.CASE_INSENSITIVE)
	private String website;

	@DecimalMin(value = "0.0", inclusive = true, message = "Rating must be at least 0.0")
	@DecimalMax(value = "5.0", inclusive = true, message = "Rating must be at most 5.0")
	private BigDecimal rating;
}