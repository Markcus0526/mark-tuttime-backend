package com.tuttime.booking.mapper;

import com.tuttime.booking.dto.BookingDto;
import com.tuttime.booking.entity.Booking;
import com.tuttime.booking.entity.BookingItem;
import com.tuttime.common.event.proto.GetTableHierarchyDetailResponse;
import com.tuttime.common.event.proto.GetUserDetailResponse;
import com.tuttime.common.event.proto.MenuItemDetailPto;
import com.tuttime.common.util.UuidUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public abstract class BookingMapper {

    @Mapping(target = "bookingId", source = "entity.id")
    @Mapping(target = "user", ignore = true)
    public abstract BookingDto.BookingResponse toResponse(Booking entity);

    @Mapping(target = "bookingId", source = "entity.id")
    @Mapping(target = "status", source = "entity.status")
    @Mapping(target = "user", source = "user")
    public abstract BookingDto.BookingResponse toWithUserResponse(Booking entity, GetUserDetailResponse user);

    // Updated: target is "tables" (list), source is "tables" (list) – MapStruct will map each element
    @Mapping(target = "bookingId", source = "entity.id")
    @Mapping(target = "status", source = "entity.status")
    @Mapping(target = "user", source = "user")
    @Mapping(target = "tables", source = "tables")   // now list → list
    @Mapping(target = "menuItems", expression = "java(mapItemsWithDetails(entity.getItems(), menuDetails))")
    public abstract BookingDto.BookingDetailResponse toDetailResponse(
            Booking entity,
            GetUserDetailResponse user,
            List<GetTableHierarchyDetailResponse> tables,
            List<MenuItemDetailPto> menuDetails);

    // Helper method to convert GetTableHierarchyDetailResponse to BookingDto.BookingDetailResponse.TableDetailResponse
    protected BookingDto.BookingDetailResponse.TableDetailResponse toTableDetail(GetTableHierarchyDetailResponse response) {
        if (response == null) return null;
        return new BookingDto.BookingDetailResponse.TableDetailResponse(
                UuidUtil.toUuid(response.getBranchId()),
                UuidUtil.toUuid(response.getRestaurantId()),
                UuidUtil.toUuid(response.getZoneId()),
                UuidUtil.toUuid(response.getTableId()),
                response.getRestaurantName(),
                response.getBranchName(),
                response.getBranchAddress(),
                response.getBranchPhone(),
                response.getZoneName(),
                response.getTableLabel()
        );
    }

    List<BookingDto.BookingDetailResponse.MenuItemResponse> mapItemsWithDetails(List<BookingItem> items, List<MenuItemDetailPto> details) {
        if (items == null || details == null) return List.of();
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