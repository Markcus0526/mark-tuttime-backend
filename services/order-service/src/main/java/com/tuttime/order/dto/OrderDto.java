package com.tuttime.order.dto;

import com.tuttime.common.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class OrderDto {

	@Schema(description = "Request object for creating a new order")
	public record OrderRequest(
			@NotNull @Schema(description = "Associated booking ID", example = "550e8400-e29b-41d4-a716-446655440000")
			UUID bookingId,

			@NotNull @Schema(description = "ID of the user placing the order")
			UUID userId,

			@NotEmpty @Schema(description = "List of items in the order")
			List<OrderItemDto.OrderItemRequest> items
	) {
	}

	public record OrderUpdateRequest(
			@NotEmpty @Schema(description = "The complete list of items for the order")
			List<OrderItemDto.OrderItemUpdateRequest> items
	) {
	}

	public record PosSyncRequest(
			@NotNull(message = "POS Status is required")
			OrderStatus posStatus,
			String posOrderId,       // The ID from the physical system (e.g., iiko ID)
			String kitchenNote,      // Optional notes from the chef
			OffsetDateTime syncTime  // Timestamp from the restaurant hardware
	) {
	}

	public record OrderResponse(

			UUID orderId,

			UUID bookingId,

			UUID userId,

			OrderStatus status,

			BigDecimal totalAmount,

			String guestName,

			String guestPhone,

			String specialRequest,

			List<OrderItemDto.OrderItemResponse> items,

			OffsetDateTime createdAt
	) {
	}

	public record OrderDetailResponse(

			UUID orderId,

			UUID bookingId,

			OrderStatus status,

			BigDecimal totalAmount,

			String guestName,

			String guestPhone,

			String specialRequest,

			List<MenuItemDto> items,

			UserDetailDto user,

			TableDetailDto table,

			OffsetDateTime createdAt
	) {
		public record UserDetailDto(
				UUID userId,
				String phoneNumber,
				String email,
				String fullName
		) {
		}

		public record TableDetailDto(
				UUID branchId,
				UUID restaurantId,
				UUID zoneId,
				UUID tableId,
				String restaurantName,
				String branchName,
				String branchAddress,
				String branchPhone,
				String zoneName,
				String tableLabel
		) {
		}

		@Builder
		public record MenuItemDto(
				UUID menuItemId,
				String name,      // From Menu gRPC
				Integer quantity,     // From local DB
				BigDecimal price,
				List<ModifierDto> modifiers
		) {
		}

		@Builder
		public record ModifierDto(
				UUID modifierId,
				String name,           // Enriched from Menu Service
				BigDecimal priceAdjustment // From Order Service DB
		) {
		}
	}
}