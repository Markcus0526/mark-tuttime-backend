package com.tuttime.booking.service;

import com.tuttime.booking.dto.BookingStatusHistoryDto;
import com.tuttime.booking.entity.BookingStatusHistory;
import com.tuttime.booking.mapper.BookingStatusHistoryMapper;
import com.tuttime.booking.repository.BookingStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class BookingStatusHistoryService {

	private final BookingStatusHistoryRepository repository;
	private final BookingStatusHistoryMapper bookingStatusHistoryMapper;

	public List<BookingStatusHistoryDto> getAllHistories() {
		return repository.findAll().stream()
				.map(bookingStatusHistoryMapper::toDto)
				.collect(Collectors.toList());
	}

	public Optional<BookingStatusHistoryDto> getHistoryById(Long id) {
		return repository.findById(id).map(bookingStatusHistoryMapper::toDto);
	}

	public BookingStatusHistoryDto createHistory(BookingStatusHistoryDto dto) {
		BookingStatusHistory entity = bookingStatusHistoryMapper.toEntity(dto);
		if (entity.getChangedAt() == null) {
			entity.setChangedAt(Instant.now());
		}
		entity = repository.save(entity);
		return bookingStatusHistoryMapper.toDto(entity);
	}


}