package com.tuttime.order.controller;

import com.tuttime.common.dto.ApiRes;
import com.tuttime.order.dto.OrderRequest;
import com.tuttime.order.dto.OrderResponse;
import com.tuttime.common.enums.OrderStatus;
import com.tuttime.order.dto.OrderUpdateRequest;
import com.tuttime.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order Controller", description = "APIs for managing restaurant pre-orders and statuses")
public class OrderController {

	private final OrderService orderService;

	@PostMapping
	@Operation(summary = "Create a new order", description = "Initializes a pre-order linked to a specific booking")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Order created successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid request data")
	})
	public ResponseEntity<ApiRes<OrderResponse>> createOrder(@Valid @RequestBody OrderRequest request) {
		OrderResponse response = orderService.createOrder(request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiRes.success(response));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get order details", description = "Retrieves full order details including items and modifiers")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Order found"),
			@ApiResponse(responseCode = "404", description = "Order not found")
	})
	public ResponseEntity<ApiRes<OrderResponse>> getOrder(
			@Parameter(description = "UUID of the order") @PathVariable UUID id) {
		OrderResponse response = orderService.getOrderById(id);
		return ResponseEntity.ok(ApiRes.success(response));
	}

	@PutMapping("/{id}")
	@Operation(summary = "Modify order content", description = "Replaces existing items with the new list and recalculates totals")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Order updated"),
			@ApiResponse(responseCode = "409", description = "Conflict (Concurrent update)"),
			@ApiResponse(responseCode = "404", description = "Order not found")
	})
	public ResponseEntity<ApiRes<OrderResponse>> updateOrderContent(
			@PathVariable UUID id,
			@Valid @RequestBody OrderUpdateRequest request) {

		OrderResponse response = orderService.updateOrderContent(id, request);
		return ResponseEntity.ok(ApiRes.success(response));
	}

	@GetMapping("/booking/{bookingId}")
	@Operation(summary = "Get order by booking ID", description = "Retrieves the active order associated with a specific table booking")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Order found"),
			@ApiResponse(responseCode = "404", description = "No order found for this booking")
	})
	public ResponseEntity<ApiRes<OrderResponse>> getOrderByBookingId(
			@Parameter(description = "UUID of the booking") @PathVariable UUID bookingId) {

		OrderResponse response = orderService.getOrderByBookingId(bookingId);
		return ResponseEntity.ok(ApiRes.success(response));
	}

	@PatchMapping("/{id}/status")
	@Operation(summary = "Update order status", description = "Transitions order through lifecycle (PAID, PREPARING, etc.)")
	@ApiResponse(responseCode = "200", description = "Status updated")
	public ResponseEntity<ApiRes<OrderResponse>> updateStatus(
			@PathVariable UUID id,
			@RequestParam OrderStatus status) {
		OrderResponse response = orderService.updateStatus(id, status);
		return ResponseEntity.ok(ApiRes.success(response));
	}

	@PostMapping("/{id}/checkout")
	@Operation(summary = "Checkout and Pay", description = "Finalizes the order, processes payment, and sets status to PAID")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Payment successful, order marked as PAID"),
			@ApiResponse(responseCode = "402", description = "Payment required/failed"),
			@ApiResponse(responseCode = "404", description = "Order not found")
	})
	public ResponseEntity<ApiRes<OrderResponse>> checkout(@PathVariable UUID id) {
		OrderResponse response = orderService.checkout(id);
		return ResponseEntity.ok(ApiRes.success(response));
	}

	@PostMapping("/{id}/cancel")
	@Operation(summary = "Cancel an order", description = "Transitions order status to CANCELED and triggers notifications")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Order cancelled successfully"),
			@ApiResponse(responseCode = "400", description = "Order cannot be cancelled (e.g., already served)"),
			@ApiResponse(responseCode = "404", description = "Order not found")
	})
	public ResponseEntity<ApiRes<OrderResponse>> cancelOrder(
			@Parameter(description = "UUID of the order to cancel") @PathVariable UUID id) {

		OrderResponse response = orderService.cancelOrder(id);
		return ResponseEntity.ok(ApiRes.success(response));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Soft delete an order", description = "Marks the order as deleted in the database")
	@ApiResponse(responseCode = "204", description = "Order deleted")
	public ResponseEntity<ApiRes<Void>> deleteOrder(@PathVariable UUID id) {
		orderService.deleteOrder(id);
		return ResponseEntity.status(HttpStatus.NO_CONTENT)
				.body(ApiRes.success(null));
	}

	@DeleteMapping("/{orderId}/items/{itemId}")
	@Operation(summary = "Remove a single item from an order")
	public ResponseEntity<ApiRes<OrderResponse>> removeItem(
			@PathVariable UUID orderId,
			@PathVariable UUID itemId) {

		OrderResponse response = orderService.removeItem(orderId, itemId);
		return ResponseEntity.ok(ApiRes.success(response));
	}
}