package com.tuttime.booking.service;

import com.tuttime.booking.dto.BookingLockDto;
import com.tuttime.booking.entity.BookingLock;
import com.tuttime.booking.mapper.BookingLockMapper;
import com.tuttime.booking.repository.BookingLockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class BookingLockService {

	private final BookingLockRepository repository;
	private final BookingLockMapper bookingLockMapper;

	public List<BookingLockDto> getAllLocks() {
		return repository.findAll().stream()
				.map(bookingLockMapper::toDto)
				.collect(Collectors.toList());
	}

	public Optional<BookingLockDto> getLockById(UUID id) {
		return repository.findById(id)
				.map(bookingLockMapper::toDto);
	}

	public BookingLockDto createLock(BookingLockDto dto) {
		BookingLock entity = bookingLockMapper.toEntity(dto);
		entity = repository.save(entity);
		return bookingLockMapper.toDto(entity);
	}

	public void deleteLock(UUID id) {
		repository.deleteById(id);
	}

}
