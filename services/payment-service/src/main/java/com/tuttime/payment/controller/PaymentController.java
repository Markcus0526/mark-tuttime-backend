package com.tuttime.payment.controller;

import com.tuttime.payment.dto.PaymentAuditLogResponseDto;
import com.tuttime.payment.dto.PaymentRequestDto;
import com.tuttime.payment.dto.PaymentResponseDto;
import com.tuttime.payment.entity.Payment;
import com.tuttime.payment.entity.PaymentAuditLog;
import com.tuttime.payment.enums.PaymentStatus;
import com.tuttime.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "API endpoints for managing payments")
public class PaymentController {

	private final PaymentService paymentService;

	@Operation(summary = "Initiate a payment", description = "Create a new payment based on the request data")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Payment initiated successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid input data")
	})
	@PostMapping
	public ResponseEntity<PaymentResponseDto> initiatePayment(
			@Parameter(description = "Payment request payload", required = true)
			@Valid @RequestBody PaymentRequestDto dto) {
		Payment payment = mapToEntity(dto);
		Payment created = paymentService.initiatePayment(payment);
		return ResponseEntity.ok(mapToDto(created));
	}

	@Operation(summary = "Get payment by ID", description = "Fetch a payment using its unique ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Payment found"),
			@ApiResponse(responseCode = "404", description = "Payment not found")
	})
	@GetMapping("/{id}")
	public ResponseEntity<PaymentResponseDto> getPaymentById(
			@Parameter(description = "ID of the payment to retrieve", required = true)
			@PathVariable Long id) {
		return paymentService.getPaymentById(id)
				.map(this::mapToDto)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	@Operation(summary = "Get payments", description = "Retrieve payments filtered by booking ID or status")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Payments retrieved successfully")
	})
	@GetMapping
	public ResponseEntity<List<PaymentResponseDto>> getPayments(
			@Parameter(description = "Filter payments by booking ID")
			@RequestParam(required = false) Long bookingId,
			@Parameter(description = "Filter payments by status")
			@RequestParam(required = false) PaymentStatus status) {
		List<Payment> payments;
		if (bookingId != null) {
			payments = paymentService.getPaymentsByBookingId(bookingId);
		} else if (status != null) {
			payments = paymentService.getPaymentsByStatus(status);
		} else {
			// Optional: Return all payments or empty list if no filters provided
			payments = List.of();
		}
		List<PaymentResponseDto> dtos = payments.stream()
				.map(this::mapToDto)
				.collect(Collectors.toList());
		return ResponseEntity.ok(dtos);
	}

	@Operation(summary = "Update payment status", description = "Update the status of an existing payment")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Payment status updated"),
			@ApiResponse(responseCode = "404", description = "Payment not found")
	})
	@PatchMapping("/{id}/status")
	public ResponseEntity<PaymentResponseDto> updatePaymentStatus(
			@Parameter(description = "ID of the payment to update", required = true)
			@PathVariable Long id,
			@Parameter(description = "New status for the payment", required = true)
			@RequestParam PaymentStatus status) {
		try {
			Payment updated = paymentService.updatePaymentStatus(id, status);
			return ResponseEntity.ok(mapToDto(updated));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.notFound().build();
		}
	}

	@Operation(summary = "Get payment audit logs", description = "Retrieve audit logs related to a payment")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Audit logs retrieved successfully")
	})
	@GetMapping("/{id}/audit-logs")
	public ResponseEntity<List<PaymentAuditLogResponseDto>> getPaymentAuditLogs(
			@Parameter(description = "ID of the payment to get audit logs for", required = true)
			@PathVariable Long id) {
//		List<PaymentAuditLog> auditLogs = paymentAuditLogService.getLogsByPaymentId(id);
		List<PaymentAuditLog> auditLogs = paymentService.getAuditLogsForPayment(id);
		List<PaymentAuditLogResponseDto> dtoList = auditLogs.stream()
				.map(log -> {
					PaymentAuditLogResponseDto dto = new PaymentAuditLogResponseDto();
					dto.setId(log.getId());
					dto.setPaymentId(log.getPayment().getId());
					dto.setEventType(log.getEventType());
					dto.setEventData(log.getEventData());
					dto.setCreatedAt(log.getCreatedAt());
					return dto;
				})
				.collect(Collectors.toList());
		return ResponseEntity.ok(dtoList);
	}

	private Payment mapToEntity(PaymentRequestDto dto) {
		return Payment.builder()
				.bookingId(dto.getBookingId())
				.amount(dto.getAmount())
				.currency(dto.getCurrency())
				.paymentMethod(dto.getPaymentMethod())
				.paymentIntentId(dto.getPaymentIntentId())
				.description(dto.getDescription())
				.build();
	}

	private PaymentResponseDto mapToDto(Payment payment) {
		PaymentResponseDto dto = new PaymentResponseDto();
		dto.setId(payment.getId());
		dto.setPaymentIntentId(payment.getPaymentIntentId());
		dto.setBookingId(payment.getBookingId());
		dto.setAmount(payment.getAmount());
		dto.setCurrency(payment.getCurrency());
		dto.setPaymentMethod(payment.getPaymentMethod());
		dto.setStatus(payment.getStatus());
		dto.setCreatedAt(payment.getCreatedAt());
		dto.setUpdatedAt(payment.getUpdatedAt());
		dto.setDescription(payment.getDescription());
		dto.setReceiptUrl(payment.getReceiptUrl());
		return dto;
	}
}