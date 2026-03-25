package com.tuttime.menu.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StopListDto {
	private Long id;

	@NotNull(message = "Menu item ID is required")
	private Long menuItemId;

	@NotNull(message = "Branch ID is required")
	private Long branchId;

	@NotNull(message = "Start date is required")
	private LocalDate startDate;

	private LocalDate endDate;

	@Size(max = 255)
	private String reason;

}