package com.tuttime.booking.controller;

import com.tuttime.booking.dto.BookingStatusHistoryDto;
import com.tuttime.booking.service.BookingStatusHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/booking-status-histories")
public class BookingStatusHistoryController {

	private final BookingStatusHistoryService service;

	public BookingStatusHistoryController(BookingStatusHistoryService service) {
		this.service = service;
	}

	@GetMapping
	public ResponseEntity<List<BookingStatusHistoryDto>> getAllHistories() {
		return ResponseEntity.ok(service.getAllHistories());
	}

	@GetMapping("/{id}")
	public ResponseEntity<BookingStatusHistoryDto> getHistoryById(@PathVariable Long id) {
		return service.getHistoryById(id)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	@PostMapping
	public ResponseEntity<BookingStatusHistoryDto> createHistory(@RequestBody BookingStatusHistoryDto dto) {
		BookingStatusHistoryDto created = service.createHistory(dto);
		return ResponseEntity.ok(created);
	}
}
