package com.tuttime.booking.mapper;

import com.tuttime.booking.dto.BookingHistoryDto;
import com.tuttime.booking.entity.BookingStatusHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class BookingHistoryMapper {

	@Mapping(target = "historyId", source = "id")
	public abstract BookingHistoryDto.BookingHistoryResponse toResponse(BookingStatusHistory history);
}
