package com.tuttime.restaurant.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchRequest {

	@NotNull(message = "Restaurant ID must not be null")
	private UUID restaurantId;

	@NotBlank(message = "Address must not be blank")
	@Size(max = 512, message = "Address must be at most 512 characters")
	private String address;

	@Pattern(regexp = "^\\+?[0-9\\-\\s]{7,20}$", message = "Phone must be a valid phone number")
	private String phone;

	@NotBlank(message = "Timezone must not be blank")
	@Size(max = 50, message = "Timezone must be at most 50 characters")
	private String timezone;

	@NotNull(message = "Latitude must not be null")
	@DecimalMin(value = "-90.0", inclusive = true, message = "Latitude must be between -90 and 90")
	@DecimalMax(value = "90.0", inclusive = true, message = "Latitude must be between -90 and 90")
	private Double latitude;

	@NotNull(message = "Longitude must not be null")
	@DecimalMin(value = "-180.0", inclusive = true, message = "Longitude must be between -180 and 180")
	@DecimalMax(value = "180.0", inclusive = true, message = "Longitude must be between -180 and 180")
	private Double longitude;

	// Getters and setters omitted for brevity

	// ... getters and setters ...
}