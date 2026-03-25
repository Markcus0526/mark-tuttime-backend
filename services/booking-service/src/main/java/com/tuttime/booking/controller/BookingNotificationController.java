package com.tuttime.booking.controller;

import com.tuttime.booking.dto.BookingNotificationDto;
import com.tuttime.booking.service.BookingNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/booking-notifications")
public class BookingNotificationController {

	private final BookingNotificationService service;

	public BookingNotificationController(BookingNotificationService service) {
		this.service = service;
	}

	@GetMapping
	public ResponseEntity<List<BookingNotificationDto>> getAllNotifications() {
		return ResponseEntity.ok(service.getAllNotifications());
	}

	@GetMapping("/{id}")
	public ResponseEntity<BookingNotificationDto> getNotificationById(@PathVariable UUID id) {
		return service.getNotificationById(id)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	@PostMapping
	public ResponseEntity<BookingNotificationDto> createNotification(@RequestBody BookingNotificationDto dto) {
		BookingNotificationDto created = service.createNotification(dto);
		return ResponseEntity.ok(created);
	}
}