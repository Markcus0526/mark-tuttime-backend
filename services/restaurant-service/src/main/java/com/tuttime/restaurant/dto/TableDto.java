package com.tuttime.restaurant.dto;

import com.tuttime.restaurant.entity.enums.TableStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public class TableDto {

	@Schema(description = "Table request details")
	public record TableRequest(

			@NotNull(message = "Zone ID must not be null")
			UUID zoneId,

			@NotBlank(message = "Table label must not be blank")
			@Size(max = 50, message = "Table label must be at most 50 characters")
			String label,

			@Size(max = 5000, message = "Description can be up to 5000 characters")
			String notes,

			@NotNull(message = "Seats must not be null")
			@Min(value = 1, message = "Seats must be at least 1")
			Integer seats,

			@Enumerated(EnumType.STRING)
			@NotNull(message = "Table status is required")
			TableStatus status,

			@NotNull(message = "closed must not be null")
			Boolean closed
	) {
	}


	@Schema(description = "Table request details")
	public record TableResponse(

			UUID tableId,
			UUID zoneId,
			String label,
			String notes,
			int seats,
			TableStatus status,
			boolean closed,
			OffsetDateTime createdAt,
			OffsetDateTime updatedAt
	) {
	}
}
