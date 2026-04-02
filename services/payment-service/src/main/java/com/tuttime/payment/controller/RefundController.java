package com.tuttime.payment.controller;

import com.tuttime.payment.dto.RefundRequestDto;
import com.tuttime.payment.dto.RefundResponseDto;
import com.tuttime.payment.entity.Refund;
import com.tuttime.payment.entity.enums.RefundStatus;
import com.tuttime.payment.service.RefundService;
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
@RequestMapping("/api/v1/refunds")
@RequiredArgsConstructor
@Tag(name = "Refunds", description = "API endpoints for managing refunds")
public class RefundController {

	private final RefundService refundService;

	@Operation(summary = "Initiate a refund", description = "Create a refund request for a payment")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Refund initiated successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid refund request data")
	})
	@PostMapping
	public ResponseEntity<RefundResponseDto> initiateRefund(
			@Parameter(description = "Refund request payload", required = true)
			@Valid @RequestBody RefundRequestDto dto) {
		try {
			Refund refund = refundService.initiateRefund(dto.getPaymentId(), dto.getAmount(), dto.getReason());
			return ResponseEntity.ok(mapToDto(refund));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(null);
		}
	}

	@Operation(summary = "Get refund by ID", description = "Retrieve a refund by its unique ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Refund found"),
			@ApiResponse(responseCode = "404", description = "Refund not found")
	})
	@GetMapping("/{id}")
	public ResponseEntity<RefundResponseDto> getRefundById(
			@Parameter(description = "ID of the refund to retrieve", required = true)
			@PathVariable Long id) {
		return refundService.getRefundById(id)
				.map(this::mapToDto)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	@Operation(summary = "Get refunds", description = "Retrieve refunds filtered by payment ID or status")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Refunds retrieved successfully")
	})
	@GetMapping
	public ResponseEntity<List<RefundResponseDto>> getRefunds(
			@Parameter(description = "Filter refunds by payment ID")
			@RequestParam(required = false) Long paymentId,
			@Parameter(description = "Filter refunds by status")
			@RequestParam(required = false) RefundStatus status) {
		List<Refund> refunds;
		if (paymentId != null) {
			refunds = refundService.getRefundsByPaymentId(paymentId);
		} else if (status != null) {
			refunds = refundService.getRefundsByStatus(status);
		} else {
			refunds = List.of();
		}
		List<RefundResponseDto> dtos = refunds.stream()
				.map(this::mapToDto)
				.collect(Collectors.toList());
		return ResponseEntity.ok(dtos);
	}

	@Operation(summary = "Update refund status", description = "Update the status of an existing refund")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Refund status updated"),
			@ApiResponse(responseCode = "404", description = "Refund not found")
	})
	@PatchMapping("/{id}/status")
	public ResponseEntity<RefundResponseDto> updateRefundStatus(
			@Parameter(description = "ID of the refund to update", required = true)
			@PathVariable Long id,
			@Parameter(description = "New status for the refund", required = true)
			@RequestParam RefundStatus status) {
		try {
			Refund updated = refundService.updateRefundStatus(id, status);
			return ResponseEntity.ok(mapToDto(updated));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.notFound().build();
		}
	}

	private RefundResponseDto mapToDto(Refund refund) {
		RefundResponseDto dto = new RefundResponseDto();
		dto.setId(refund.getId());
		dto.setRefundId(refund.getRefundId());
		dto.setPaymentId(refund.getPayment().getId());
		dto.setAmount(refund.getAmount());
		dto.setStatus(refund.getStatus());
		dto.setReason(refund.getReason());
		dto.setCreatedAt(refund.getCreatedAt());
		dto.setUpdatedAt(refund.getUpdatedAt());
		return dto;
	}
}