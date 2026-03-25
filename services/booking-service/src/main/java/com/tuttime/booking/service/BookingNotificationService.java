package com.tuttime.booking.service;

import com.tuttime.booking.dto.BookingNotificationDto;
import com.tuttime.booking.entity.BookingNotification;
import com.tuttime.booking.mapper.BookingLockMapper;
import com.tuttime.booking.mapper.BookingNotificationMapper;
import com.tuttime.booking.repository.BookingNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class BookingNotificationService {

	private final BookingNotificationRepository repository;
    private final BookingNotificationMapper bookingNotificationMapper;

	public List<BookingNotificationDto> getAllNotifications() {
		return repository.findAll().stream()
				.map(bookingNotificationMapper::toDto)
				.collect(Collectors.toList());
	}

	public Optional<BookingNotificationDto> getNotificationById(UUID id) {
		return repository.findById(id).map(bookingNotificationMapper::toDto);
	}

	public BookingNotificationDto createNotification(BookingNotificationDto dto) {
		BookingNotification entity = bookingNotificationMapper.toEntity(dto);
		if (entity.getSentAt() == null) {
			entity.setSentAt(Instant.now());
		}
		entity = repository.save(entity);
		return bookingNotificationMapper.toDto(entity);
	}


}
