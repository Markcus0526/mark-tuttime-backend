package com.tuttime.order.mapper;

import com.tuttime.order.dto.OrderDto;
import com.tuttime.order.dto.OrderItemDto;
import com.tuttime.order.dto.OrderModifierDto;
import com.tuttime.order.entity.Order;
import com.tuttime.order.entity.OrderItem;
import com.tuttime.order.entity.OrderItemModifier;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
		unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

	// Main conversion: Entity -> Response DTO
	@Mapping(target = "orderId", source = "id")
	OrderDto.OrderResponse toResponse(Order order);

	// MapStruct automatically uses these for nested lists
	@Mapping(target = "orderItemId", source = "id")
	OrderItemDto.OrderItemResponse toItemResponse(OrderItem item);

	@Mapping(target = "modifierId", source = "id")
	OrderModifierDto.OrderModifierResponse toModifierResponse(OrderItemModifier modifier);

	// Main update method
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "bookingId", ignore = true) // Booking ID shouldn't change
	@Mapping(target = "branchId", ignore = true)  // Branch ID shouldn't change
	@Mapping(target = "totalAmount", ignore = true) // Recalculated in Service
	@Mapping(target = "items", source = "items")
	void updateEntityFromDto(OrderDto.OrderUpdateRequest dto, @MappingTarget Order order);

	// Maps DTO item to Entity item
	OrderItem toItemEntity(OrderItemDto.OrderItemRequest itemDto);

	// Maps DTO modifier to Entity modifier
	OrderItemModifier toModifierEntity(OrderModifierDto.OrderModifierRequest modDto);

	/**
	 * CRITICAL: Bidirectional Sync
	 * After MapStruct maps the lists, we must manually set the Parent-Child
	 * references so Hibernate knows how to fill the Foreign Key columns.
	 */
	@AfterMapping
	default void linkItems(@MappingTarget Order order) {
		if (order.getItems() != null) {
			order.getItems().forEach(item -> {
				item.setOrder(order); // Sets fk_order_items_orders (order_id)
				if (item.getModifiers() != null) {
					item.getModifiers().forEach(modifier ->
							modifier.setOrderItem(item) // Sets fk_modifiers_order_item (order_item_id)
					);
				}
			});
		}
	}
}

