package com.tuttime.restaurant.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BranchRequest {

	@NotNull(message = "Restaurant ID is required")
	private UUID restaurantId;

	@NotNull(message = "Manager user ID is required")
	private UUID managerUserId;

	@NotBlank(message = "Address is required")
	@Size(max = 512, message = "Address must be at most 512 characters")
	private String address;

	@Size(max = 20, message = "Phone must be at most 20 characters")
	@Pattern(regexp = "^\\+?[0-9\\-\\s]{7,20}$", message = "Phone must be a valid phone number")
	private String phone;

	@NotBlank(message = "Timezone is required")
	@Size(max = 50, message = "Timezone must be at most 50 characters")
	private String timezone;

	@DecimalMin(value = "-180.0", inclusive = true, message = "Longitude must be >= -180")
	@DecimalMax(value = "180.0", inclusive = true, message = "Longitude must be <= 180")
	private Double longitude;

	@DecimalMin(value = "-90.0", inclusive = true, message = "Latitude must be >= -90")
	@DecimalMax(value = "90.0", inclusive = true, message = "Latitude must be <= 90")
	private Double latitude;
}

