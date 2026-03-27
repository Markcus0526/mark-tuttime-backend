package com.tuttime.restaurant.dto;

import java.util.UUID;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableRequest {

	@NotNull(message = "Zone ID must not be null")
	private UUID zoneId;

	@NotBlank(message = "Table number must not be blank")
	@Size(max = 50, message = "Table number must be at most 50 characters")
	private String number;

	@NotNull(message = "Seats must not be null")
	@Min(value = 1, message = "Seats must be at least 1")
	private Integer seats;

	@NotBlank(message = "Status must not be blank")
	@Size(max = 20, message = "Status must be at most 20 characters")
	private String status;

	@NotNull(message = "isClosed must not be null")
	private Boolean isClosed;

	// Getters and setters omitted for brevity

	// ... getters and setters ...
}
