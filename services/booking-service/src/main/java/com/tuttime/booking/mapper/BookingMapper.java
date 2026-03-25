package com.tuttime.booking.mapper;

import com.tuttime.booking.dto.BookingDto;
import com.tuttime.booking.entity.Booking;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public class BookingMapper {

	// Mapper methods
	public BookingDto toDto(Booking booking) {
		BookingDto dto = new BookingDto();
		dto.setBookingId(booking.getBookingId());
		dto.setUserId(booking.getUserId());
		dto.setRestaurantId(booking.getRestaurantId());
		dto.setBranchId(booking.getBranchId());
		dto.setZoneId(booking.getZoneId());
		dto.setTableId(booking.getTableId());
		dto.setBookingStatus(booking.getBookingStatus());
		dto.setBookingDatetime(booking.getBookingDatetime());
		dto.setPartySize(booking.getPartySize());
		dto.setLeadTimeMinutes(booking.getLeadTimeMinutes());
		dto.setGracePeriodMins(booking.getGracePeriodMins());
		dto.setCreatedAt(booking.getCreatedAt());
		dto.setUpdatedAt(booking.getUpdatedAt());
		dto.setCancellationReason(booking.getCancellationReason());
		dto.setAutoCancelled(booking.getAutoCancelled());
		return dto;
	}

	public Booking toEntity(BookingDto dto) {
		Booking booking = new Booking();
		booking.setBookingId(dto.getBookingId());
		booking.setUserId(dto.getUserId());
		booking.setRestaurantId(dto.getRestaurantId());
		booking.setBranchId(dto.getBranchId());
		booking.setZoneId(dto.getZoneId());
		booking.setTableId(dto.getTableId());
		booking.setBookingStatus(dto.getBookingStatus());
		booking.setBookingDatetime(dto.getBookingDatetime());
		booking.setPartySize(dto.getPartySize());
		booking.setLeadTimeMinutes(dto.getLeadTimeMinutes());
		booking.setGracePeriodMins(dto.getGracePeriodMins());
		booking.setCancellationReason(dto.getCancellationReason());
		booking.setAutoCancelled(dto.getAutoCancelled());
		return booking;
	}

}
