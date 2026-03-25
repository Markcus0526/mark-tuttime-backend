package com.tuttime.booking.controller;

import com.tuttime.booking.dto.BookingLockDto;
import com.tuttime.booking.service.BookingLockService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/booking-locks")
public class BookingLockController {

	private final BookingLockService bookingLockService;

	public BookingLockController(BookingLockService bookingLockService) {
		this.bookingLockService = bookingLockService;
	}

	@GetMapping
	public ResponseEntity<List<BookingLockDto>> getAllLocks() {
		return ResponseEntity.ok(bookingLockService.getAllLocks());
	}

	@GetMapping("/{id}")
	public ResponseEntity<BookingLockDto> getLockById(@PathVariable UUID id) {
		return bookingLockService.getLockById(id)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	@PostMapping
	public ResponseEntity<BookingLockDto> createLock(@RequestBody BookingLockDto dto) {
		BookingLockDto created = bookingLockService.createLock(dto);
		return ResponseEntity.ok(created);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteLock(@PathVariable UUID id) {
		bookingLockService.deleteLock(id);
		return ResponseEntity.noContent().build();
	}
}
