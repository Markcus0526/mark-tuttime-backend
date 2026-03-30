package com.tuttime.restaurant.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZoneRequest {

	@NotNull(message = "Branch ID must not be null")
	private UUID branchId;

	@NotBlank(message = "Name must not be blank")
	@Size(max = 255, message = "Name must be at most 255 characters")
	private String name;

	@Size(max = 1000, message = "Description must be at most 1000 characters")
	private String description;

	@NotNull(message = "Capacity must not be null")
	@Min(value = 1, message = "Capacity must be at least 1")
	private Integer capacity;

	@NotNull(message = "isClosed must not be null")
	private Boolean isClosed;

	// Getters and setters omitted for brevity

	// ... getters and setters ...
}