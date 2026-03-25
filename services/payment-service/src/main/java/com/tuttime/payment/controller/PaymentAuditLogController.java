package com.tuttime.payment.controller;

import com.tuttime.payment.dto.PaymentAuditLogRequestDTO;
import com.tuttime.payment.dto.PaymentAuditLogResponseDTO;
import com.tuttime.payment.entity.PaymentAuditLog;
import com.tuttime.payment.service.PaymentAuditLogService;
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
@RequestMapping("/api/v1/payment-audit-logs")
@RequiredArgsConstructor
@Tag(name = "Payment Audit Logs", description = "Endpoints for managing payment audit logs")
public class PaymentAuditLogController {

	private final PaymentAuditLogService paymentAuditLogService;

	@Operation(summary = "Add a payment audit log", description = "Create a new audit log entry for a payment")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Audit log added successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid request data")
	})
	@PostMapping
	public ResponseEntity<PaymentAuditLogResponseDTO> addAuditLog(
			@Parameter(description = "Audit log request payload", required = true)
			@Valid @RequestBody PaymentAuditLogRequestDTO dto) {
		PaymentAuditLog log = paymentAuditLogService.addAuditLog(dto.getPaymentId(), dto.getEventType(), dto.getEventData());
		return ResponseEntity.ok(mapToDTO(log));
	}

	@Operation(summary = "Get payment audit logs", description = "Retrieve audit logs filtered by payment ID or event type")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Audit logs retrieved successfully")
	})
	@GetMapping
	public ResponseEntity<List<PaymentAuditLogResponseDTO>> getAuditLogs(
			@Parameter(description = "Filter audit logs by payment ID")
			@RequestParam(required = false) Long paymentId,
			@Parameter(description = "Filter audit logs by event type")
			@RequestParam(required = false) String eventType) {
		List<PaymentAuditLog> logs;
		if (paymentId != null) {
			logs = paymentAuditLogService.getLogsByPaymentId(paymentId);
		} else if (eventType != null) {
			logs = paymentAuditLogService.getLogsByEventType(eventType);
		} else {
			logs = List.of();
		}
		List<PaymentAuditLogResponseDTO> dtos = logs.stream()
				.map(this::mapToDTO)
				.collect(Collectors.toList());
		return ResponseEntity.ok(dtos);
	}

	private PaymentAuditLogResponseDTO mapToDTO(PaymentAuditLog log) {
		PaymentAuditLogResponseDTO dto = new PaymentAuditLogResponseDTO();
		dto.setId(log.getId());
		dto.setPaymentId(log.getPayment().getId());
		dto.setEventType(log.getEventType());
		dto.setEventData(log.getEventData());
		dto.setCreatedAt(log.getCreatedAt());
		return dto;
	}
}