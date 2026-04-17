package com.tuttime.booking.mapper;

import com.tuttime.booking.dto.BookingDto;
import com.tuttime.booking.entity.Booking;
import com.tuttime.booking.entity.BookingItem;
import com.tuttime.common.proto.BranchDetailResponse;
import com.tuttime.common.proto.MenuItemDetail;
import com.tuttime.common.proto.UserDetailResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class BookingMapper {

	@Mapping(target = "bookingId", source = "entity.id")
	public abstract BookingDto.BookingResponse toResponse(Booking entity);

//	@Mapping(target = "bookingId", source = "entity.id")
//	@Mapping(target = "user.userId", source = "user.userId")
//	@Mapping(target = "user.phoneNumber", source = "user.email")
//	@Mapping(target = "user.email", source = "user.email")
//	@Mapping(target = "user.fullName", source = "user.fullName", ignore = true)
//	public abstract BookingDto.BookingResponse toWithUserResponse(Booking entity, UserDetailResponse user);

	@Mapping(target = "bookingId", source = "entity.id")
	@Mapping(target = "status", source = "entity.status")
	@Mapping(target = "user.userId", source = "user.userId")
	@Mapping(target = "user.phoneNumber", source = "user.email")
	@Mapping(target = "user.email", source = "user.email")
	@Mapping(target = "user.fullName", source = "user.fullName")
	@Mapping(target = "branch.branchId", source = "branch.branchId")
	@Mapping(target = "branch.restaurantId", source = "branch.restaurantId")
	@Mapping(target = "branch.branchName", source = "branch.branchName")
	@Mapping(target = "branch.branchAddress", source = "branch.branchAddress")
	@Mapping(target = "branch.branchPhone", source = "branch.branchPhone")
	@Mapping(target = "branch.restaurantName", source = "branch.restaurantName")
	@Mapping(target = "branch.restaurantAddress", source = "branch.restaurantAddress")
	@Mapping(target = "branch.restaurantPhone", source = "branch.restaurantPhone")
	@Mapping(target = "items", expression = "java(mapItemsWithDetails(entity.getItems(), menuDetails))")
	public abstract BookingDto.BookingDetailResponse toDetailResponse(
			Booking entity,
			UserDetailResponse user,
			BranchDetailResponse branch,
			List<MenuItemDetail> menuDetails);

	List<BookingDto.BookingDetailResponse.BookingItemResponse> mapItemsWithDetails(List<BookingItem> items, List<MenuItemDetail> details) {
		return items.stream().map(item -> {
			var detail = details.stream()
					.filter(d -> d.getMenuItemId().equals(item.getMenuItemId().toString()))
					.findFirst().orElse(null);

			return new BookingDto.BookingDetailResponse.BookingItemResponse(
					item.getMenuItemId(),
					detail != null ? detail.getName() : "Unknown Item",
					item.getQuantity(),
					item.getUnitPrice(),
					item.getTotalPrice(),
					item.getNotes());
		}).toList();
	}
}
