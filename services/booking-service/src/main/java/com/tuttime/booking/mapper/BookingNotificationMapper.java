package com.tuttime.booking.mapper;

import com.tuttime.booking.dto.BookingNotificationDto;
import com.tuttime.booking.entity.BookingNotification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public class BookingNotificationMapper {

	public BookingNotificationDto toDto(BookingNotification entity) {
		BookingNotificationDto dto = new BookingNotificationDto();
		dto.setNotificationId(entity.getNotificationId());
		dto.setBookingId(entity.getBookingId());
		dto.setNotificationType(entity.getNotificationType());
		dto.setSentAt(entity.getSentAt());
		dto.setStatus(entity.getStatus());
		dto.setRecipientContact(entity.getRecipientContact());
		return dto;
	}

	public BookingNotification toEntity(BookingNotificationDto dto) {
		BookingNotification entity = new BookingNotification();
		entity.setNotificationId(dto.getNotificationId());
		entity.setBookingId(dto.getBookingId());
		entity.setNotificationType(dto.getNotificationType());
		entity.setSentAt(dto.getSentAt());
		entity.setStatus(dto.getStatus());
		entity.setRecipientContact(dto.getRecipientContact());
		return entity;
	}
}
