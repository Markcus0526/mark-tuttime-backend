package com.tuttime.restaurant.dto;

import com.tuttime.restaurant.entity.enums.TableStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
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

	@Enumerated(EnumType.STRING)
	@NotNull(message = "Table status is required")
	private TableStatus status;

	@NotNull(message = "isClosed must not be null")
	private Boolean isClosed;

	// Getters and setters omitted for brevity

	// ... getters and setters ...
}
