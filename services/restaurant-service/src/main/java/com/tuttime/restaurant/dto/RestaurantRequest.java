package com.tuttime.restaurant.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

// --- RestaurantRequest DTO ---
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantRequest {

	@NotNull(message = "Owner user ID must not be null")
	private UUID ownerUserId;

	@NotBlank(message = "Restaurant name must not be blank")
	@Size(max = 255, message = "Name must be at most 255 characters")
	private String name;

	@Size(max = 1000, message = "Description must be at most 1000 characters")
	private String description;

	@NotBlank(message = "Address must not be blank")
	@Size(max = 512, message = "Address must be at most 512 characters")
	private String address;

	@Pattern(regexp = "^\\+?[0-9\\-\\s]{7,20}$", message = "Phone must be a valid phone number")
	private String phone;

	@Size(max = 255, message = "Website URL must be at most 255 characters")
	@Pattern(regexp = "^(https?://)?(www\\.)?([\\w-]+)(\\.[\\w-]+)+(/\\S*)?$", message = "Website must be a valid URL")
	private String website;

	@DecimalMin(value = "0.0", inclusive = true, message = "Rating must be between 0 and 5")
	@DecimalMax(value = "5.0", inclusive = true, message = "Rating must be between 0 and 5")
	private BigDecimal rating;

	// Getters and setters omitted for brevity
}