package com.tuttime.booking.mapper;

import com.tuttime.booking.dto.BookingDto;
import com.tuttime.booking.dto.BookingHistoryDto;
import com.tuttime.booking.entity.Booking;
import com.tuttime.booking.entity.BookingItem;
import com.tuttime.booking.entity.BookingStatusHistory;
import com.tuttime.common.proto.BranchDetailResponse;
import com.tuttime.common.proto.MenuItemDetail;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class BookingHistoryMapper {

	@Mapping(target = "historyId", source = "id")
	public abstract BookingHistoryDto.BookingHistoryResponse toResponse(BookingStatusHistory history);
}
