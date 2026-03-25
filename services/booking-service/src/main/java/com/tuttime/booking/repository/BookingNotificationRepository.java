package com.tuttime.booking.repository;

import com.tuttime.booking.dto.BookingNotificationDto;
import com.tuttime.booking.entity.BookingNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BookingNotificationRepository extends JpaRepository<BookingNotification, UUID> {
	private BookingNotificationDto mapToDto(BookingNotification entity) {
		BookingNotificationDto dto = new BookingNotificationDto();
		dto.setNotificationId(entity.getNotificationId());
		dto.setBookingId(entity.getBookingId());
		dto.setNotificationType(entity.getNotificationType());
		dto.setSentAt(entity.getSentAt());
		dto.setStatus(entity.getStatus());
		dto.setRecipientContact(entity.getRecipientContact());
		return dto;
	}

	private BookingNotification mapToEntity(BookingNotificationDto dto) {
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
