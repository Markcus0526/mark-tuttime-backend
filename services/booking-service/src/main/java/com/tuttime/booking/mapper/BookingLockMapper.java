package com.tuttime.booking.mapper;

import com.tuttime.booking.dto.BookingLockDto;
import com.tuttime.booking.entity.BookingLock;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public class BookingLockMapper {

	public BookingLockDto toDto(BookingLock entity) {
		BookingLockDto dto = new BookingLockDto();
		dto.setLockId(entity.getLockId());
		dto.setBookingId(entity.getBookingId());
		dto.setTableId(entity.getTableId());
		dto.setLockAcquiredAt(entity.getLockAcquiredAt());
		dto.setLockExpiresAt(entity.getLockExpiresAt());
		return dto;
	}

	public BookingLock toEntity(BookingLockDto dto) {
		BookingLock entity = new BookingLock();
		entity.setLockId(dto.getLockId());
		entity.setBookingId(dto.getBookingId());
		entity.setTableId(dto.getTableId());
		entity.setLockAcquiredAt(dto.getLockAcquiredAt());
		entity.setLockExpiresAt(dto.getLockExpiresAt());
		return entity;
	}
}
