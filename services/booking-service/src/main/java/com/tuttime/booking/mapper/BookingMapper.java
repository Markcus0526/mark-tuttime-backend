package com.tuttime.booking.mapper;

import com.tuttime.booking.dto.BookingDto;
import com.tuttime.booking.entity.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public abstract class BookingMapper {

	@Mapping(target = "paymentUrl", ignore = true)
	public abstract BookingDto.BookingResponse toResponse(Booking booking);
}
