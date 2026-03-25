package com.tuttime.booking.mapper;

import com.tuttime.booking.dto.BookingStatusHistoryDto;
import com.tuttime.booking.entity.BookingStatusHistory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public class BookingStatusHistoryMapper {

	public BookingStatusHistoryDto toDto(BookingStatusHistory entity) {
		BookingStatusHistoryDto dto = new BookingStatusHistoryDto();
		dto.setHistoryId(entity.getHistoryId());
		dto.setBookingId(entity.getBookingId());
		dto.setOldStatus(entity.getOldStatus());
		dto.setNewStatus(entity.getNewStatus());
		dto.setChangedBy(entity.getChangedBy());
		dto.setChangedAt(entity.getChangedAt());
		dto.setReason(entity.getReason());
		return dto;
	}

	public BookingStatusHistory toEntity(BookingStatusHistoryDto dto) {
		BookingStatusHistory entity = new BookingStatusHistory();
		entity.setHistoryId(dto.getHistoryId());
		entity.setBookingId(dto.getBookingId());
		entity.setOldStatus(dto.getOldStatus());
		entity.setNewStatus(dto.getNewStatus());
		entity.setChangedBy(dto.getChangedBy());
		entity.setChangedAt(dto.getChangedAt());
		entity.setReason(dto.getReason());
		return entity;
	}
}
