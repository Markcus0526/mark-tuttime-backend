package com.tuttime.order.mapper;

import com.tuttime.common.event.proto.MenuItemDetailResponse;
import com.tuttime.common.event.proto.TableHierarchyResponse;
import com.tuttime.common.event.proto.UserDetailResponse;
import com.tuttime.common.util.UuidUtil;
import com.tuttime.order.dto.OrderDto;
import com.tuttime.order.dto.OrderItemDto;
import com.tuttime.order.dto.OrderModifierDto;
import com.tuttime.order.entity.Order;
import com.tuttime.order.entity.OrderItem;
import com.tuttime.order.entity.OrderItemModifier;
import org.mapstruct.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring",
		unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class OrderMapper {

	// Main conversion: Entity -> Response DTO
	@Mapping(target = "orderId", source = "id")
	public abstract OrderDto.OrderResponse toResponse(Order order);

	// MapStruct automatically uses these for nested lists
	@Mapping(target = "orderItemId", source = "id")
	public abstract OrderItemDto.OrderItemResponse toItemResponse(OrderItem item);

	@Mapping(target = "modifierId", source = "id")
	public abstract OrderModifierDto.OrderModifierResponse toModifierResponse(OrderItemModifier modifier);

	@Mapping(target = "orderId", source = "order.id")
	@Mapping(target = "createdAt", source = "order.createdAt")
	@Mapping(target = "status", source = "order.status")
	@Mapping(target = "user", source = "user")
	@Mapping(target = "tables", source = "tables")
	@Mapping(target = "items", expression = "java(mapItemsWithDetails(order.getItems(), menuDetails))")
	public abstract OrderDto.OrderDetailResponse toDetailResponse(
			Order order,
			UserDetailResponse user,
			List<TableHierarchyResponse> tables,
			List<MenuItemDetailResponse> menuDetails);

	/**
	 * Enhanced mapping to include Item Names and Modifiers
	 */
	protected List<OrderDto.OrderDetailResponse.MenuItemDto> mapItemsWithDetails(
			List<OrderItem> items,
			List<MenuItemDetailResponse> details) {

		if (items == null) return Collections.emptyList();

		return items.stream().map(item -> {
			// Find matching menu detail from the gRPC list
			var detail = (details == null) ? null : details.stream()
			                                        .filter(d -> d.getMenuItemId().equals(item.getMenuItemId().toString()))
			                                        .findFirst()
			                                        .orElse(null);

			return OrderDto.OrderDetailResponse.MenuItemDto.builder()
					.menuItemId(item.getMenuItemId())
					.name(detail != null ? detail.getName() : "Unknown Item")
					.quantity(item.getQuantity())
					.price(item.getPrice())
					// Ensure you map modifiers here too!
					.modifiers(mapModifiersWithDetails(item.getModifiers(), detail))
					.build();
		}).toList();
	}

	protected List<OrderDto.OrderDetailResponse.ModifierDto> mapModifiersWithDetails(
			List<OrderItemModifier> selectedModifiers,
			MenuItemDetailResponse menuDetail) {

		if (selectedModifiers == null || selectedModifiers.isEmpty()) {
			return Collections.emptyList();
		}

		return selectedModifiers.stream().map(mod -> {
			// Flatten the groups to find the specific option ID
			var modOption = (menuDetail == null) ? null : menuDetail.getModifierGroupsList().stream()
			                                              .flatMap(group -> group.getOptionsList().stream())
			                                              .filter(option -> option.getOptionId().equals(mod.getModifierId().toString()))
			                                              .findFirst()
			                                              .orElse(null);

			return OrderDto.OrderDetailResponse.ModifierDto.builder()
					.modifierId(mod.getModifierId())
					.name(modOption != null ? modOption.getName() : "Extra Option")
					.priceAdjustment(mod.getPriceAdjustment())
					.build();
		}).toList();
	}

	protected UUID map(String value) {
		return UuidUtil.toUuid(value);
	}

	/**
	 * CRITICAL: Bidirectional Sync
	 * After MapStruct maps the lists, we must manually set the Parent-Child
	 * references so Hibernate knows how to fill the Foreign Key columns.
	 */
	@AfterMapping
	void linkItems(@MappingTarget Order order) {
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

