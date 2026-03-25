package com.tuttime.booking.service;

import com.tuttime.booking.dto.BookingDto;
import com.tuttime.booking.entity.Booking;
import com.tuttime.booking.mapper.BookingMapper;
import com.tuttime.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

	private final BookingRepository bookingRepository;
	private final BookingMapper bookingMapper;
	private final RedisTableLockService redisTableLockService;
	private final AvailabilityService availabilityService;
	private final RedisTemplate<String, String> redisTemplate;
	private final KafkaTemplate<String, Object> kafkaTemplate;

	public List<BookingDto> getAllBookings() {
		return bookingRepository.findAll()
				.stream()
				.map(bookingMapper::toDto)
				.collect(Collectors.toList());
	}

	public Optional<BookingDto> getBookingById(UUID id) {
		return bookingRepository.findById(id)
				.map(bookingMapper::toDto);
	}

	public BookingDto createBooking(BookingDto dto) {
		Booking booking = bookingMapper.toEntity(dto);
		booking = bookingRepository.save(booking);
		return bookingMapper.toDto(booking);
	}

	public Optional<BookingDto> updateBooking(UUID id, BookingDto dto) {
		return bookingRepository.findById(id).map(existing -> {
			// Update fields; example below (add more as needed)
			existing.setBookingStatus(dto.getBookingStatus());
			existing.setBookingDatetime(dto.getBookingDatetime());
			existing.setPartySize(dto.getPartySize());
			existing.setUpdatedAt(Instant.now());
			bookingRepository.save(existing);
			return bookingMapper.toDto(existing);
		});
	}

	public void deleteBooking(UUID id) {
		bookingRepository.deleteById(id);
	}
}
