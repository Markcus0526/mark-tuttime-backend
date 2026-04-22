package com.tuttime.booking.mapper;

import com.tuttime.booking.dto.BookingDto;
import com.tuttime.booking.entity.Booking;
import com.tuttime.booking.entity.BookingItem;
import com.tuttime.common.event.proto.MenuItemDetailResponse;
import com.tuttime.common.event.proto.TableHierarchyResponse;
import com.tuttime.common.event.proto.UserDetailResponse;
import com.tuttime.common.util.UuidUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public abstract class BookingMapper {

	@Mapping(target = "bookingId", source = "entity.id")
	public abstract BookingDto.BookingResponse toResponse(Booking entity);

	@Mapping(target = "bookingId", source = "entity.id")
	@Mapping(target = "status", source = "entity.status")
	@Mapping(target = "user", source = "user")
	public abstract BookingDto.BookingResponse toWithUserResponse(Booking entity, UserDetailResponse user);

	@Mapping(target = "bookingId", source = "entity.id")
	@Mapping(target = "status", source = "entity.status")
	@Mapping(target = "user", source = "user")
	@Mapping(target = "table", source = "table")
	@Mapping(target = "menuItems", expression = "java(mapItemsWithDetails(entity.getItems(), menuDetails))")
	public abstract BookingDto.BookingDetailResponse toDetailResponse(
			Booking entity,
			UserDetailResponse user,
			TableHierarchyResponse table,
			List<MenuItemDetailResponse> menuDetails);

	List<BookingDto.BookingDetailResponse.MenuItemResponse> mapItemsWithDetails(List<BookingItem> items, List<MenuItemDetailResponse> details) {
		return items.stream().map(item -> {
			var detail = details.stream()
					.filter(d -> d.getMenuItemId().equals(item.getMenuItemId().toString()))
					.findFirst().orElse(null);

			return new BookingDto.BookingDetailResponse.MenuItemResponse(
					item.getMenuItemId(),
					detail != null ? detail.getName() : "Unknown Item",
					item.getQuantity(),
					item.getUnitPrice(),
					item.getTotalPrice(),
					item.getNotes());
		}).toList();
	}

	protected UUID map(String value) {
		return UuidUtil.toUuid(value);
	}
}
